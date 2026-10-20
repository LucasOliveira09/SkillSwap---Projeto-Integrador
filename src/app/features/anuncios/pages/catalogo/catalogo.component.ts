import { Component, computed, inject, linkedSignal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { AnunciosDemoService } from '../../data-access/anuncios-demo.service';
import { CATEGORIAS_ANUNCIO } from '../../models/anuncio.model';
import {
  CATEGORIAS_VISUAIS,
  DESTAQUES_VISUAIS,
  EXEMPLOS_VITRINE,
  SERVICOS_VISUAIS,
} from '../../data-access/vitrine-exemplos';
import { BannerDestaquesComponent } from '../../ui/banner-destaques/banner-destaques.component';
import { AnuncioCardComponent } from '../../ui/anuncio-card/anuncio-card.component';
import { ColecaoVisualComponent } from '../../ui/colecao-visual/colecao-visual.component';
import { IconeComponent } from '../../../../shared/ui/icone/icone.component';
import { CartaoAnuncio } from '../../ui/vitrine.model';

function normalizar(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .trim();
}

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
  private readonly route = inject(ActivatedRoute);
  protected readonly anuncios = inject(AnunciosDemoService).anuncios;
  protected readonly parametros = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });
  protected readonly pagina = linkedSignal({ source: this.parametros, computation: () => 0 });
  protected readonly categorias = CATEGORIAS_VISUAIS;
  protected readonly destaques = DESTAQUES_VISUAIS;
  protected readonly servicos = SERVICOS_VISUAIS;
  protected readonly temFiltro = computed(
    () => !!(this.parametros().get('busca') || this.parametros().get('categoria')),
  );
  protected readonly cartoes = computed<readonly CartaoAnuncio[]>(() => [
    ...this.anuncios().map(
      (anuncio): CartaoAnuncio => ({
        id: anuncio.id,
        titulo: anuncio.titulo,
        categoria: CATEGORIAS_ANUNCIO.find((categoria) => categoria.valor === anuncio.categoria)!
          .rotulo,
        filtros: [anuncio.categoria],
        preco: anuncio.preco,
        unidade: '',
        trocaPor: anuncio.habilidadeDesejada,
        detalhes: [anuncio.descricao],
        autor: 'Seu anúncio',
        estrelas: null,
        quando: 'nesta sessão',
        cidade: anuncio.cidade,
        exemplo: false,
      }),
    ),
    ...EXEMPLOS_VITRINE,
  ]);
  protected readonly filtrados = computed(() => {
    const busca = normalizar(this.parametros().get('busca') || '');
    const categoria = this.parametros().get('categoria');
    return this.cartoes().filter(
      (item) =>
        (!categoria || item.filtros.includes(categoria)) &&
        normalizar(
          [
            item.titulo,
            item.categoria,
            item.cidade,
            item.autor,
            item.trocaPor,
            ...item.detalhes,
          ].join(' '),
        ).includes(busca),
    );
  });
  protected readonly paginas = computed(() =>
    Array.from(
      { length: Math.max(1, Math.ceil(this.filtrados().length / 5)) },
      (_, indice) => indice,
    ),
  );
  protected readonly paginaAtual = computed(() =>
    Math.min(this.pagina(), this.paginas().length - 1),
  );
  protected readonly visiveis = computed(() =>
    this.filtrados().slice(this.paginaAtual() * 5, this.paginaAtual() * 5 + 5),
  );
  protected mover(direcao: number): void {
    this.pagina.set(Math.max(0, Math.min(this.paginas().length - 1, this.paginaAtual() + direcao)));
  }
}
