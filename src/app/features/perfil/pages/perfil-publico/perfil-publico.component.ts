import { Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, combineLatest, forkJoin, map, of, startWith, Subject, switchMap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { AnunciosApiService } from '../../../anuncios/data-access/anuncios-api.service';
import { AnuncioApi } from '../../../anuncios/models/anuncio-api.model';
import { IconeComponent } from '../../../../shared/ui/icone/icone.component';
import { PerfilApiService } from '../../data-access/perfil-api.service';
import { EstadoSeguindo, PerfilProfissional, PublicacaoPortfolio } from '../../models/perfil.model';
import { Avaliacao } from '../../../negociacao/models/negociacao.model';
import { DenunciarComponent } from '../../../moderacao/ui/denunciar/denunciar.component';
import { SessaoService } from '../../../conta/data-access/sessao.service';
import { ConversasApiService } from '../../../negociacao/data-access/conversas-api.service';

interface EstadoPerfil {
  carregando: boolean;
  erro: boolean;
  erroOfertas: boolean;
  erroAvaliacoes: boolean;
  perfil: PerfilProfissional | null;
  ofertas: readonly AnuncioApi[];
  paginaOfertas: number;
  totalPaginasOfertas: number;
  totalOfertas: number;
  publicacoes: readonly PublicacaoPortfolio[];
  erroPublicacoes: boolean;
  paginaPublicacoes: number;
  totalPaginasPublicacoes: number;
  totalPublicacoes: number;
  avaliacoes: readonly Avaliacao[];
  seguidores: EstadoSeguindo | null;
}

const estadoInicial: EstadoPerfil = {
  carregando: true,
  erro: false,
  erroOfertas: false,
  erroAvaliacoes: false,
  perfil: null,
  ofertas: [],
  paginaOfertas: 0,
  totalPaginasOfertas: 0,
  totalOfertas: 0,
  publicacoes: [],
  erroPublicacoes: false,
  paginaPublicacoes: 0,
  totalPaginasPublicacoes: 0,
  totalPublicacoes: 0,
  avaliacoes: [],
  seguidores: null,
};

@Component({
  selector: 'app-perfil-publico',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, DecimalPipe, MatButtonModule, IconeComponent, DenunciarComponent],
  templateUrl: './perfil-publico.component.html',
  styleUrl: './perfil-publico.component.scss',
})
export class PerfilPublicoComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly perfilApi = inject(PerfilApiService);
  private readonly anunciosApi = inject(AnunciosApiService);
  private readonly conversasApi = inject(ConversasApiService);
  protected readonly sessao = inject(SessaoService);
  private readonly atualizacao = new Subject<void>();
  private readonly estadoSeguindoAtual = signal<EstadoSeguindo | null>(null);
  protected readonly alterandoSeguimento = signal(false);
  protected readonly abrindoConversa = signal(false);
  protected readonly erroSeguimento = signal('');

  protected readonly estado = toSignal(
    combineLatest([
      this.route.paramMap,
      this.route.queryParamMap,
      this.atualizacao.pipe(startWith(undefined)),
    ]).pipe(
      switchMap(([params, query]) => {
        const id = params.get('id');
        if (!id || !/^\d+$/.test(id)) {
          return of({ ...estadoInicial, carregando: false, erro: true });
        }
        const usuarioId = Number(id);
        if (!Number.isSafeInteger(usuarioId) || usuarioId <= 0) {
          return of({ ...estadoInicial, carregando: false, erro: true });
        }
        this.estadoSeguindoAtual.set(null);
        this.erroSeguimento.set('');
        const numeroPagina = Number(query.get('portfolioPagina') || 0);
        const pagina = Number.isSafeInteger(numeroPagina) && numeroPagina >= 0 && numeroPagina <= 1_000_000
          ? numeroPagina
          : 0;
        const numeroPaginaOfertas = Number(query.get('ofertasPagina') || 0);
        const paginaOfertas = Number.isSafeInteger(numeroPaginaOfertas) && numeroPaginaOfertas >= 0 && numeroPaginaOfertas <= 1_000_000
          ? numeroPaginaOfertas
          : 0;
        return this.perfilApi.publico(usuarioId).pipe(
          switchMap((perfil) =>
            forkJoin({
              ofertas: this.anunciosApi.listarDoUsuario(perfil.id, paginaOfertas).pipe(catchError(() => of(null))),
              publicacoes: this.perfilApi.publicacoes(perfil.id, pagina).pipe(catchError(() => of(null))),
              avaliacoes: this.perfilApi.avaliacoes(perfil.id).pipe(catchError(() => of(null))),
              seguidores: this.sessao.autenticado()
                ? this.perfilApi.estadoSeguindo(perfil.id).pipe(catchError(() => of(null)))
                : of(null),
            }).pipe(
              map(({ ofertas, publicacoes, avaliacoes, seguidores }) => ({
                carregando: false,
                erro: false,
                erroOfertas: ofertas === null,
                erroAvaliacoes: avaliacoes === null,
                perfil,
                ofertas: ofertas?.itens ?? [],
                paginaOfertas: ofertas?.pagina ?? 0,
                totalPaginasOfertas: ofertas?.totalPaginas ?? 0,
                totalOfertas: ofertas?.totalItens ?? 0,
                publicacoes: publicacoes?.itens ?? [],
                erroPublicacoes: publicacoes === null,
                paginaPublicacoes: publicacoes?.pagina ?? 0,
                totalPaginasPublicacoes: publicacoes?.totalPaginas ?? 0,
                totalPublicacoes: publicacoes?.totalItens ?? 0,
                avaliacoes: avaliacoes ?? [],
                seguidores,
              })),
            ),
          ),
          catchError(() => of({ ...estadoInicial, carregando: false, erro: true })),
          startWith(estadoInicial),
        );
      }),
    ),
    { initialValue: estadoInicial },
  );

  protected readonly perfil = computed(() => this.estado().perfil);
  protected readonly ofertas = computed(() => this.estado().ofertas);
  protected readonly publicacoes = computed(() => this.estado().publicacoes);
  protected readonly avaliacoes = computed(() => this.estado().avaliacoes);
  protected readonly seguidores = computed(() => this.estadoSeguindoAtual() ?? this.estado().seguidores);
  protected readonly totalSeguidores = computed(() =>
    this.seguidores()?.totalSeguidores ?? this.perfil()?.totalSeguidores ?? null,
  );

  protected alternarSeguimento(): void {
    const perfil = this.perfil();
    const estado = this.seguidores();
    if (!perfil || !estado || estado.proprioPerfil || this.alterandoSeguimento()) return;

    this.alterandoSeguimento.set(true);
    this.erroSeguimento.set('');
    const requisicao = estado.seguindo
      ? this.perfilApi.deixarDeSeguir(perfil.id)
      : this.perfilApi.seguir(perfil.id);
    requisicao.subscribe({
      next: (novoEstado) => {
        this.estadoSeguindoAtual.set(novoEstado);
        this.alterandoSeguimento.set(false);
      },
      error: () => {
        this.erroSeguimento.set('Não foi possível atualizar o seguimento. Tente novamente.');
        this.alterandoSeguimento.set(false);
      },
    });
  }

  protected iniciarConversa(): void {
    const perfil = this.perfil();
    if (!perfil || this.abrindoConversa()) return;
    if (!this.sessao.autenticado()) {
      void this.router.navigate(['/conta'], { queryParams: { retorno: `/profissionais/${perfil.id}` } });
      return;
    }
    this.abrindoConversa.set(true);
    this.conversasApi.abrirConversa(perfil.id).subscribe({
      next: conversa => {
        void this.router.navigate(['/mensagens'], { queryParams: { conversa: `chat-${conversa.id}` } });
        this.abrindoConversa.set(false);
      },
      error: () => {
        this.erroSeguimento.set('Não foi possível abrir a conversa. Tente novamente.');
        this.abrindoConversa.set(false);
      },
    });
  }

  protected tentarNovamente(): void {
    this.atualizacao.next();
  }

  protected mudarPaginaPublicacoes(pagina: number): void {
    const atual = this.estado();
    if (pagina < 0 || pagina >= atual.totalPaginasPublicacoes || atual.carregando) return;
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { portfolioPagina: pagina === 0 ? null : pagina },
      queryParamsHandling: 'merge',
    });
  }

  protected mudarPaginaOfertas(pagina: number): void {
    const atual = this.estado();
    if (pagina < 0 || pagina >= atual.totalPaginasOfertas || atual.carregando) return;
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { ofertasPagina: pagina === 0 ? null : pagina },
      queryParamsHandling: 'merge',
    });
  }

  protected estrelas(nota: number): string {
    return '★'.repeat(nota) + '☆'.repeat(5 - nota);
  }
}
