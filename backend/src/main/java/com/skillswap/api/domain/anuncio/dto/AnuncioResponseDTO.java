package com.skillswap.api.domain.anuncio.dto;

import com.skillswap.api.domain.anuncio.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AnuncioResponseDTO(
        Long id,
        String titulo,
        String descricao,
        CategoriaAnuncio categoria,
        String cidade,
        ModalidadeAnuncio modalidade,
        BigDecimal preco,
        String habilidadeDesejada,
        StatusAnuncio status,
        Long usuarioId,
        String usuarioNome,
        LocalDateTime criadoEm,
        String categoriaRotulo, String imagem, String unidade, String autor,
        String avatar, Integer estrelas, boolean exemplo,
        Double mediaAvaliacoesAnuncio, long quantidadeAvaliacoesAnuncio,
        List<DisponibilidadeAnuncioDTO> disponibilidades
) {}
