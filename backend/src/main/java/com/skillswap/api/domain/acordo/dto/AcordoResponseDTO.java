package com.skillswap.api.domain.acordo.dto;

import com.skillswap.api.domain.acordo.StatusAcordo;
import java.time.LocalDateTime;

public record AcordoResponseDTO(
        Long id,
        Long propostaId,
        Long anuncioId,
        String anuncioTitulo,
        Long anuncianteId,
        String anuncianteNome,
        Long proponenteId,
        String proponenteNome,
        StatusAcordo status,
        LocalDateTime criadoEm,
        LocalDateTime concluidoEm
) {}
