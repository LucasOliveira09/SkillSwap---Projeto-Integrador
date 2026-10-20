export interface CartaoAnuncio {
  id: string;
  titulo: string;
  categoria: string;
  filtros: readonly string[];
  imagem?: string;
  preco: number | null;
  unidade: string;
  trocaPor: string;
  detalhes: readonly string[];
  autor: string;
  avatar?: string;
  estrelas: number | null;
  quando: string;
  cidade: string;
  exemplo: boolean;
}

export interface ItemColecao {
  titulo: string;
  imagem: string;
  parametros: { busca?: string; categoria?: string };
}
