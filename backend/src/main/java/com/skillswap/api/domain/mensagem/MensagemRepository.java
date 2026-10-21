package com.skillswap.api.domain.mensagem;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    @EntityGraph(attributePaths = {"remetente", "acordo"})
    List<Mensagem> findByAcordoIdOrderByEnviadoEmAsc(Long acordoId);
}
