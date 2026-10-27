package com.skillswap.api.domain.portfolio.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CriarPublicacaoPortfolioDTO(
        @Size(max = 500) String legenda,
        @NotEmpty @Size(min = 1, max = 6) List<@Size(max = 120) String> fotos,
        Long anuncioId
) {}
