import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  AtualizacaoPerfil,
  PaginaPerfisProfissionais,
  PerfilProfissional,
  PaginaPublicacoesPortfolio,
  PublicacaoPortfolio,
} from '../models/perfil.model';

@Injectable({ providedIn: 'root' })
export class PerfilApiService {
  private readonly http = inject(HttpClient);

  meuPerfil() {
    return this.http.get<PerfilProfissional>('/api/perfil');
  }

  publico(id: number | string) {
    return this.http.get<PerfilProfissional>(`/api/perfis/${encodeURIComponent(id)}`);
  }

  buscarProfissionais(busca: string, cidade: string, pagina: number) {
    return this.http.get<PaginaPerfisProfissionais>('/api/profissionais', {
      params: { busca, cidade, pagina, tamanho: 12 },
    });
  }

  atualizarMeuPerfil(dados: AtualizacaoPerfil) {
    return this.http.patch<PerfilProfissional>('/api/perfil', dados);
  }

  atualizarFoto(arquivo: File) {
    const dados = new FormData();
    dados.append('file', arquivo);
    return this.http.put<PerfilProfissional>('/api/perfil/foto', dados);
  }

  removerFoto() {
    return this.http.delete<void>('/api/perfil/foto');
  }

  publicacoes(id: number | string, pagina = 0) {
    return this.http.get<PaginaPublicacoesPortfolio>(
      `/api/perfis/${encodeURIComponent(id)}/publicacoes`,
      { params: { pagina, tamanho: 12 } },
    );
  }

  enviarFotoPortfolio(arquivo: File) {
    const dados = new FormData();
    dados.append('file', arquivo);
    return this.http.post<{ url: string }>('/api/uploads', dados);
  }

  criarPublicacao(legenda: string, fotos: readonly string[], anuncioId?: number) {
    return this.http.post<PublicacaoPortfolio>('/api/publicacoes', {
      legenda, fotos, ...(anuncioId ? { anuncioId } : {}),
    });
  }

  excluirPublicacao(id: number) {
    return this.http.delete<void>(`/api/publicacoes/${id}`);
  }
}
