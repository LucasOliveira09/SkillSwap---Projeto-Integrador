import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Denuncia, MotivoDenuncia, PaginaDenuncias, StatusDenuncia, TipoConteudoDenunciado } from '../models/moderacao.model';

@Injectable({ providedIn: 'root' })
export class ModeracaoApiService {
  private readonly http = inject(HttpClient);

  acesso() {
    return this.http.get<boolean>('/api/moderacao/acesso');
  }

  denunciar(tipoConteudo: TipoConteudoDenunciado, recursoId: number, motivo: MotivoDenuncia, descricao: string) {
    return this.http.post<Denuncia>('/api/denuncias', { tipoConteudo, recursoId, motivo, descricao });
  }

  listar(status: StatusDenuncia = 'ABERTA', pagina = 0) {
    return this.http.get<PaginaDenuncias>('/api/moderacao/denuncias', {
      params: { status, pagina, tamanho: 20 },
    });
  }

  decidir(id: number, acao: 'OCULTAR' | 'DESCARTAR' | 'REEXIBIR') {
    return this.http.patch<Denuncia>(`/api/moderacao/denuncias/${id}`, { acao });
  }
}
