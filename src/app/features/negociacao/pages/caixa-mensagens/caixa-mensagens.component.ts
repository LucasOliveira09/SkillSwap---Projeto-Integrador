import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, forkJoin } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { PerfilApiService } from '../../../perfil/data-access/perfil-api.service';
import { PerfilProfissional } from '../../../perfil/models/perfil.model';
import { SessaoService } from '../../../conta/data-access/sessao.service';
import { Acordo, Mensagem, Pagina } from '../../models/negociacao.model';
import { NegociacaoApiService } from '../../data-access/negociacao-api.service';

@Component({
  selector: 'app-caixa-mensagens',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './caixa-mensagens.component.html',
  styleUrl: './caixa-mensagens.component.scss',
})
export class CaixaMensagensComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly sessao = inject(SessaoService);
  private readonly perfilApi = inject(PerfilApiService);
  private readonly api = inject(NegociacaoApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly carregando = signal(true);
  protected readonly carregandoMais = signal(false);
  protected readonly enviando = signal(false);
  protected readonly erro = signal('');
  protected readonly sucesso = signal('');
  protected readonly perfil = signal<PerfilProfissional | null>(null);
  protected readonly acordos = signal<readonly Acordo[]>([]);
  protected readonly paginaAcordos = signal(0);
  protected readonly totalPaginasAcordos = signal(0);
  protected readonly acordoSelecionado = signal<Acordo | null>(null);
  protected readonly mensagens = signal<readonly Mensagem[]>([]);
  protected readonly paginaMensagens = signal(0);
  protected readonly totalPaginasMensagens = signal(0);
  protected readonly carregandoMensagens = signal(false);
  protected texto = '';

  constructor() {
    if (!this.sessao.autenticado()) {
      void this.router.navigate(['/conta'], { queryParams: { retorno: '/mensagens' } });
      return;
    }
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erro.set('');
    forkJoin({ perfil: this.perfilApi.meuPerfil(), acordos: this.api.acordos(0) })
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.carregando.set(false)))
      .subscribe({
        next: ({ perfil, acordos }) => {
          this.perfil.set(perfil);
          this.acordos.set(acordos.itens);
          this.paginaAcordos.set(acordos.pagina);
          this.totalPaginasAcordos.set(acordos.totalPaginas);
          const idSolicitado = Number(this.route.snapshot.queryParamMap.get('acordo'));
          const selecionado = acordos.itens.find((item) => item.id === idSolicitado)
            ?? acordos.itens[0]
            ?? null;
          if (selecionado) this.selecionar(selecionado, false);
        },
        error: () => this.erro.set('Não foi possível carregar suas conversas. Tente novamente.'),
      });
  }

  protected carregarMaisAcordos(): void {
    if (this.carregandoMais() || this.paginaAcordos() + 1 >= this.totalPaginasAcordos()) return;
    const proximaPagina = this.paginaAcordos() + 1;
    this.carregandoMais.set(true);
    this.api.acordos(proximaPagina)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.carregandoMais.set(false)))
      .subscribe({
        next: (pagina) => {
          this.acordos.update((atuais) => [...atuais, ...pagina.itens]);
          this.paginaAcordos.set(pagina.pagina);
          this.totalPaginasAcordos.set(pagina.totalPaginas);
        },
        error: () => this.erro.set('Não foi possível carregar mais conversas.'),
      });
  }

  protected selecionar(acordo: Acordo, atualizarUrl = true): void {
    this.acordoSelecionado.set(acordo);
    this.mensagens.set([]);
    this.erro.set('');
    this.sucesso.set('');
    if (atualizarUrl) {
      void this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { acordo: acordo.id },
        queryParamsHandling: 'merge',
      });
    }
    this.carregandoMensagens.set(true);
    this.api.mensagens(acordo.id, 0)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (primeiraPagina) => {
          if (primeiraPagina.totalPaginas <= 1) {
            this.aplicarMensagens(primeiraPagina);
            this.carregandoMensagens.set(false);
            return;
          }
          const paginaAtual = Math.max(0, primeiraPagina.totalPaginas - 1);
          this.api.mensagens(acordo.id, paginaAtual)
            .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.carregandoMensagens.set(false)))
            .subscribe({
              next: (pagina) => this.aplicarMensagens(pagina),
              error: () => this.erro.set('Não foi possível carregar as mensagens desta conversa.'),
            });
        },
        error: () => {
          this.carregandoMensagens.set(false);
          this.erro.set('Não foi possível carregar as mensagens desta conversa.');
        },
      });
  }

  protected carregarMensagensAnteriores(): void {
    const acordo = this.acordoSelecionado();
    const pagina = this.paginaMensagens() - 1;
    if (!acordo || pagina < 0 || this.carregandoMensagens()) return;
    this.carregandoMensagens.set(true);
    this.api.mensagens(acordo.id, pagina)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.carregandoMensagens.set(false)))
      .subscribe({
        next: (resposta) => this.aplicarMensagens(resposta),
        error: () => this.erro.set('Não foi possível carregar mensagens anteriores.'),
      });
  }

  protected enviar(): void {
    const acordo = this.acordoSelecionado();
    const conteudo = this.texto.trim();
    if (!acordo || acordo.status !== 'EM_ANDAMENTO' || !conteudo || conteudo.length > 2000 || this.enviando()) return;
    this.enviando.set(true);
    this.erro.set('');
    this.sucesso.set('');
    this.api.enviarMensagem(acordo.id, conteudo)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.enviando.set(false)))
      .subscribe({
        next: () => {
          this.texto = '';
          this.selecionar(acordo, false);
          this.sucesso.set('Mensagem enviada.');
        },
        error: () => this.erro.set('Não foi possível enviar a mensagem. Tente novamente.'),
      });
  }

  protected interlocutor(acordo: Acordo): string {
    const id = this.perfil()?.id;
    return acordo.anuncianteId === id ? acordo.proponenteNome : acordo.anuncianteNome;
  }

  protected iniciais(nome: string): string {
    return nome.trim().split(/\s+/).slice(0, 2).map((parte) => parte.charAt(0)).join('').toLocaleUpperCase('pt-BR');
  }

  protected carregarMensagemAnteriorDisponivel(): boolean {
    return this.paginaMensagens() > 0;
  }

  private aplicarMensagens(pagina: Pagina<Mensagem>): void {
    this.mensagens.set(pagina.itens);
    this.paginaMensagens.set(pagina.pagina);
    this.totalPaginasMensagens.set(pagina.totalPaginas);
  }
}
