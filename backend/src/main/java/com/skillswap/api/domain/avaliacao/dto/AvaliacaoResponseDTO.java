package com.skillswap.api.domain.avaliacao.dto;

import java.time.LocalDateTime;

public record AvaliacaoResponseDTO(
        Long id,
        Long acordoId,
        Long avaliadorId,
        String avaliadorNome,
        Long avaliadoId,
        String avaliadoNome,
        int nota,
        Integer notaAnuncio,
        String depoimento,
        LocalDateTime criadoEm,
        String respostaProfissional,
        LocalDateTime respondidaEm,
        boolean edicaoRealizada,
        LocalDateTime editadaEm
) {}
