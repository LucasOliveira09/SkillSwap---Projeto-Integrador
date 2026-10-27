import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Notificacao, PaginaNotificacoes } from '../models/notificacao.model';

@Injectable({ providedIn: 'root' })
export class NotificacoesApiService {
  private readonly http = inject(HttpClient);

  listar() {
    return this.http.get<PaginaNotificacoes>('/api/notificacoes?pagina=0&tamanho=10');
  }

  atualizarLeitura(id: number, lida: boolean) {
    return this.http.patch<Notificacao>(`/api/notificacoes/${id}`, { lida });
  }
}
