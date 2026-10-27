package com.skillswap.api.domain.acordo;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface AcordoRepository extends JpaRepository<Acordo, Long> {

    @EntityGraph(attributePaths = {"proposta", "proposta.anuncio", "proposta.anuncio.usuario", "proposta.proponente"})
    List<Acordo> findByPropostaAnuncioUsuarioIdOrPropostaProponenteId(Long anuncioUsuarioId, Long proponenteId);

    @EntityGraph(attributePaths = {"proposta", "proposta.anuncio", "proposta.anuncio.usuario", "proposta.proponente"})
    Page<Acordo> findByPropostaAnuncioUsuarioIdOrPropostaProponenteId(
            Long anuncioUsuarioId, Long proponenteId, Pageable pageable);

    @EntityGraph(attributePaths = {"proposta", "proposta.anuncio", "proposta.anuncio.usuario", "proposta.proponente"})
    Optional<Acordo> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Acordo a where a.id = :id")
    Optional<Acordo> findByIdForUpdate(@Param("id") Long id);
}
