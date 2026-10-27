import { DadosAnuncio } from './anuncio.model';
import { ItemColecao } from '../ui/vitrine.model';

export interface CategoriaApi {
  valor: string;
  rotulo: string;
}

export interface AnuncioApi extends Omit<DadosAnuncio, 'categoria'> {
  id: number;
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
  busca: string;
  categoria: string;
  pagina: number;
}
