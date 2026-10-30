package com.skillswap.api.domain.conversa;

import com.skillswap.api.domain.mensagem.MensagemRepository;
import com.skillswap.api.domain.proposta.PropostaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

/** Reassocia mensagens gravadas por uma imagem anterior durante uma reversão. */
@Service @RequiredArgsConstructor
public class ReconciliadorMensagensLegadas {
    private final MensagemRepository mensagens;
    private final PropostaRepository propostas;
    private final ConversaService conversas;
    private final EntityManager entityManager;
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void reconciliar() {
        for (var proposta : propostas.findByConversaIsNull()) {
            var resultado = conversas.obterOuCriar(proposta.getAnuncio().getUsuario(), proposta.getProponente());
            proposta.setConversa(resultado.conversa());
        }
        entityManager.flush();
        mensagens.reconciliarConversasLegadas();
        mensagens.reconciliarAcordosLegados();
    }
}
