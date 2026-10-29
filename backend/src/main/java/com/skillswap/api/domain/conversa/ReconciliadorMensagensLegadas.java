package com.skillswap.api.domain.conversa;

import com.skillswap.api.domain.mensagem.MensagemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reassocia mensagens gravadas por uma imagem anterior durante uma reversão. */
@Service @RequiredArgsConstructor
public class ReconciliadorMensagensLegadas {
    private final MensagemRepository mensagens;
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void reconciliar() { mensagens.reconciliarLegadas(); }
}
