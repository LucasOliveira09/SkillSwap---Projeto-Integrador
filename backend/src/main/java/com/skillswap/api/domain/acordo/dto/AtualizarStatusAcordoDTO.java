package com.skillswap.api.domain.acordo.dto;

import com.skillswap.api.domain.acordo.StatusAcordo;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusAcordoDTO(@NotNull StatusAcordo status) {}
