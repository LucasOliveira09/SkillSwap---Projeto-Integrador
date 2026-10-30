package com.skillswap.api.domain.usuario.dto;

import com.skillswap.api.domain.usuario.CertificacaoProfissional;
import java.time.LocalDate;

public record CertificacaoProfissionalDTO(Long id, String nome, String emissor, LocalDate dataEmissao,
        LocalDate dataValidade, String urlCredencial) {
    public static CertificacaoProfissionalDTO de(CertificacaoProfissional certificacao) {
        return new CertificacaoProfissionalDTO(certificacao.getId(), certificacao.getNome(), certificacao.getEmissor(),
                certificacao.getDataEmissao(), certificacao.getDataValidade(), certificacao.getUrlCredencial());
    }
}
