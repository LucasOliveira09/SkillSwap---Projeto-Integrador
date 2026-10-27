import { Component, computed, inject } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, combineLatest, forkJoin, map, of, startWith, switchMap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { AnunciosApiService } from '../../../anuncios/data-access/anuncios-api.service';
import { AnuncioApi } from '../../../anuncios/models/anuncio-api.model';
import { IconeComponent } from '../../../../shared/ui/icone/icone.component';
import { PerfilApiService } from '../../data-access/perfil-api.service';
import { PerfilProfissional, PublicacaoPortfolio } from '../../models/perfil.model';
import { Avaliacao } from '../../../negociacao/models/negociacao.model';

interface EstadoPerfil {
  carregando: boolean;
  erro: boolean;
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
}

const estadoInicial: EstadoPerfil = {
  carregando: true,
  erro: false,
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
};

@Component({
  selector: 'app-perfil-publico',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, DecimalPipe, MatButtonModule, IconeComponent],
  templateUrl: './perfil-publico.component.html',
  styleUrl: './perfil-publico.component.scss',
})
export class PerfilPublicoComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly perfilApi = inject(PerfilApiService);
  private readonly anunciosApi = inject(AnunciosApiService);

  protected readonly estado = toSignal(
    combineLatest([this.route.paramMap, this.route.queryParamMap]).pipe(
      switchMap(([params, query]) => {
        const id = params.get('id');
        if (!id || !/^\d+$/.test(id)) {
          return of({ ...estadoInicial, carregando: false, erro: true });
        }
        const usuarioId = Number(id);
        if (!Number.isSafeInteger(usuarioId) || usuarioId <= 0) {
          return of({ ...estadoInicial, carregando: false, erro: true });
        }
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
              ofertas: this.anunciosApi.listarDoUsuario(perfil.id, paginaOfertas),
              publicacoes: this.perfilApi.publicacoes(perfil.id, pagina).pipe(catchError(() => of(null))),
              avaliacoes: this.perfilApi.avaliacoes(perfil.id).pipe(catchError(() => of([]))),
            }).pipe(
              map(({ ofertas, publicacoes, avaliacoes }) => ({
                carregando: false,
                erro: false,
                perfil,
                ofertas: ofertas.itens,
                paginaOfertas: ofertas.pagina,
                totalPaginasOfertas: ofertas.totalPaginas,
                totalOfertas: ofertas.totalItens,
                publicacoes: publicacoes?.itens ?? [],
                erroPublicacoes: publicacoes === null,
                paginaPublicacoes: publicacoes?.pagina ?? 0,
                totalPaginasPublicacoes: publicacoes?.totalPaginas ?? 0,
                totalPublicacoes: publicacoes?.totalItens ?? 0,
                avaliacoes,
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
