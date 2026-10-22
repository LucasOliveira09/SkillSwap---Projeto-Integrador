package com.skillswap.api.domain.mensagem.dto;

import java.time.LocalDateTime;

public record MensagemResponseDTO(
        Long id,
        Long acordoId,
        Long remetenteId,
        String remetenteNome,
        String conteudo,
        LocalDateTime enviadoEm
) {}
