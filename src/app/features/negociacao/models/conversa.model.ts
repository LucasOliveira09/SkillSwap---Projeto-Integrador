import { StatusProposta, StatusAcordo } from './negociacao.model';

export interface MensagemConversa {
  id: number;
  conversaId: number;
  propostaId: number | null;
  acordoId: number | null;
  anuncioId: number | null;
  anuncioTitulo: string | null;
  cidade: string | null;
  modalidade: 'VENDA' | 'TROCA' | null;
  preco: number | null;
  remetenteId: number;
  remetenteNome: string;
  conteudo: string;
  enviadoEm: string;
  proposta: PropostaMensagem | null;
}

export interface PropostaMensagem {
  id: number;
  proponenteId: number;
  status: import('./negociacao.model').StatusProposta;
  servico: string | null;
  escopo: string | null;
  formaPagamento: FormaPagamentoProposta | null;
  valor: number | null;
  parcelas: number | null;
  duracao: string | null;
  validadeEm: string | null;
}

export interface Conversa {
  id: number;
  outroParticipanteId: number;
  outroParticipanteNome: string;
  outroFotoUrl: string | null;
  anuncioId: number | null;
  anuncioTitulo: string | null;
  anuncioImagem: string | null;
  cidade: string | null;
  modalidade: 'VENDA' | 'TROCA' | null;
  status: StatusProposta | null;
  acordoId: number | null;
  acordoStatus: StatusAcordo | null;
  podeEnviar: boolean;
  ultimaMensagem: MensagemConversa | null;
  naoLidas: number;
  criadoEm: string;
  atualizadaEm: string;
}

export type FormaPagamentoProposta = 'A_VISTA' | 'PARCELADO' | 'TROCA';

export interface NovaPropostaConversa {
  anuncioId: number;
  servico: string;
  escopo: string;
  formaPagamento: FormaPagamentoProposta;
  valor: number | null;
  parcelas: number | null;
  duracao: string | null;
  validadeEm: string | null;
}
