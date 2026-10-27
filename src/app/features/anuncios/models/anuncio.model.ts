export type CategoriaAnuncio = string;
export type ModalidadeAnuncio = 'venda' | 'troca';
export type DiaSemana = 'SEGUNDA' | 'TERCA' | 'QUARTA' | 'QUINTA' | 'SEXTA' | 'SABADO' | 'DOMINGO';
export type PeriodoDia = 'MANHA' | 'TARDE' | 'NOITE';

export interface DisponibilidadeAnuncio {
  diaSemana: DiaSemana;
  periodo: PeriodoDia;
}

export const DIAS_SEMANA: readonly { valor: DiaSemana; rotulo: string }[] = [
  { valor: 'SEGUNDA', rotulo: 'Segunda-feira' }, { valor: 'TERCA', rotulo: 'Terça-feira' },
  { valor: 'QUARTA', rotulo: 'Quarta-feira' }, { valor: 'QUINTA', rotulo: 'Quinta-feira' },
  { valor: 'SEXTA', rotulo: 'Sexta-feira' }, { valor: 'SABADO', rotulo: 'Sábado' },
  { valor: 'DOMINGO', rotulo: 'Domingo' },
];

export const PERIODOS_DIA: readonly { valor: PeriodoDia; rotulo: string }[] = [
  { valor: 'MANHA', rotulo: 'Manhã' }, { valor: 'TARDE', rotulo: 'Tarde' }, { valor: 'NOITE', rotulo: 'Noite' },
];

export interface DadosAnuncio {
  titulo: string;
  descricao: string;
  categoria: CategoriaAnuncio;
  cidade: string;
  modalidade: ModalidadeAnuncio;
  preco: number | null;
  habilidadeDesejada: string;
  imagem?: string | null;
  disponibilidades?: DisponibilidadeAnuncio[];
}

export type Anuncio = Readonly<DadosAnuncio & { id: string }>;
