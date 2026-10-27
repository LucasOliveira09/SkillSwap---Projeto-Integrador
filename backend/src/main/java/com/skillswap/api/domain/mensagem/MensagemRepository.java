package com.skillswap.api.domain.mensagem;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    @EntityGraph(attributePaths = {"remetente", "acordo"})
    List<Mensagem> findByAcordoIdOrderByEnviadoEmAsc(Long acordoId);

    @EntityGraph(attributePaths = {"remetente", "acordo"})
    Page<Mensagem> findByAcordoId(Long acordoId, Pageable pageable);

    @EntityGraph(attributePaths = {"remetente", "acordo"})
    Optional<Mensagem> findByIdAndAcordoId(Long id, Long acordoId);
}
