import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { BehaviorSubject, catchError, combineLatest, map, of, startWith, switchMap } from 'rxjs';
import { AnunciosApiService } from '../../data-access/anuncios-api.service';
import { AnuncioApi, PaginaAnuncios } from '../../models/anuncio-api.model';
import { DIAS_SEMANA, DiaSemana, PERIODOS_DIA, PeriodoDia } from '../../models/anuncio.model';
import { BannerDestaquesComponent } from '../../ui/banner-destaques/banner-destaques.component';
import { AnuncioCardComponent } from '../../ui/anuncio-card/anuncio-card.component';
import { ColecaoVisualComponent } from '../../ui/colecao-visual/colecao-visual.component';
import { IconeComponent } from '../../../../shared/ui/icone/icone.component';
import { CartaoAnuncio } from '../../ui/vitrine.model';

const paginaVazia: PaginaAnuncios = {
  itens: [],
  pagina: 0,
  tamanho: 5,
  totalItens: 0,
  totalPaginas: 0,
};
const carregando = { carregando: true, erro: false, dados: paginaVazia };

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [
    RouterLink,
    MatButtonModule,
    BannerDestaquesComponent,
    AnuncioCardComponent,
    ColecaoVisualComponent,
    IconeComponent,
  ],
  templateUrl: './catalogo.component.html',
  styleUrl: './catalogo.component.scss',
})
export class CatalogoComponent {
  protected readonly dias = DIAS_SEMANA;
  protected readonly periodos = PERIODOS_DIA;
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(AnunciosApiService);
  private readonly repetir = new BehaviorSubject(0);
  protected readonly erroFiltro = signal('');
  protected readonly parametros = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });
  protected readonly temFiltro = computed(
    () => !!(
      this.parametros().get('busca') || this.parametros().get('categoria') ||
      this.parametros().get('cidade') ||
      this.parametros().get('modalidade') || this.parametros().get('precoMinimo') ||
      this.parametros().get('precoMaximo')
      || this.parametros().get('diaSemana') || this.parametros().get('periodo')
    ),
  );
  protected readonly estado = toSignal(
    combineLatest([this.route.queryParamMap, this.repetir]).pipe(
      switchMap(([parametros]) => {
        const pagina = Number(parametros.get('pagina') || 0);
        return this.api
          .listar({
            busca: parametros.get('busca') || '',
            categoria: parametros.get('categoria') || '',
            cidade: parametros.get('cidade') || '',
            modalidade: parametros.get('modalidade') || '',
            precoMinimo: parametros.get('precoMinimo') || '',
            precoMaximo: parametros.get('precoMaximo') || '',
            diaSemana: (parametros.get('diaSemana') || '') as DiaSemana | '',
            periodo: (parametros.get('periodo') || '') as PeriodoDia | '',
            pagina: Number.isSafeInteger(pagina) && pagina >= 0 && pagina <= 1000000 ? pagina : 0,
          })
          .pipe(
            map((dados) => ({ carregando: false, erro: false, dados })),
            catchError(() => of({ carregando: false, erro: true, dados: paginaVazia })),
            startWith(carregando),
          );
      }),
    ),
    { initialValue: carregando },
  );
  protected readonly vitrine = toSignal(
    this.repetir.pipe(
      switchMap(() =>
        this.api.vitrine().pipe(
          map((dados) => ({ dados, erro: false })),
          catchError(() => of({ dados: null, erro: true })),
        ),
      ),
    ),
    { initialValue: { dados: null, erro: false } },
  );
  protected readonly paginaAtual = computed(() => this.estado().dados.pagina);
  protected readonly totalPaginas = computed(() => this.estado().dados.totalPaginas);
  protected readonly paginas = computed(() => {
    const total = this.totalPaginas();
    const inicio = Math.max(0, Math.min(this.paginaAtual() - 2, total - 5));
    return Array.from({ length: Math.min(5, total) }, (_, i) => inicio + i);
  });
  protected readonly visiveis = computed(() => this.estado().dados.itens.map(this.cartao));

  protected recarregar(): void {
    this.repetir.next(this.repetir.value + 1);
  }
  protected irPara(pagina: number): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { pagina },
      queryParamsHandling: 'merge',
    });
  }
  protected submeterFiltros(
    evento: Event,
    cidade: string,
    modalidade: string,
    precoMinimo: string,
    precoMaximo: string,
    diaSemana: string,
    periodo: string,
  ): void {
    evento.preventDefault();
    const minimo = precoMinimo.trim();
    const maximo = precoMaximo.trim();
    if (minimo && maximo && Number(minimo) > Number(maximo)) {
      this.erroFiltro.set('O preço mínimo não pode ser maior que o máximo.');
      return;
    }
    this.erroFiltro.set('');
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        cidade: cidade.trim() || null,
        modalidade: modalidade || null,
        precoMinimo: minimo || null,
        precoMaximo: maximo || null,
        diaSemana: diaSemana || null,
        periodo: periodo || null,
        pagina: 0,
      },
      queryParamsHandling: 'merge',
    });
  }
  protected mover(direcao: number): void {
    this.irPara(Math.max(0, Math.min(this.totalPaginas() - 1, this.paginaAtual() + direcao)));
  }

  private cartao(anuncio: AnuncioApi): CartaoAnuncio {
    return {
      id: String(anuncio.id),
      titulo: anuncio.titulo,
      categoria: anuncio.categoriaRotulo,
      filtros: [anuncio.categoria],
      imagem: anuncio.imagem || undefined,
      preco: anuncio.preco,
      unidade: anuncio.unidade,
      trocaPor: anuncio.habilidadeDesejada,
      detalhes: anuncio.descricao.split('\n'),
      autor: anuncio.autor || 'Anunciante',
      avatar: anuncio.avatar || undefined,
      estrelas: anuncio.estrelas,
      quando: anuncio.exemplo
        ? 'Ilustrativo'
        : new Date(anuncio.criadoEm).toLocaleDateString('pt-BR'),
      cidade: anuncio.cidade,
      exemplo: anuncio.exemplo,
      autorId: anuncio.usuarioId,
    };
  }
}
