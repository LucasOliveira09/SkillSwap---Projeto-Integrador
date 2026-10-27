package com.skillswap.api.domain.avaliacao;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    @EntityGraph(attributePaths = {"avaliador", "avaliado", "acordo"})
    List<Avaliacao> findByAvaliadoId(Long avaliadoId);

    long countByAvaliadoId(Long avaliadoId);

    boolean existsByAcordoIdAndAvaliadorId(Long acordoId, Long avaliadorId);

    @EntityGraph(attributePaths = {"avaliador", "avaliado", "acordo"})
    List<Avaliacao> findByAcordoId(Long acordoId);

    @EntityGraph(attributePaths = {"avaliador", "avaliado", "acordo"})
    Optional<Avaliacao> findByIdAndAcordoId(Long id, Long acordoId);
}
