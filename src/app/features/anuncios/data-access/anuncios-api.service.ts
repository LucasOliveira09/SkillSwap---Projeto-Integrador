import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DadosAnuncio } from '../models/anuncio.model';
import { AnuncioApi, CategoriaApi, FiltrosAnuncios, PaginaAnuncios, VitrineApi } from '../models/anuncio-api.model';

@Injectable({ providedIn: 'root' })
export class AnunciosApiService {
  private readonly http = inject(HttpClient);

  listar(filtros: FiltrosAnuncios) {
    return this.http.get<PaginaAnuncios>('/api/anuncios', { params: { ...filtros, tamanho: 5 } });
  }

  categorias() { return this.http.get<readonly CategoriaApi[]>('/api/categorias'); }
  vitrine() { return this.http.get<VitrineApi>('/api/vitrine'); }
  criar(dados: DadosAnuncio) { return this.http.post<AnuncioApi>('/api/anuncios', dados); }
}
