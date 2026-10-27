package com.skillswap.api.domain.proposta;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PropostaRepository extends JpaRepository<Proposta, Long> {

    @EntityGraph(attributePaths = {"anuncio", "anuncio.usuario", "proponente"})
    List<Proposta> findByAnuncioId(Long anuncioId);

    @EntityGraph(attributePaths = {"anuncio", "anuncio.usuario", "proponente"})
    List<Proposta> findByAnuncioUsuarioId(Long usuarioId);

    @EntityGraph(attributePaths = {"anuncio", "anuncio.usuario", "proponente"})
    List<Proposta> findByProponenteId(Long proponenteId);

    @EntityGraph(attributePaths = {"anuncio", "anuncio.usuario", "proponente"})
    Page<Proposta> findByAnuncioUsuarioId(Long usuarioId, Pageable pageable);

    @EntityGraph(attributePaths = {"anuncio", "anuncio.usuario", "proponente"})
    Page<Proposta> findByProponenteId(Long proponenteId, Pageable pageable);

    @EntityGraph(attributePaths = {"anuncio", "anuncio.usuario", "proponente"})
    Optional<Proposta> findById(Long id);

    boolean existsByAnuncioIdAndProponenteIdAndStatusIn(Long anuncioId, Long proponenteId, Collection<StatusProposta> statuses);
}
