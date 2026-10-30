package com.skillswap.api.domain.avaliacao;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContestacaoAvaliacaoRepository extends JpaRepository<ContestacaoAvaliacao, Long> {

    @EntityGraph(attributePaths = {"avaliacao", "avaliacao.avaliador", "avaliacao.avaliado", "contestador"})
    Optional<ContestacaoAvaliacao> findByAvaliacaoId(Long avaliacaoId);

    @EntityGraph(attributePaths = {"avaliacao", "avaliacao.avaliador", "avaliacao.avaliado", "contestador"})
    Optional<ContestacaoAvaliacao> findByIdAndAvaliacaoId(Long id, Long avaliacaoId);

    boolean existsByAvaliacaoId(Long avaliacaoId);

    @EntityGraph(attributePaths = {"avaliacao", "avaliacao.avaliador", "avaliacao.avaliado", "contestador"})
    Page<ContestacaoAvaliacao> findByStatusOrderByCriadaEmAscIdAsc(
            StatusContestacaoAvaliacao status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ContestacaoAvaliacao c join fetch c.avaliacao where c.id = :id")
    Optional<ContestacaoAvaliacao> findByIdForUpdate(@Param("id") Long id);
}
