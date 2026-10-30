package com.skillswap.api.domain.conversa;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ConversaRepository extends JpaRepository<Conversa, Long> {
    @EntityGraph(attributePaths = {"participanteMenor", "participanteMaior", "anuncioContexto"})
    Optional<Conversa> findByParticipanteMenorIdAndParticipanteMaiorId(Long menorId, Long maiorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"participanteMenor", "participanteMaior", "anuncioContexto"})
    @Query("select c from Conversa c where c.id = :id")
    Optional<Conversa> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"participanteMenor", "participanteMaior", "anuncioContexto"})
    @Query("select c from Conversa c where c.participanteMenor.id = :usuario or c.participanteMaior.id = :usuario")
    Page<Conversa> listarPorParticipante(@Param("usuario") Long usuario, Pageable pageable);

    @EntityGraph(attributePaths = {"participanteMenor", "participanteMaior", "anuncioContexto"})
    Optional<Conversa> findById(Long id);
}
