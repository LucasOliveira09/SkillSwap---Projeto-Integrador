package com.skillswap.api.domain.portfolio.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PublicacaoPortfolioDTO(
        Long id,
        Long profissionalId,
        String profissionalNome,
        Long anuncioId,
        String anuncioTitulo,
        String legenda,
        List<String> fotos,
        LocalDateTime criadoEm
) {}
