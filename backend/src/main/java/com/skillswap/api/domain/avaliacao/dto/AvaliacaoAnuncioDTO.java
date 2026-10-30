package com.skillswap.api.domain.avaliacao.dto;

import java.time.LocalDateTime;

public record AvaliacaoAnuncioDTO(Long id, String avaliadorNome, int nota,
        int notaAnuncio, String depoimento, LocalDateTime criadoEm, String respostaProfissional) {}
