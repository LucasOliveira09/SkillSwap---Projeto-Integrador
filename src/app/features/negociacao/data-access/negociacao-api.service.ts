import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Acordo, Avaliacao, Mensagem, Pagina, Proposta, StatusAcordo, StatusProposta } from '../models/negociacao.model';

@Injectable({ providedIn: 'root' })
export class NegociacaoApiService {
  private readonly http = inject(HttpClient);

  propostas(direcao: 'ENVIADAS' | 'RECEBIDAS', pagina = 0) {
    return this.http.get<Pagina<Proposta>>('/api/propostas', { params: { direcao, pagina, tamanho: 20 } });
  }

  atualizarProposta(id: number, status: Extract<StatusProposta, 'ACEITA' | 'RECUSADA' | 'CANCELADA'>) {
    return this.http.patch<Proposta>(`/api/propostas/${id}`, { status });
  }

  acordos(pagina = 0) {
    return this.http.get<Pagina<Acordo>>('/api/acordos', { params: { pagina, tamanho: 20 } });
  }

  acordo(id: number) {
    return this.http.get<Acordo>(`/api/acordos/${id}`);
  }

  atualizarAcordo(id: number, status: Extract<StatusAcordo, 'CONCLUIDO' | 'CANCELADO'>) {
    return this.http.patch<Acordo>(`/api/acordos/${id}`, { status });
  }

  mensagens(acordoId: number, pagina = 0) {
    return this.http.get<Pagina<Mensagem>>(`/api/acordos/${acordoId}/mensagens`, {
      params: { pagina, tamanho: 50 },
    });
  }

  enviarMensagem(acordoId: number, conteudo: string) {
    return this.http.post<Mensagem>(`/api/acordos/${acordoId}/mensagens`, { conteudo });
  }

  avaliacoes(acordoId: number) {
    return this.http.get<readonly Avaliacao[]>(`/api/acordos/${acordoId}/avaliacoes`);
  }

  avaliar(acordoId: number, nota: number, notaAnuncio: number, depoimento: string) {
    return this.http.post<Avaliacao>(`/api/acordos/${acordoId}/avaliacoes`, {
      nota, notaAnuncio, depoimento,
    });
  }
}
