package com.skillswap.api.domain.comentario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Edição do texto de um comentário pelo próprio autor. */
public record EditarComentarioDTO(
        @NotBlank(message = "O comentário não pode ficar vazio.")
        @Size(max = 1000, message = "O comentário deve ter no máximo 1000 caracteres.")
        String conteudo
) {}
