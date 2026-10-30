import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { finalize } from 'rxjs';
import { SessaoService } from '../../../conta/data-access/sessao.service';
import { DenunciarComponent } from '../../../moderacao/ui/denunciar/denunciar.component';
import { PerfilApiService } from '../../data-access/perfil-api.service';
import { ComentarioPublicacao, CurtidaPublicacao, PaginaComentariosPublicacao, PublicacaoPortfolio } from '../../models/perfil.model';

@Component({
  selector: 'app-publicacao-interacoes',
  standalone: true,
  imports: [DatePipe, FormsModule, MatButtonModule, DenunciarComponent],
  templateUrl: './publicacao-interacoes.component.html',
  styleUrl: './publicacao-interacoes.component.scss',
})
export class PublicacaoInteracoesComponent {
  readonly publicacao = input.required<PublicacaoPortfolio>();

  private readonly api = inject(PerfilApiService);
  private readonly router = inject(Router);
  protected readonly sessao = inject(SessaoService);
  protected readonly aberta = signal(false);
  protected readonly carregando = signal(false);
  protected readonly carregada = signal(false);
  protected readonly enviando = signal(false);
  protected readonly erroLeitura = signal('');
  protected readonly erroEnvio = signal('');
  protected readonly pagina = signal<PaginaComentariosPublicacao | null>(null);
  protected readonly paginaAtual = computed(() => this.pagina()?.pagina ?? 0);
  protected readonly respostaA = signal<ComentarioPublicacao | null>(null);
  protected texto = '';
  private readonly resumoCurtidaAtualizado = signal<CurtidaPublicacao | null>(null);
  protected readonly curtidaEmAndamento = signal(false);
  protected readonly erroCurtida = signal('');
  protected readonly resumoCurtida = computed(() => this.resumoCurtidaAtualizado() ?? {
    publicacaoId: this.publicacao().id,
    totalCurtidas: this.publicacao().totalCurtidas,
    curtidaPeloUsuario: this.publicacao().curtidaPeloUsuario,
  });

  protected alternarCurtida(): void {
    if (this.curtidaEmAndamento()) return;
    if (!this.sessao.autenticado()) {
      void this.router.navigate(['/conta'], { queryParams: { retorno: this.router.url } });
      return;
    }
    const publicacao = this.publicacao();
    const requisicao = this.resumoCurtida().curtidaPeloUsuario
      ? this.api.deixarDeCurtirPublicacao(publicacao.id)
      : this.api.curtirPublicacao(publicacao.id);
    this.curtidaEmAndamento.set(true);
    this.erroCurtida.set('');
    requisicao.pipe(finalize(() => this.curtidaEmAndamento.set(false))).subscribe({
      next: (resumo) => this.resumoCurtidaAtualizado.set(resumo),
      error: () => this.erroCurtida.set('Não foi possível atualizar a curtida. Tente novamente.'),
    });
  }

  protected alternar(): void {
    const abrir = !this.aberta();
    this.aberta.set(abrir);
    if (abrir && !this.carregada()) this.carregar(0);
  }

  protected carregar(pagina: number): void {
    if (this.carregando()) return;
    this.carregando.set(true);
    this.erroLeitura.set('');
    this.api.comentariosPublicacao(this.publicacao().id, pagina).pipe(
      finalize(() => this.carregando.set(false)),
    ).subscribe({
      next: (resultado) => {
        this.pagina.set(resultado);
        this.carregada.set(true);
      },
      error: () => this.erroLeitura.set('Não foi possível carregar os comentários.'),
    });
  }

  protected responder(comentario: ComentarioPublicacao): void {
    this.respostaA.set(comentario);
    this.erroEnvio.set('');
  }

  protected cancelarResposta(): void {
    this.respostaA.set(null);
  }

  protected enviar(): void {
    const conteudo = this.texto.trim();
    if (!conteudo || conteudo.length > 1000 || this.enviando()) return;
    if (!this.sessao.autenticado()) {
      void this.router.navigate(['/conta'], { queryParams: { retorno: this.router.url } });
      return;
    }
    const raiz = this.respostaA();
    this.enviando.set(true);
    this.erroEnvio.set('');
    this.api.comentarPublicacao(this.publicacao().id, conteudo, raiz?.id).pipe(
      finalize(() => this.enviando.set(false)),
    ).subscribe({
      next: (criado) => {
        const atual = this.pagina();
        if (atual) {
          if (raiz) {
            const itens = atual.itens.map((item) => item.id === raiz.id
              ? { ...item, respostas: [...item.respostas, criado] }
              : item);
            this.pagina.set({ ...atual, itens, totalItens: atual.totalItens + 1 });
          } else if (atual.pagina === 0) {
            this.pagina.set({ ...atual, itens: [criado, ...atual.itens], totalItens: atual.totalItens + 1 });
          } else {
            this.carregar(0);
          }
        }
        this.texto = '';
        this.respostaA.set(null);
      },
      error: () => this.erroEnvio.set('Não foi possível publicar seu comentário. Tente novamente.'),
    });
  }

  protected paginaAnterior(): void {
    if (this.paginaAtual() > 0) this.carregar(this.paginaAtual() - 1);
  }

  protected proximaPagina(): void {
    const atual = this.pagina();
    if (atual && atual.pagina + 1 < atual.totalPaginas) this.carregar(atual.pagina + 1);
  }

  protected entrarParaComentar(): void {
    void this.router.navigate(['/conta'], { queryParams: { retorno: this.router.url } });
  }
}
