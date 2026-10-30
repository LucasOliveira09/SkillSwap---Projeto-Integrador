import { DadosAnuncio, DiaSemana, PeriodoDia } from './anuncio.model';
import { ItemColecao } from '../ui/vitrine.model';

export interface CategoriaApi {
  valor: string;
  rotulo: string;
}

export interface AnuncioApi extends Omit<DadosAnuncio, 'categoria'> {
  id: number;
  status: 'ATIVO' | 'INATIVO';
  usuarioId: number;
  categoria: string;
  categoriaRotulo: string;
  criadoEm: string;
  imagem: string | null;
  unidade: string;
  autor: string | null;
  avatar: string | null;
  estrelas: number | null;
  exemplo: boolean;
  mediaAvaliacoesAnuncio?: number | null;
  quantidadeAvaliacoesAnuncio?: number;
}

export interface ComentarioAnuncioApi {
  id: number;
  anuncioId: number;
  respostaAId: number | null;
  autorNome: string;
  conteudo: string;
  criadoEm: string;
  podeResponder: boolean;
  podeDenunciar: boolean;
}

export interface AvaliacaoAnuncioApi {
  id: number;
  avaliadorNome: string;
  nota: number;
  notaAnuncio: number;
  depoimento: string | null;
  criadoEm: string;
  respostaProfissional: string | null;
}

export type StatusAnuncioApi = AnuncioApi['status'];

export interface PaginaAnuncios {
  itens: readonly AnuncioApi[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
}

export interface VitrineApi {
  categorias: readonly ItemColecao[];
  destaques: readonly ItemColecao[];
  servicos: readonly ItemColecao[];
}

export interface FiltrosAnuncios {
  tamanho?: number;
  busca: string;
  categoria: string;
  cidade: string;
  modalidade: string;
  precoMinimo: string;
  precoMaximo: string;
  diaSemana: DiaSemana | '';
  periodo: PeriodoDia | '';
  pagina: number;
}
