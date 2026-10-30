package com.skillswap.api.domain.proposta;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PropostaRepository extends JpaRepository<Proposta, Long> {

    @EntityGraph(attributePaths = {"anuncio", "anuncio.usuario", "proponente"})
    List<Proposta> findByAnuncioId(Long anuncioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"anuncio", "anuncio.usuario", "proponente"})
    @Query("select p from Proposta p where p.anuncio.id = :anuncioId")
    List<Proposta> findByAnuncioIdForUpdate(@Param("anuncioId") Long anuncioId);

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

    @Query("select p.anuncio.id from Proposta p where p.id = :id")
    Optional<Long> findAnuncioIdById(@Param("id") Long id);

    boolean existsByAnuncioIdAndProponenteIdAndStatusIn(Long anuncioId, Long proponenteId, Collection<StatusProposta> statuses);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Proposta p where p.id=:id")
    Optional<Proposta> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"anuncio", "anuncio.usuario", "proponente"})
    List<Proposta> findByConversaId(Long conversaId);

    @EntityGraph(attributePaths = {"anuncio", "anuncio.usuario", "proponente"})
    List<Proposta> findByConversaIsNull();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"anuncio", "anuncio.usuario", "proponente"})
    @Query("select p from Proposta p where p.conversa.id = :conversaId")
    List<Proposta> findByConversaIdForUpdate(@Param("conversaId") Long conversaId);

}
