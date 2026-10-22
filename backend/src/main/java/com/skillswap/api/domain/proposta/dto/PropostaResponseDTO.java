package com.skillswap.api.domain.proposta.dto;

import com.skillswap.api.domain.proposta.StatusProposta;
import java.time.LocalDateTime;

public record PropostaResponseDTO(
        Long id,
        Long anuncioId,
        String anuncioTitulo,
        Long proponenteId,
        String proponenteNome,
        String mensagemInicial,
        StatusProposta status,
        LocalDateTime criadoEm
) {}
