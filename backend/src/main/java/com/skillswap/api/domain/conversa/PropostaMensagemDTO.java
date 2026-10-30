package com.skillswap.api.domain.conversa;

import com.skillswap.api.domain.proposta.Proposta;
import com.skillswap.api.domain.proposta.StatusProposta;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PropostaMensagemDTO(Long id, Long proponenteId, StatusProposta status,
        String servico, String escopo, String formaPagamento, BigDecimal valor,
        Integer parcelas, String duracao, LocalDate validadeEm) {
    public static PropostaMensagemDTO de(Proposta proposta) {
        if (proposta == null) return null;
        return new PropostaMensagemDTO(proposta.getId(), proposta.getProponente().getId(), proposta.getStatus(),
                proposta.getServico(), proposta.getEscopo(),
                proposta.getFormaPagamento() == null ? null : proposta.getFormaPagamento().name(),
                proposta.getValor(), proposta.getParcelas(), proposta.getDuracao(), proposta.getValidadeEm());
    }
}
