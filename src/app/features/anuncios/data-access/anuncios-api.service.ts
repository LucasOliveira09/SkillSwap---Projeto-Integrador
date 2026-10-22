import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { DadosAnuncio } from '../models/anuncio.model';
import {
  AnuncioApi,
  CategoriaApi,
  FiltrosAnuncios,
  PaginaAnuncios,
  VitrineApi,
} from '../models/anuncio-api.model';

@Injectable({ providedIn: 'root' })
export class AnunciosApiService {
  private readonly http = inject(HttpClient);

  listar(filtros: FiltrosAnuncios) {
    return this.http
      .get<PaginaAnuncios>(environment.apiUrl + '/anuncios', { params: { ...filtros, tamanho: 5 } })
      .pipe(
        map((pagina) => ({
          ...pagina,
          itens: pagina.itens.map((anuncio) => this.converter(anuncio)),
        })),
      );
  }

  categorias() {
    return this.http.get<readonly CategoriaApi[]>('/api/categorias');
  }
  vitrine() {
    return this.http.get<VitrineApi>('/api/vitrine');
  }
  criar(dados: DadosAnuncio) {
    return this.http
      .post<AnuncioApi>(environment.apiUrl + '/anuncios', {
        ...dados,
        categoria: dados.categoria.toUpperCase(),
        modalidade: dados.modalidade.toUpperCase(),
        preco: dados.modalidade === 'venda' ? dados.preco : null,
      })
      .pipe(map((anuncio) => this.converter(anuncio)));
  }

  private converter(anuncio: AnuncioApi): AnuncioApi {
    return {
      ...anuncio,
      categoria: anuncio.categoria.toLowerCase(),
      modalidade: anuncio.modalidade.toLowerCase() as DadosAnuncio['modalidade'],
    };
  }
}
