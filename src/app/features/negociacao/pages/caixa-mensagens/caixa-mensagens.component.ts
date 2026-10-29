import { DOCUMENT, DatePipe } from '@angular/common';
import { Component, DestroyRef, ElementRef, Injector, afterNextRender, computed, effect, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, forkJoin, interval } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { SessaoService } from '../../../conta/data-access/sessao.service';
import { PerfilApiService } from '../../../perfil/data-access/perfil-api.service';
import { PerfilProfissional } from '../../../perfil/models/perfil.model';
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
  protected readonly filtro = signal('');
  protected readonly visiveis = computed(() => {
    const busca = this.filtro().trim().toLocaleLowerCase('pt-BR');
    return this.acordos().filter(acordo =>
      `${this.interlocutor(acordo)} ${acordo.anuncioTitulo}`.toLocaleLowerCase('pt-BR').includes(busca));
  });
  protected readonly erroSincronizacao = signal('');
  private readonly document = inject(DOCUMENT);
  private readonly injector = inject(Injector);
  private readonly historico = viewChild<ElementRef<HTMLDivElement>>('historico');
  private readonly rascunhos = new Map<number, string>();
  private sincronizando = false;
  private rolarAoAbrir = true;
  private readonly sessao = inject(SessaoService);
  private readonly acompanharSessao = effect(() => {
    if (!this.sessao.autenticado()) {
      const id = this.acordoSelecionado()?.id;
      void this.router.navigate(['/conta'], { queryParams: { retorno: id ? `/mensagens?acordo=${id}` : '/mensagens' } });
    }
  });
  private geracaoSelecao = 0;

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (!this.carregando()) this.sincronizarRota();
    });
    interval(8000).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.document.visibilityState === 'visible') this.atualizarConversa();
    });
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
          this.sincronizarRota();
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
          this.acordos.update((atuais) => [...new Map(
            [...atuais, ...pagina.itens].map(item => [item.id, item]),
          ).values()]);
          this.paginaAcordos.set(pagina.pagina);
          this.totalPaginasAcordos.set(pagina.totalPaginas);
        },
        error: () => this.erro.set('Não foi possível carregar mais conversas.'),
      });
  }

  protected selecionar(acordo: Acordo, atualizarUrl = true): void {
    if (atualizarUrl) {
      void this.router.navigate([], { relativeTo: this.route, queryParams: { acordo: acordo.id }, queryParamsHandling: 'merge' });
      return;
    }
    const anterior = this.acordoSelecionado();
    if (anterior) this.rascunhos.set(anterior.id, this.texto);
    this.texto = this.rascunhos.get(acordo.id) ?? '';
    this.rolarAoAbrir = true;
    const geracao = ++this.geracaoSelecao;
    this.acordoSelecionado.set(acordo);
    this.mensagens.set([]);
    this.erro.set('');
    this.sucesso.set('');
    this.carregandoMensagens.set(true);
    this.api.mensagens(acordo.id, 0)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (primeiraPagina) => {
          if (geracao !== this.geracaoSelecao) return;
          if (primeiraPagina.totalPaginas <= 1) {
            this.aplicarMensagens(primeiraPagina);
            this.carregandoMensagens.set(false);
            return;
          }
          const paginaAtual = Math.max(0, primeiraPagina.totalPaginas - 1);
          this.api.mensagens(acordo.id, paginaAtual)
            .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => {
              if (geracao === this.geracaoSelecao) this.carregandoMensagens.set(false);
            }))
            .subscribe({
              next: (pagina) => {
                if (geracao === this.geracaoSelecao) this.aplicarMensagens(pagina);
              },
              error: () => {
                if (geracao === this.geracaoSelecao) this.erro.set('Não foi possível carregar as mensagens desta conversa.');
              },
            });
        },
        error: () => {
          if (geracao === this.geracaoSelecao) {
            this.carregandoMensagens.set(false);
            this.erro.set('Não foi possível carregar as mensagens desta conversa.');
          }
        },
      });
  }

  protected voltarLista(): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: { acordo: null }, queryParamsHandling: 'merge' });
  }

  private sincronizarRota(): void {
    const id = Number(this.route.snapshot.queryParamMap.get('acordo'));
    if (id === this.acordoSelecionado()?.id) return;
    if (!Number.isSafeInteger(id) || id <= 0) {
      const anterior = this.acordoSelecionado();
      if (anterior) this.rascunhos.set(anterior.id, this.texto);
      ++this.geracaoSelecao;
      this.acordoSelecionado.set(null);
      this.mensagens.set([]);
      this.carregandoMensagens.set(false);
      return;
    }
    const acordo = this.acordos().find(item => item.id === id);
    if (acordo) { this.selecionar(acordo, false); return; }
    const geracao = ++this.geracaoSelecao;
    this.api.acordo(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: item => {
        if (geracao !== this.geracaoSelecao) return;
        this.acordos.update(atuais => [item, ...atuais.filter(a => a.id !== item.id)]);
        this.selecionar(item, false);
      },
      error: () => { if (geracao === this.geracaoSelecao) this.erro.set('Conversa indisponível ou sem permissão de acesso.'); },
    });
  }

  protected atualizarConversa(): void {
    const acordo = this.acordoSelecionado();
    if (!acordo || this.sincronizando || this.enviando() || this.carregandoMensagens() ||
      this.paginaMensagens() < this.totalPaginasMensagens() - 1) return;
    const geracao = this.geracaoSelecao;
    this.sincronizando = true;
    this.api.mensagens(acordo.id, this.paginaMensagens()).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.sincronizando = false),
    ).subscribe({
      next: pagina => {
        if (geracao !== this.geracaoSelecao) return;
        this.erroSincronizacao.set('');
        if (pagina.totalPaginas > this.totalPaginasMensagens()) {
          this.carregarPagina(acordo.id, pagina.totalPaginas - 1, geracao);
        } else this.aplicarMensagens(pagina);
      },
      error: () => { if (geracao === this.geracaoSelecao) this.erroSincronizacao.set('A atualização automática falhou. Tentaremos novamente.'); },
    });
  }

  private carregarPagina(id: number, pagina: number, geracao: number): void {
    this.carregandoMensagens.set(true);
    this.api.mensagens(id, pagina).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => {
      if (geracao === this.geracaoSelecao) this.carregandoMensagens.set(false);
    })).subscribe({
      next: resposta => { if (geracao === this.geracaoSelecao) this.aplicarMensagens(resposta); },
      error: () => { if (geracao === this.geracaoSelecao) this.erroSincronizacao.set('Não foi possível atualizar as mensagens.'); },
    });
  }

  protected carregarMensagensRecentes(): void {
    const acordo = this.acordoSelecionado();
    if (acordo && !this.carregandoMensagens()) this.selecionar(acordo, false);
  }

  protected aoEnter(evento: KeyboardEvent): void {
    if (!evento.shiftKey && !evento.isComposing && window.matchMedia('(pointer: fine)').matches) {
      evento.preventDefault();
      this.enviar();
    }
  }

  protected carregarMensagensAnteriores(): void {
    const acordo = this.acordoSelecionado();
    const pagina = this.paginaMensagens() - 1;
    if (!acordo || pagina < 0 || this.carregandoMensagens()) return;
    const geracao = this.geracaoSelecao;
    this.carregandoMensagens.set(true);
    this.api.mensagens(acordo.id, pagina)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => {
        if (geracao === this.geracaoSelecao) this.carregandoMensagens.set(false);
      }))
      .subscribe({
        next: (resposta) => {
          if (geracao === this.geracaoSelecao) this.aplicarMensagens(resposta);
        },
        error: () => {
          if (geracao === this.geracaoSelecao) this.erro.set('Não foi possível carregar mensagens anteriores.');
        },
      });
  }

  protected enviar(): void {
    const acordo = this.acordoSelecionado();
    const conteudo = this.texto.trim();
    if (!acordo || acordo.status !== 'EM_ANDAMENTO' || !conteudo || conteudo.length > 2000 || this.enviando()) return;
    const geracao = this.geracaoSelecao;
    this.enviando.set(true);
    this.erro.set('');
    this.sucesso.set('');
    this.api.enviarMensagem(acordo.id, conteudo)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.enviando.set(false)))
      .subscribe({
        next: () => {
          this.rascunhos.delete(acordo.id);
          if (geracao !== this.geracaoSelecao) return;
          this.texto = '';
          this.selecionar(acordo, false);
          this.sucesso.set('Mensagem enviada.');
        },
        error: () => { if (geracao === this.geracaoSelecao) this.erro.set('Não foi possível enviar a mensagem. Tente novamente.'); },
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
    const elemento = this.historico()?.nativeElement;
    const rolar = this.rolarAoAbrir || (elemento && elemento.scrollHeight - elemento.scrollTop - elemento.clientHeight < 100);
    this.rolarAoAbrir = false;
    this.mensagens.set(pagina.itens);
    if (rolar) afterNextRender(() => {
      const historico = this.historico()?.nativeElement;
      if (historico) historico.scrollTop = historico.scrollHeight;
    }, { injector: this.injector });
    this.paginaMensagens.set(pagina.pagina);
    this.totalPaginasMensagens.set(pagina.totalPaginas);
  }
}
