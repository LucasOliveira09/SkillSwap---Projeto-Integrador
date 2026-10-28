package com.skillswap.api.domain.anuncio.dto;

import com.skillswap.api.domain.anuncio.CategoriaAnuncio;
import com.skillswap.api.domain.anuncio.ModalidadeAnuncio;
import java.math.BigDecimal;

public record ResumoOfertaProfissionalDTO(
        Long profissionalId,
        CategoriaAnuncio categoria,
        ModalidadeAnuncio modalidade,
        Long quantidade,
        BigDecimal precoMinimo,
        BigDecimal precoMaximo
) {}
