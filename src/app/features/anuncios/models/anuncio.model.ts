export const CATEGORIAS_ANUNCIO = [
  { valor: 'manutencao', rotulo: 'Manutenção e reparos' },
  { valor: 'aulas', rotulo: 'Aulas e instrução' },
] as const;

export type CategoriaAnuncio = (typeof CATEGORIAS_ANUNCIO)[number]['valor'];
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
