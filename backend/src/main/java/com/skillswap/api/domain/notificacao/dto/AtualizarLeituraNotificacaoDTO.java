package com.skillswap.api.domain.notificacao.dto;

import jakarta.validation.constraints.NotNull;

public record AtualizarLeituraNotificacaoDTO(@NotNull Boolean lida) {}
