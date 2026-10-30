export type TipoConteudoDenunciado = 'PUBLICACAO' | 'AVALIACAO' | 'COMENTARIO_ANUNCIO' | 'COMENTARIO_PUBLICACAO';
export type MotivoDenuncia = 'SPAM' | 'ASSEDIO' | 'FRAUDE' | 'DADOS_PESSOAIS' | 'CONTEUDO_INADEQUADO' | 'OUTRO';
export type StatusDenuncia = 'ABERTA' | 'OCULTADA' | 'DESCARTADA' | 'REEXIBIDA';

export interface Denuncia {
  id: number;
  tipoConteudo: TipoConteudoDenunciado;
  recursoId: number;
  motivo: MotivoDenuncia;
  descricao: string | null;
  status: StatusDenuncia;
  criadaEm: string;
  analisadaEm: string | null;
  analisadaPor: string | null;
  autorId: number;
  autorNome: string;
  trechoConteudo: string | null;
}

export interface PaginaDenuncias {
  itens: readonly Denuncia[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
}

export interface HistoricoAvaliacao {
  avaliacaoId: number;
  notaAnterior: number;
  notaAnuncioAnterior: number | null;
  depoimentoAnterior: string | null;
  editorNome: string;
  registradaEm: string;
}
