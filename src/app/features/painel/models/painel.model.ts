/** Indicadores da conta autenticada (GET /api/painel). */
export interface PainelUsuario {
  anunciosAtivos: number;
  anunciosPausados: number;
  propostasRecebidasPendentes: number;
  propostasEnviadasPendentes: number;
  acordosEmAndamento: number;
  acordosConcluidos: number;
  /** Percentual das propostas recebidas e respondidas que foram aceitas; null sem respostas. */
  taxaAceitacao: number | null;
  notaMedia: number | null;
  avaliacoesRecebidas: number;
}

/** Indicadores gerais da plataforma (GET /api/painel/plataforma, perfil ADMIN). */
export interface PainelPlataforma {
  usuarios: number;
  anunciosAtivos: number;
  propostasPorStatus: Record<string, number>;
  acordosPorStatus: Record<string, number>;
  taxaConclusaoAcordos: number | null;
  mediaAvaliacoes: number | null;
  anunciosAtivosPorCategoria: Record<string, number>;
}
