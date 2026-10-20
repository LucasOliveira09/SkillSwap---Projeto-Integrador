package br.edu.unifio.skillswap.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AnuncioRequest(
    @NotBlank @Size(max = 100) String titulo,
    @NotBlank @Size(max = 1500) String descricao,
    @NotBlank @Size(max = 40) String categoria,
    @NotBlank @Size(max = 80) String cidade,
    @NotNull @Pattern(regexp = "venda|troca") String modalidade,
    @DecimalMin("0.01") @DecimalMax("99999999.99") @Digits(integer = 8, fraction = 2) BigDecimal preco,
    @Size(max = 120) String habilidadeDesejada
) {}
