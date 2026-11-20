package com.skillswap.api.domain.moderacao.dto;

import com.skillswap.api.domain.moderacao.MotivoDenuncia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Edição da denúncia pelo autor enquanto ainda não foi analisada. */
public record EditarDenunciaDTO(
        @NotNull(message = "Motivo é obrigatório") MotivoDenuncia motivo,
        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres") String descricao
) {}
