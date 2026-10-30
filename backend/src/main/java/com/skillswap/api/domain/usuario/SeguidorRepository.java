package com.skillswap.api.domain.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SeguidorRepository extends JpaRepository<Seguidor, SeguidorId> {
    boolean existsById_SeguidorIdAndId_SeguidoId(Long seguidorId, Long seguidoId);
    long countById_SeguidoId(Long seguidoId);
    void deleteById_SeguidorIdAndId_SeguidoId(Long seguidorId, Long seguidoId);
}
