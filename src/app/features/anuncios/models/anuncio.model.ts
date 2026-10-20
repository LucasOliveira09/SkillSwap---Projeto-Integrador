export type CategoriaAnuncio = string;
export type ModalidadeAnuncio = 'venda' | 'troca';

export interface DadosAnuncio {
  titulo: string;
  descricao: string;
  categoria: CategoriaAnuncio;
  cidade: string;
  modalidade: ModalidadeAnuncio;
  preco: number | null;
  habilidadeDesejada: string;
}

export type Anuncio = Readonly<DadosAnuncio & { id: string }>;
