package com.skillswap.api.domain.conversa;

import com.skillswap.api.domain.acordo.StatusAcordo;
import com.skillswap.api.domain.proposta.StatusProposta;
import java.time.LocalDateTime;

public record ConversaDTO(Long id, Long outroParticipanteId, String outroParticipanteNome, String outroFotoUrl,
        Long anuncioId, String anuncioTitulo, String anuncioImagem, String cidade, String modalidade,
        StatusProposta status, Long acordoId, StatusAcordo acordoStatus, boolean podeEnviar,
        MensagemConversaDTO ultimaMensagem, long naoLidas, LocalDateTime criadoEm, LocalDateTime atualizadaEm) {}
