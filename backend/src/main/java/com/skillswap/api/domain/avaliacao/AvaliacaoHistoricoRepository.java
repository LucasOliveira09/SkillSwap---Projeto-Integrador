package com.skillswap.api.domain.avaliacao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AvaliacaoHistoricoRepository extends JpaRepository<AvaliacaoHistorico, Long> {
    @EntityGraph(attributePaths = {"editor"})
    Optional<AvaliacaoHistorico> findByAvaliacaoId(Long avaliacaoId);
}
