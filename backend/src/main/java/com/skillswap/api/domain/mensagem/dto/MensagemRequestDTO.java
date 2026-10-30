package com.skillswap.api.domain.mensagem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

public record MensagemRequestDTO(
        @NotBlank(message = "Conteúdo é obrigatório")
        @Size(max = 2000, message = "Conteúdo deve ter no máximo 2000 caracteres")
        String conteudo,
        @Positive(message = "O anúncio deve ser válido") Long anuncioId
) {}
