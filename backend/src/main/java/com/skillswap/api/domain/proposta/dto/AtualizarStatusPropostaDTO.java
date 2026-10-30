package com.skillswap.api.domain.proposta.dto;

import com.skillswap.api.domain.proposta.StatusProposta;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusPropostaDTO(@NotNull StatusProposta status) {}
