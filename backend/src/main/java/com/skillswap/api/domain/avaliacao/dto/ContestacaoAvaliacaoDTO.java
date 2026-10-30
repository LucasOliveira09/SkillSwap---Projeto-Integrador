package com.skillswap.api.domain.avaliacao.dto;

import com.skillswap.api.domain.avaliacao.StatusContestacaoAvaliacao;

import java.time.LocalDateTime;

public record ContestacaoAvaliacaoDTO(
        Long id,
        Long avaliacaoId,
        Long acordoId,
        Long contestadorId,
        String contestadorNome,
        String avaliadorNome,
        String avaliadoNome,
        int nota,
        Integer notaAnuncio,
        String trechoAvaliacao,
        String justificativa,
        StatusContestacaoAvaliacao status,
        LocalDateTime criadaEm,
        String decisao,
        LocalDateTime analisadaEm,
        String analisadaPor
) {}
