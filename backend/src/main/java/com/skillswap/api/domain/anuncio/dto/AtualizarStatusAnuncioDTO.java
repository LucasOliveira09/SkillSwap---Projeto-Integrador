package com.skillswap.api.domain.anuncio.dto;

import com.skillswap.api.domain.anuncio.StatusAnuncio;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusAnuncioDTO(@NotNull StatusAnuncio status) {}
