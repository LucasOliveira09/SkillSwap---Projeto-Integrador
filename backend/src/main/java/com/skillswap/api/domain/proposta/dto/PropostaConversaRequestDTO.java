package com.skillswap.api.domain.proposta.dto;

import com.skillswap.api.domain.proposta.FormaPagamentoProposta;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PropostaConversaRequestDTO(
        @NotNull @Positive Long anuncioId,
        @NotBlank @Size(max = 120) String servico,
        @NotBlank @Size(max = 4000) String escopo,
        @NotNull FormaPagamentoProposta formaPagamento,
        @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2) BigDecimal valor,
        @Min(2) @Max(12) Integer parcelas,
        @Size(max = 120) String duracao,
        LocalDate validadeEm
) {}
