package com.skillswap.api.domain.comentario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ComentarioPublicacaoRepository extends JpaRepository<ComentarioPublicacao, Long> {
    @EntityGraph(attributePaths = {"autor", "publicacao", "publicacao.usuario"})
    Page<ComentarioPublicacao> findByPublicacaoIdAndRespostaAIsNullAndModeradaOcultaFalse(
            Long publicacaoId, Pageable pageable);

    @EntityGraph(attributePaths = {"autor", "respostaA", "publicacao", "publicacao.usuario"})
    List<ComentarioPublicacao> findByRespostaAIdInAndModeradaOcultaFalseOrderByCriadoEmAscIdAsc(
            Collection<Long> respostaAIds);

    @EntityGraph(attributePaths = {"autor", "respostaA", "publicacao", "publicacao.usuario"})
    Optional<ComentarioPublicacao> findByIdAndPublicacaoIdAndModeradaOcultaFalse(Long id, Long publicacaoId);

    @EntityGraph(attributePaths = {"autor", "respostaA", "publicacao", "publicacao.usuario"})
    Optional<ComentarioPublicacao> findByIdAndModeradaOcultaFalse(Long id);

    Optional<ComentarioPublicacao> findById(Long id);
}
