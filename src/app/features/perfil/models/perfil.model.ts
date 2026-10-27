export interface PerfilProfissional {
  id: number;
  nome: string;
  tituloProfissional: string | null;
  bio: string | null;
  cidade: string | null;
  fotoUrl: string | null;
  capaUrl: string | null;
  linkedinUrl?: string | null;
  instagramUrl?: string | null;
  siteUrl?: string | null;
  especialidades: string | null;
  criadoEm: string;
  mediaNotas: number | null;
  quantidadeAvaliacoes: number;
}

export interface UsoArmazenamento {
  usoBytes: number;
  limiteBytes: number;
  limitePorArquivoBytes: number;
}

export interface AtualizacaoPerfil {
  nome: string;
  tituloProfissional: string;
  bio: string;
  cidade: string;
  especialidades: string;
  linkedinUrl?: string;
  instagramUrl?: string;
  siteUrl?: string;
}

export interface PaginaPerfisProfissionais {
  itens: readonly PerfilProfissional[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
}

export interface PublicacaoPortfolio {
  id: number;
  profissionalId: number;
  profissionalNome: string;
  anuncioId: number | null;
  anuncioTitulo: string | null;
  legenda: string;
  fotos: readonly string[];
  criadoEm: string;
}

export interface PaginaPublicacoesPortfolio {
  itens: readonly PublicacaoPortfolio[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
}
