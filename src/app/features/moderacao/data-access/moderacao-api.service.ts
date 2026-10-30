import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Denuncia, HistoricoAvaliacao, MotivoDenuncia, PaginaDenuncias, StatusDenuncia, TipoConteudoDenunciado } from '../models/moderacao.model';
import { AcaoContestacaoAvaliacao, PaginaContestacoes, StatusContestacaoAvaliacao, ContestacaoAvaliacao } from '../../avaliacao/models/contestacao.model';

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

  listarContestacoes(status: StatusContestacaoAvaliacao = 'ABERTA', pagina = 0) {
    return this.http.get<PaginaContestacoes>('/api/moderacao/contestacoes', {
      params: { status, pagina, tamanho: 20 },
    });
  }

  decidirContestacao(id: number, acao: AcaoContestacaoAvaliacao, decisao: string) {
    return this.http.patch<ContestacaoAvaliacao>(`/api/moderacao/contestacoes/${id}`, {
      acao, decisao,
    });
  }

  historicoAvaliacao(avaliacaoId: number) {
    return this.http.get<HistoricoAvaliacao>(`/api/moderacao/avaliacoes/${avaliacaoId}/historico`);
  }
}
