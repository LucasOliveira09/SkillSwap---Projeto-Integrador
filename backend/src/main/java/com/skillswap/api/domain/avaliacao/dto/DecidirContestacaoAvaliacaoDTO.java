package com.skillswap.api.domain.avaliacao.dto;

import com.skillswap.api.domain.avaliacao.AcaoContestacaoAvaliacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecidirContestacaoAvaliacaoDTO(
        @NotNull AcaoContestacaoAvaliacao acao,
        @NotBlank @Size(min = 20, max = 1000) String decisao
) {}
