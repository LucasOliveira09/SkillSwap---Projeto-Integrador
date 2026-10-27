import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AtualizacaoPerfil, PerfilProfissional } from '../models/perfil.model';

@Injectable({ providedIn: 'root' })
export class PerfilApiService {
  private readonly http = inject(HttpClient);

  meuPerfil() {
    return this.http.get<PerfilProfissional>('/api/perfil');
  }

  publico(id: number | string) {
    return this.http.get<PerfilProfissional>(`/api/perfis/${encodeURIComponent(id)}`);
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
}
