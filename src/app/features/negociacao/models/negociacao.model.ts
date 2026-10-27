export type StatusProposta = 'PENDENTE' | 'ACEITA' | 'RECUSADA' | 'CANCELADA';
export type StatusAcordo = 'EM_ANDAMENTO' | 'CONCLUIDO' | 'CANCELADO';

export interface Proposta {
  id: number;
  anuncioId: number;
  anuncioTitulo: string;
  proponenteId: number;
  proponenteNome: string;
  mensagemInicial: string;
  status: StatusProposta;
  criadoEm: string;
}

export interface Acordo {
  id: number;
  propostaId: number;
  anuncioId: number;
  anuncioTitulo: string;
  anuncianteId: number;
  anuncianteNome: string;
  proponenteId: number;
  proponenteNome: string;
  status: StatusAcordo;
  criadoEm: string;
  concluidoEm: string | null;
}

export interface Mensagem {
  id: number;
  acordoId: number;
  remetenteId: number;
  remetenteNome: string;
  conteudo: string;
  enviadoEm: string;
}

export interface Avaliacao {
  id: number;
  acordoId: number;
  avaliadorId: number;
  avaliadorNome: string;
  avaliadoId: number;
  avaliadoNome: string;
  nota: number;
  depoimento: string | null;
  criadoEm: string;
}

export interface Pagina<T> {
  itens: readonly T[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
}
