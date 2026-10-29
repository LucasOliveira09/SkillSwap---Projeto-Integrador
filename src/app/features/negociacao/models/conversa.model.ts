import { StatusProposta, StatusAcordo, Mensagem } from './negociacao.model';
export interface MensagemConversa extends Omit<Mensagem, 'acordoId'> { conversaId: number; }
export interface Conversa {
  id: number;
  anuncioId: number;
  anuncioTitulo: string;
  anuncioImagem: string | null;
  anuncioCategoria: string;
  cidade: string;
  modalidade: 'VENDA' | 'TROCA';
  preco: number | null;
  anuncianteId: number;
  anuncianteNome: string;
  proponenteId: number;
  proponenteNome: string;
  status: StatusProposta;
  acordoId: number | null;
  acordoStatus: StatusAcordo | null;
  podeEnviar: boolean;
  ultimaMensagem: MensagemConversa | null;
  naoLidas: number;
  criadoEm: string;
}
