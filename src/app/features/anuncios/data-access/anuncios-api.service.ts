import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { DadosAnuncio } from '../models/anuncio.model';
import {
  AnuncioApi,
  CategoriaApi,
  FiltrosAnuncios,
  PaginaAnuncios,
  StatusAnuncioApi,
  VitrineApi,
} from '../models/anuncio-api.model';

@Injectable({ providedIn: 'root' })
export class AnunciosApiService {
  private readonly http = inject(HttpClient);

  listar(filtros: FiltrosAnuncios) {
    const params = new HttpParams({
      fromObject: {
        ...filtros,
        pagina: String(filtros.pagina),
        tamanho: '5',
      },
    });
    return this.http
      .get<PaginaAnuncios>(environment.apiUrl + '/anuncios', { params })
      .pipe(
        map((pagina) => ({
          ...pagina,
          itens: pagina.itens.map((anuncio) => this.converter(anuncio)),
        })),
      );
  }

  listarDoUsuario(usuarioId: number, pagina = 0) {
    return this.http
      .get<PaginaAnuncios>(`/api/perfis/${usuarioId}/anuncios`, {
        params: { pagina, tamanho: 6 },
      })
      .pipe(
        map((resultado) => ({
          ...resultado,
          itens: resultado.itens.map((anuncio) => this.converter(anuncio)),
        })),
      );
  }

  listarMeus(pagina = 0) {
    return this.http
      .get<PaginaAnuncios>('/api/anuncios/meus', { params: { pagina, tamanho: 20 } })
      .pipe(
        map((resultado) => ({
          ...resultado,
          itens: resultado.itens.map((anuncio) => this.converter(anuncio)),
        })),
      );
  }

  obterMeu(id: number) {
    return this.http
      .get<AnuncioApi>(`/api/anuncios/meus/${id}`)
      .pipe(map((anuncio) => this.converter(anuncio)));
  }

  obter(id: string | number) {
    return this.http
      .get<AnuncioApi>(`${environment.apiUrl}/anuncios/${id}`)
      .pipe(map((anuncio) => this.converter(anuncio)));
  }

  categorias() {
    return this.http.get<readonly CategoriaApi[]>('/api/categorias');
  }
  vitrine() {
    return this.http.get<VitrineApi>('/api/vitrine');
  }
  criar(dados: DadosAnuncio) {
    return this.http
      .post<AnuncioApi>(environment.apiUrl + '/anuncios', this.payload(dados))
      .pipe(map((anuncio) => this.converter(anuncio)));
  }

  atualizar(id: number, dados: DadosAnuncio) {
    return this.http
      .put<AnuncioApi>(`${environment.apiUrl}/anuncios/${id}`, this.payload(dados))
      .pipe(map((anuncio) => this.converter(anuncio)));
  }

  atualizarStatus(id: number, status: StatusAnuncioApi) {
    return this.http.patch<AnuncioApi>(`${environment.apiUrl}/anuncios/${id}`, { status })
      .pipe(map((anuncio) => this.converter(anuncio)));
  }

  uploadImagem(arquivo: File) {
    const formData = new FormData();
    formData.append('file', arquivo);
    return this.http.post<{ url: string; nomeArquivo: string }>('/api/uploads', formData);
  }

  enviarProposta(anuncioId: number | string, mensagemInicial: string) {
    return this.http.post<{ id: number; mensagemInicial: string; status: string }>(
      `${environment.apiUrl}/anuncios/${anuncioId}/propostas`,
      { mensagemInicial },
    );
  }

  private converter(anuncio: AnuncioApi): AnuncioApi {
    return {
      ...anuncio,
      categoria: anuncio.categoria.toLowerCase(),
      modalidade: anuncio.modalidade.toLowerCase() as DadosAnuncio['modalidade'],
    };
  }

  private payload(dados: DadosAnuncio) {
    return {
      ...dados,
      categoria: dados.categoria.toUpperCase(),
      modalidade: dados.modalidade.toUpperCase(),
      preco: dados.modalidade === 'venda' ? dados.preco : null,
      imagem: dados.imagem || null,
    };
  }
}
