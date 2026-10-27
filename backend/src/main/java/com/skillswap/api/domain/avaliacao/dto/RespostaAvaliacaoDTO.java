package com.skillswap.api.domain.avaliacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RespostaAvaliacaoDTO(
        @NotBlank @Size(max = 1000) String resposta
) {}
