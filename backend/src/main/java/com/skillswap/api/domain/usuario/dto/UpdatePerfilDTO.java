package com.skillswap.api.domain.usuario.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record UpdatePerfilDTO(
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        @Pattern(regexp = ".*\\S.*", message = "Nome não pode conter apenas espaços")
        String nome,

        @Size(max = 500, message = "Bio deve ter no máximo 500 caracteres")
        String bio,

        @Size(max = 80, message = "Cidade deve ter no máximo 80 caracteres")
        @Pattern(regexp = "^$|.*\\S.*", message = "Cidade não pode conter apenas espaços")
        String cidade,

        @Size(max = 500, message = "URL da foto deve ter no máximo 500 caracteres")
        String fotoUrl,

        @Size(max = 500, message = "Especialidades deve ter no máximo 500 caracteres")
        String especialidades
) {}
