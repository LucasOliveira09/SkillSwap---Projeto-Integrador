package com.skillswap.api.domain.moderacao.dto;

import com.skillswap.api.domain.moderacao.MotivoDenuncia;
import com.skillswap.api.domain.moderacao.TipoConteudoDenunciado;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CriarDenunciaDTO(
        @NotNull TipoConteudoDenunciado tipoConteudo,
        @NotNull @Positive Long recursoId,
        @NotNull MotivoDenuncia motivo,
        @Size(max = 500) String descricao
) {}
