package com.skillswap.api.domain.anuncio.dto;

import com.skillswap.api.domain.anuncio.CategoriaAnuncio;
import com.skillswap.api.domain.anuncio.ModalidadeAnuncio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

public record AnuncioRequestDTO(
        @NotBlank(message = "Título é obrigatório")
        @Size(max = 100, message = "Título deve ter no máximo 100 caracteres")
        String titulo,

        @NotBlank(message = "Descrição é obrigatória")
        @Size(max = 1500, message = "Descrição deve ter no máximo 1500 caracteres")
        String descricao,

        @NotNull(message = "Categoria é obrigatória")
        CategoriaAnuncio categoria,

        @NotBlank(message = "Cidade é obrigatória")
        @Size(max = 80, message = "Cidade deve ter no máximo 80 caracteres")
        String cidade,

        @NotNull(message = "Modalidade é obrigatória")
        ModalidadeAnuncio modalidade,

        @DecimalMin("0.01") @DecimalMax("99999999.99") @Digits(integer = 8, fraction = 2)
        BigDecimal preco,

        @Size(max = 120, message = "Habilidade desejada deve ter no máximo 120 caracteres")
        String habilidadeDesejada,

        @Size(max = 500, message = "URL da imagem deve ter no máximo 500 caracteres")
        String imagem,

        @Size(max = 21, message = "Informe até 21 períodos de disponibilidade")
        List<@NotNull @Valid DisponibilidadeAnuncioDTO> disponibilidades
) {}
