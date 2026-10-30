package com.skillswap.api.domain.sessao;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoPersistenteRepository extends JpaRepository<SessaoPersistente, Long> {
    Optional<SessaoPersistente> findByTokenHashAndExpiraEmAfter(String tokenHash, LocalDateTime agora);
    void deleteByTokenHash(String tokenHash);
    void deleteByExpiraEmBefore(LocalDateTime agora);
}
