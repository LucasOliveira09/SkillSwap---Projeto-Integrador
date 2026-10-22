package com.skillswap.api.domain.avaliacao.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AvaliacaoRequestDTO(
        @Min(value = 1, message = "Nota mínima é 1")
        @Max(value = 5, message = "Nota máxima é 5")
        int nota,

        @Size(max = 1000, message = "Depoimento deve ter no máximo 1000 caracteres")
        String depoimento
) {}
