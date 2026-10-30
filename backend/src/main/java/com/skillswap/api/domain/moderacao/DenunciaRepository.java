package com.skillswap.api.domain.moderacao;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface DenunciaRepository extends JpaRepository<Denuncia, Long> {
    boolean existsByAutorIdAndTipoConteudoAndRecursoId(Long autorId, TipoConteudoDenunciado tipo, Long recursoId);

    boolean existsByTipoConteudoAndRecursoId(TipoConteudoDenunciado tipo, Long recursoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Denuncia d join fetch d.autor where d.id = :id")
    Optional<Denuncia> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = "autor")
    Page<Denuncia> findByStatusOrderByCriadaEmAscIdAsc(StatusDenuncia status, Pageable pageable);
}
