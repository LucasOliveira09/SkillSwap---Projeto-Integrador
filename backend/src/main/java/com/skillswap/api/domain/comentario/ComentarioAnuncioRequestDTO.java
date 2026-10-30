package com.skillswap.api.domain.comentario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ComentarioAnuncioRequestDTO(
        @NotBlank @Size(max = 1000) String conteudo,
        @Positive Long respostaAId
) {}
