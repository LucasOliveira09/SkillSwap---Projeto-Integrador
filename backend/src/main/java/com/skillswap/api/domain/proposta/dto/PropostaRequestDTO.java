package com.skillswap.api.domain.proposta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PropostaRequestDTO(
        @NotBlank(message = "Mensagem inicial é obrigatória")
        @Size(max = 500, message = "Mensagem deve ter no máximo 500 caracteres")
        String mensagemInicial
) {}
