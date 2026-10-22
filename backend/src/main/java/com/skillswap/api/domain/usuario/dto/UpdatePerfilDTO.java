package com.skillswap.api.domain.usuario.dto;

import jakarta.validation.constraints.Size;

public record UpdatePerfilDTO(
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        String nome,

        @Size(max = 500, message = "Bio deve ter no máximo 500 caracteres")
        String bio,

        @Size(max = 80, message = "Cidade deve ter no máximo 80 caracteres")
        String cidade,

        String fotoUrl,

        @Size(max = 500, message = "Especialidades deve ter no máximo 500 caracteres")
        String especialidades
) {}
