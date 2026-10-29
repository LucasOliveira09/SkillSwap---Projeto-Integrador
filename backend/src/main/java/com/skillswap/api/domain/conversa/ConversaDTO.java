package com.skillswap.api.domain.conversa;

import com.skillswap.api.domain.proposta.StatusProposta;
import com.skillswap.api.domain.acordo.StatusAcordo;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public record ConversaDTO(Long id, Long anuncioId, String anuncioTitulo, String anuncioImagem,
        String anuncioCategoria, String cidade, String modalidade, BigDecimal preco,
        Long anuncianteId, String anuncianteNome, Long proponenteId, String proponenteNome,
        StatusProposta status, Long acordoId, StatusAcordo acordoStatus, boolean podeEnviar,
        MensagemConversaDTO ultimaMensagem, long naoLidas, LocalDateTime criadoEm) {}
