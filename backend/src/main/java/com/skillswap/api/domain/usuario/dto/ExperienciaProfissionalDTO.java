package com.skillswap.api.domain.usuario.dto;

import com.skillswap.api.domain.usuario.ExperienciaProfissional;
import java.time.LocalDate;

public record ExperienciaProfissionalDTO(Long id, String cargo, String organizacao, String cidade,
        LocalDate dataInicio, LocalDate dataFim, String descricao) {
    public static ExperienciaProfissionalDTO de(ExperienciaProfissional experiencia) {
        return new ExperienciaProfissionalDTO(experiencia.getId(), experiencia.getCargo(), experiencia.getOrganizacao(),
                experiencia.getCidade(), experiencia.getDataInicio(), experiencia.getDataFim(), experiencia.getDescricao());
    }
}
