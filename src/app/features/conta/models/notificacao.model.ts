export type TipoNotificacao =
  | 'PROPOSTA_RECEBIDA'
  | 'PROPOSTA_RESPONDIDA'
  | 'NOVA_MENSAGEM'
  | 'ACORDO_ATUALIZADO';

export interface Notificacao {
  readonly id: number;
  readonly tipo: TipoNotificacao;
  readonly titulo: string;
  readonly mensagem: string;
  readonly recursoUrl: string;
  readonly lida: boolean;
  readonly criadaEm: string;
}

export interface PaginaNotificacoes {
  readonly itens: readonly Notificacao[];
  readonly pagina: number;
  readonly tamanho: number;
  readonly totalItens: number;
  readonly totalPaginas: number;
}
