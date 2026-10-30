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
  disponivelTrabalhos: boolean;
  totalSeguidores: number;
  trabalhosConcluidos: number;
}

export interface EstadoSeguindo {
  seguindo: boolean;
  proprioPerfil: boolean;
  totalSeguidores: number;
}

export interface ExperienciaProfissional {
  id: number;
  cargo: string;
  organizacao: string;
  cidade: string | null;
  dataInicio: string;
  dataFim: string | null;
  descricao: string | null;
}

export interface CertificacaoProfissional {
  id: number;
  nome: string;
  emissor: string;
  dataEmissao: string;
  dataValidade: string | null;
  urlCredencial: string | null;
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
  disponivelTrabalhos?: boolean;
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
  totalCurtidas: number;
  curtidaPeloUsuario: boolean;
}

export interface CurtidaPublicacao {
  publicacaoId: number;
  totalCurtidas: number;
  curtidaPeloUsuario: boolean;
}

export interface ComentarioPublicacao {
  id: number;
  publicacaoId: number;
  respostaAId: number | null;
  autorNome: string;
  conteudo: string;
  criadoEm: string;
  respostas: readonly ComentarioPublicacao[];
  podeResponder: boolean;
  podeDenunciar: boolean;
}

export interface PaginaComentariosPublicacao {
  itens: readonly ComentarioPublicacao[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
}

export interface PaginaPublicacoesPortfolio {
  itens: readonly PublicacaoPortfolio[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
}
