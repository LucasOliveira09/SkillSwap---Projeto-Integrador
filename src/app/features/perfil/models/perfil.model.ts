export interface PerfilProfissional {
  id: number;
  nome: string;
  bio: string | null;
  cidade: string | null;
  fotoUrl: string | null;
  especialidades: string | null;
  criadoEm: string;
  mediaNotas: number | null;
  quantidadeAvaliacoes: number;
}

export interface AtualizacaoPerfil {
  nome: string;
  bio: string;
  cidade: string;
  especialidades: string;
}

export interface PaginaPerfisProfissionais {
  itens: readonly PerfilProfissional[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
}
