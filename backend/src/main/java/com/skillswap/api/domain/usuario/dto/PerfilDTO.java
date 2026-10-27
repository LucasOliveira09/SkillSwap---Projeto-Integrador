package com.skillswap.api.domain.usuario.dto;

import java.time.LocalDateTime;

public record PerfilDTO(
        Long id,
        String nome,
        String tituloProfissional,
        String bio,
        String cidade,
        String fotoUrl,
        String capaUrl,
        String linkedinUrl,
        String instagramUrl,
        String siteUrl,
        boolean perfilPublico,
        String especialidades,
        LocalDateTime criadoEm,
        Double mediaNotas,
        long quantidadeAvaliacoes
) {}
