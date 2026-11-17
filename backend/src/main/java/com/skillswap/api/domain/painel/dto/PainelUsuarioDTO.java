package com.skillswap.api.domain.painel.dto;

/**
 * KPIs da conta autenticada, calculados a partir dos dados persistidos.
 * taxaAceitacao: percentual das propostas recebidas e já respondidas que foram aceitas (null sem respostas).
 */
public record PainelUsuarioDTO(
        long anunciosAtivos,
        long anunciosPausados,
        long propostasRecebidasPendentes,
        long propostasEnviadasPendentes,
        long acordosEmAndamento,
        long acordosConcluidos,
        Double taxaAceitacao,
        Double notaMedia,
        long avaliacoesRecebidas
) {}
