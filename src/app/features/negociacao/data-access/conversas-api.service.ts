import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Conversa, MensagemConversa, NovaPropostaConversa } from '../models/conversa.model';
import { Pagina } from '../models/negociacao.model';

@Injectable({ providedIn: 'root' })
export class ConversasApiService {
  private readonly http = inject(HttpClient);
  abrir(anuncioId: number, conteudo: string) {
    return this.http.post<Conversa>(`/api/anuncios/${anuncioId}/conversas`, { conteudo });
  }
  abrirConversa(destinatarioId: number, anuncioId?: number) {
    return this.http.post<Conversa>('/api/conversas', {
      destinatarioId,
      ...(anuncioId ? { anuncioId } : {}),
    });
  }
  listar(pagina = 0) {
    return this.http.get<Pagina<Conversa>>('/api/conversas', { params: { pagina, tamanho: 20 } });
  }
  conversa(id: number) { return this.http.get<Conversa>(`/api/conversas/${id}`); }
  conversaDaProposta(id: number) { return this.http.get<Conversa>(`/api/propostas/${id}/conversa`); }
  mensagens(id: number, pagina = 0) {
    return this.http.get<Pagina<MensagemConversa>>(`/api/conversas/${id}/mensagens`, { params: { pagina, tamanho: 50 } });
  }
  enviarMensagem(id: number, conteudo: string, anuncioId?: number) {
    return this.http.post<MensagemConversa>(`/api/conversas/${id}/mensagens`, {
      conteudo,
      ...(anuncioId ? { anuncioId } : {}),
    });
  }
  criarProposta(id: number, proposta: NovaPropostaConversa) {
    return this.http.post<Record<string, unknown>>(`/api/conversas/${id}/propostas`, proposta);
  }
  ler(id: number, ultimaMensagemId: number) {
    return this.http.patch<void>(`/api/conversas/${id}/leitura`, { ultimaMensagemId });
  }
}
