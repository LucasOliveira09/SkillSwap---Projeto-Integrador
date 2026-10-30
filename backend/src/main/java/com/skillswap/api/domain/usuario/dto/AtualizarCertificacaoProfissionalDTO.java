package com.skillswap.api.domain.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AtualizarCertificacaoProfissionalDTO(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Size(max = 120) String emissor,
        @NotNull @PastOrPresent LocalDate dataEmissao,
        LocalDate dataValidade,
        @Size(max = 500) String urlCredencial) {}
