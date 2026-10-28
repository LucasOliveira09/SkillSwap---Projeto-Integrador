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
  perfilPublico?: boolean;
  especialidades: string | null;
  criadoEm: string;
  mediaNotas: number | null;
  quantidadeAvaliacoes: number;
}

export interface ResumoOfertaProfissional {
  profissionalId: number;
  categoria: string;
  modalidade: 'VENDA' | 'TROCA';
  quantidade: number;
  precoMinimo: number | null;
  precoMaximo: number | null;
}

export interface FiltrosBuscaProfissionais {
  categoria?: string;
  modalidade?: 'VENDA' | 'TROCA';
  precoMinimo?: number;
  precoMaximo?: number;
}

export interface ProfissionalEncontrado extends PerfilProfissional {
  resumoOfertas?: readonly ResumoOfertaProfissional[];
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
  perfilPublico?: boolean;
}

export interface PaginaPerfisProfissionais {
  itens: readonly ProfissionalEncontrado[];
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
