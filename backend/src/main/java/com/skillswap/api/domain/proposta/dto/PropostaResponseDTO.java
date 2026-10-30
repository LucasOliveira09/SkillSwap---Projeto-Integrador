package com.skillswap.api.domain.proposta.dto;

import com.skillswap.api.domain.proposta.StatusProposta;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.skillswap.api.domain.proposta.FormaPagamentoProposta;

public record PropostaResponseDTO(
        Long id,
        Long anuncioId,
        String anuncioTitulo,
        Long proponenteId,
        String proponenteNome,
        String mensagemInicial,
        StatusProposta status,
        LocalDateTime criadoEm,
        String servico,
        String escopo,
        FormaPagamentoProposta formaPagamento,
        BigDecimal valor,
        Integer parcelas,
        String duracao,
        LocalDate validadeEm
) {}
