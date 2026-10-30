import { Component, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { catchError, map, of, startWith, switchMap } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { AnunciosApiService } from '../../data-access/anuncios-api.service';
import { AnuncioApi } from '../../models/anuncio-api.model';
import { DiaSemana, PeriodoDia } from '../../models/anuncio.model';
import { SessaoService } from '../../../conta/data-access/sessao.service';
import { ConversasApiService } from '../../../negociacao/data-access/conversas-api.service';
import { IconeComponent } from '../../../../shared/ui/icone/icone.component';
import { DenunciarComponent } from '../../../moderacao/ui/denunciar/denunciar.component';
import { AvaliacaoAnuncioApi, ComentarioAnuncioApi } from '../../models/anuncio-api.model';

interface EstadoDetalhe {
  carregando: boolean;
  erro: boolean;
  anuncio: AnuncioApi | null;
}

const estadoInicial: EstadoDetalhe = {
  carregando: true,
  erro: false,
  anuncio: null,
};

@Component({
  selector: 'app-detalhe-anuncio',
  standalone: true,
  imports: [
    RouterLink,
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    IconeComponent,
    DenunciarComponent,
  ],
  templateUrl: './detalhe-anuncio.component.html',
  styleUrl: './detalhe-anuncio.component.scss',
})
export class DetalheAnuncioComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(AnunciosApiService);
  private readonly conversasApi = inject(ConversasApiService);
  protected readonly sessao = inject(SessaoService);

  protected readonly fotoSelecionada = signal<string | null>(null);
  private readonly fotoQueFalhou = signal<string | null>(null);
  protected readonly enviandoMensagem = signal(false);
  protected readonly feedbackSucesso = signal<string | null>(null);
  protected readonly feedbackErro = signal<string | null>(null);
  protected readonly comentarios = signal<readonly ComentarioAnuncioApi[]>([]);
  protected readonly paginaComentarios = signal(0);
  protected readonly totalPaginasComentarios = signal(0);
  protected readonly totalComentarios = signal(0);
  protected readonly carregandoMaisComentarios = signal(false);
  protected readonly avaliacoesAnuncio = signal<readonly AvaliacaoAnuncioApi[]>([]);
  protected readonly carregandoInteracoes = signal(false);
  protected readonly erroInteracoes = signal('');
  protected comentarioTexto = '';
  protected readonly respostaAId = signal<number | null>(null);
  private geracaoInteracoes = 0;

  protected readonly estado = toSignal(
    this.route.paramMap.pipe(
      switchMap((params) => {
        const id = params.get('id');
        if (!id) {
          return of({ carregando: false, erro: true, anuncio: null });
        }
        return this.api.obter(id).pipe(
          map((anuncio) => {
            this.fotoSelecionada.set(anuncio.imagem);
            return { carregando: false, erro: false, anuncio };
          }),
          catchError(() => of({ carregando: false, erro: true, anuncio: null })),
          startWith(estadoInicial),
        );
      }),
    ),
    { initialValue: estadoInicial },
  );

  protected readonly anuncio = computed(() => this.estado().anuncio);
  protected readonly fotoPrincipal = computed(() => {
    const foto = this.fotoSelecionada() || this.anuncio()?.imagem || null;
    return foto && foto !== this.fotoQueFalhou() ? foto : null;
  });
  protected readonly fotoIndisponivel = computed(() => {
    const foto = this.fotoSelecionada() || this.anuncio()?.imagem || null;
    return Boolean(foto && foto === this.fotoQueFalhou());
  });
  private readonly carregarInteracoesPorAnuncio = effect(() => {
    const anuncio = this.anuncio();
    this.sessao.autenticado();
    if (anuncio) this.carregarInteracoes(anuncio);
  });

  protected readonly linhasDescricao = computed(() => {
    const desc = this.anuncio()?.descricao;
    if (!desc) return [];
    return desc.split('\n').map((l) => l.trim()).filter((l) => l.length > 0);
  });

  protected selecionarFoto(url: string | null): void {
    this.fotoSelecionada.set(url);
  }

  protected marcarFotoIndisponivel(url: string | null): void {
    if (url) this.fotoQueFalhou.set(url);
  }

  protected estrelas(quantidade: number | null): string {
    if (quantidade === null) return '';
    return '★'.repeat(quantidade) + '☆'.repeat(5 - quantidade);
  }

  protected rotuloDia(dia: DiaSemana): string {
    return ({ SEGUNDA: 'Segunda-feira', TERCA: 'Terça-feira', QUARTA: 'Quarta-feira', QUINTA: 'Quinta-feira', SEXTA: 'Sexta-feira', SABADO: 'Sábado', DOMINGO: 'Domingo' })[dia];
  }

  protected rotuloPeriodo(periodo: PeriodoDia): string {
    return ({ MANHA: 'Manhã', TARDE: 'Tarde', NOITE: 'Noite' })[periodo];
  }

  protected entrarEmContato(): void {
    const anuncioAtual = this.anuncio();
    if (!anuncioAtual || this.enviandoMensagem()) return;
    if (anuncioAtual.exemplo) { this.feedbackErro.set('Este anúncio é ilustrativo e não recebe mensagens.'); return; }

    if (!this.sessao.autenticado()) {
      void this.router.navigate(['/conta'], {
        queryParams: { retorno: `/anuncios/${anuncioAtual.id}` },
      });
      return;
    }

    this.enviandoMensagem.set(true);
    this.feedbackSucesso.set(null);
    this.feedbackErro.set(null);

    this.conversasApi.abrirConversa(anuncioAtual.usuarioId, anuncioAtual.id).subscribe({
      next: (conversa) => {
        this.enviandoMensagem.set(false);
        void this.router.navigate(['/mensagens'], {
          queryParams: { conversa: `chat-${conversa.id}`, anuncioId: anuncioAtual.id },
        });
      },
      error: (err) => {
        this.enviandoMensagem.set(false);
        const msg =
          err?.error?.mensagem ||
          'Não foi possível enviar sua mensagem. Tente novamente.';
        this.feedbackErro.set(msg);
      },
    });
  }

  protected carregarInteracoes(anuncio: AnuncioApi): void {
    const geracao = ++this.geracaoInteracoes;
    this.comentarios.set([]);
    this.paginaComentarios.set(0);
    this.totalPaginasComentarios.set(0);
    this.totalComentarios.set(0);
    this.avaliacoesAnuncio.set([]);
    this.erroInteracoes.set('');
    if (anuncio.exemplo) {
      this.carregandoInteracoes.set(false);
      return;
    }
    this.carregandoInteracoes.set(true);
    this.api.comentarios(anuncio.id).subscribe({
      next: pagina => {
        if (geracao !== this.geracaoInteracoes) return;
        this.comentarios.set(pagina.itens);
        this.paginaComentarios.set(pagina.pagina);
        this.totalPaginasComentarios.set(pagina.totalPaginas);
        this.totalComentarios.set(pagina.totalItens);
      },
      error: () => { if (geracao === this.geracaoInteracoes) this.erroInteracoes.set('Não foi possível carregar os comentários.'); },
      complete: () => { if (geracao === this.geracaoInteracoes) this.carregandoInteracoes.set(false); },
    });
    this.api.avaliacoesAnuncio(anuncio.id).subscribe({
      next: avaliacoes => { if (geracao === this.geracaoInteracoes) this.avaliacoesAnuncio.set(avaliacoes); },
      error: () => { if (geracao === this.geracaoInteracoes) this.erroInteracoes.set('Não foi possível carregar as avaliações.'); },
    });
  }

  protected responderComentario(comentario: ComentarioAnuncioApi): void {
    if (comentario.podeResponder) this.respostaAId.set(comentario.id);
  }

  protected cancelarResposta(): void { this.respostaAId.set(null); }

  protected carregarMaisComentarios(): void {
    const anuncio = this.anuncio();
    if (!anuncio || this.carregandoMaisComentarios() || this.paginaComentarios() + 1 >= this.totalPaginasComentarios()) return;
    const proximaPagina = this.paginaComentarios() + 1;
    this.carregandoMaisComentarios.set(true);
    this.api.comentarios(anuncio.id, proximaPagina).subscribe({
      next: pagina => {
        this.comentarios.update(itens => [...itens, ...pagina.itens]);
        this.paginaComentarios.set(pagina.pagina);
        this.totalPaginasComentarios.set(pagina.totalPaginas);
        this.totalComentarios.set(pagina.totalItens);
      },
      error: () => this.feedbackErro.set('Não foi possível carregar mais comentários.'),
      complete: () => this.carregandoMaisComentarios.set(false),
    });
  }

  protected enviarComentario(): void {
    const anuncio = this.anuncio();
    const conteudo = this.comentarioTexto.trim();
    if (!anuncio || anuncio.exemplo || !conteudo || conteudo.length > 1000) return;
    if (!this.sessao.autenticado()) {
      void this.router.navigate(['/conta'], { queryParams: { retorno: `/anuncios/${anuncio.id}#comentarios` } });
      return;
    }
    const parentId = this.respostaAId() ?? undefined;
    this.feedbackErro.set(null);
    this.api.comentar(anuncio.id, conteudo, parentId).subscribe({
      next: comentario => {
        this.comentarios.update(itens => [...itens, comentario]);
        this.comentarioTexto = '';
        this.respostaAId.set(null);
      },
      error: () => this.feedbackErro.set('Não foi possível publicar seu comentário. Tente novamente.'),
    });
  }

  protected recarregarInteracoes(): void {
    const anuncio = this.anuncio();
    if (anuncio) this.carregarInteracoes(anuncio);
  }
}
