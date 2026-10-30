export type StatusContestacaoAvaliacao = 'ABERTA' | 'MANTIDA' | 'OCULTADA';
export type AcaoContestacaoAvaliacao = 'MANTER' | 'OCULTAR';

export interface ContestacaoAvaliacao {
  id: number;
  avaliacaoId: number;
  acordoId: number;
  contestadorId: number;
  contestadorNome: string;
  avaliadorNome: string;
  avaliadoNome: string;
  nota: number;
  notaAnuncio: number | null;
  trechoAvaliacao: string | null;
  justificativa: string;
  status: StatusContestacaoAvaliacao;
  criadaEm: string;
  decisao: string | null;
  analisadaEm: string | null;
  analisadaPor: string | null;
}

export interface PaginaContestacoes {
  itens: readonly ContestacaoAvaliacao[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
}
