package com.skillswap.api.domain.avaliacao.dto;

import java.time.LocalDateTime;

public record AvaliacaoHistoricoDTO(
        Long avaliacaoId,
        int notaAnterior,
        Integer notaAnuncioAnterior,
        String depoimentoAnterior,
        String editorNome,
        LocalDateTime registradaEm
) {}
