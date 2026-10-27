package com.skillswap.api.domain.anuncio.dto;

import com.skillswap.api.domain.anuncio.DiaSemana;
import com.skillswap.api.domain.anuncio.PeriodoDia;
import jakarta.validation.constraints.NotNull;

public record DisponibilidadeAnuncioDTO(
        @NotNull DiaSemana diaSemana,
        @NotNull PeriodoDia periodo
) {}
