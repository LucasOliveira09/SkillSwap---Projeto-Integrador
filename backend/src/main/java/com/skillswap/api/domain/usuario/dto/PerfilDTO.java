package com.skillswap.api.domain.usuario.dto;

import java.time.LocalDateTime;

public record PerfilDTO(
        Long id,
        String nome,
        String bio,
        String cidade,
        String fotoUrl,
        String especialidades,
        LocalDateTime criadoEm,
        Double mediaNotas,
        long quantidadeAvaliacoes
) {}
