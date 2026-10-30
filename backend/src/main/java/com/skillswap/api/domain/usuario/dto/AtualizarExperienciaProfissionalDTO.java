package com.skillswap.api.domain.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AtualizarExperienciaProfissionalDTO(
        @NotBlank @Size(max = 120) String cargo,
        @NotBlank @Size(max = 120) String organizacao,
        @Size(max = 80) String cidade,
        @NotNull @PastOrPresent LocalDate dataInicio,
        @PastOrPresent LocalDate dataFim,
        @Size(max = 1500) String descricao) {}
