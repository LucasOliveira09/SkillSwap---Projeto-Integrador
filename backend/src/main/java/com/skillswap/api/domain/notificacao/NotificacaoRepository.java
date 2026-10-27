package com.skillswap.api.domain.notificacao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    Page<Notificacao> findByDestinatarioId(Long destinatarioId, Pageable pageable);
    java.util.Optional<Notificacao> findByIdAndDestinatarioId(Long id, Long destinatarioId);
}
