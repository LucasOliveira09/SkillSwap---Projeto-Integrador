package com.skillswap.api.domain.avaliacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarContestacaoAvaliacaoDTO(
        @NotBlank @Size(min = 20, max = 1000) String justificativa
) {}
