package com.skillswap.api.domain.notificacao.dto;

import com.skillswap.api.domain.notificacao.TipoNotificacao;
import java.time.LocalDateTime;

public record NotificacaoDTO(
        Long id,
        TipoNotificacao tipo,
        String titulo,
        String mensagem,
        String recursoUrl,
        boolean lida,
        LocalDateTime criadaEm) {}
