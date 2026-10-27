package com.skillswap.api.domain.moderacao.dto;

import com.skillswap.api.domain.moderacao.MotivoDenuncia;
import com.skillswap.api.domain.moderacao.StatusDenuncia;
import com.skillswap.api.domain.moderacao.TipoConteudoDenunciado;

import java.time.LocalDateTime;

public record DenunciaDTO(
        Long id,
        TipoConteudoDenunciado tipoConteudo,
        Long recursoId,
        MotivoDenuncia motivo,
        String descricao,
        StatusDenuncia status,
        LocalDateTime criadaEm,
        LocalDateTime analisadaEm,
        String analisadaPor,
        Long autorId,
        String autorNome,
        String trechoConteudo
) {}
