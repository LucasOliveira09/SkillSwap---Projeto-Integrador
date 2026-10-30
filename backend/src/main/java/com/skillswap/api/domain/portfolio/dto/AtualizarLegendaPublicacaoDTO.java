package com.skillswap.api.domain.portfolio.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AtualizarLegendaPublicacaoDTO(@NotNull @Size(max = 500) String legenda) {}
