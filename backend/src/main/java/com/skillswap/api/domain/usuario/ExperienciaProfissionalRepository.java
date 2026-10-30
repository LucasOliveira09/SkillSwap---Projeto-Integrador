package com.skillswap.api.domain.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ExperienciaProfissionalRepository extends JpaRepository<ExperienciaProfissional, Long> {
    List<ExperienciaProfissional> findByUsuarioIdOrderByDataInicioDescIdDesc(Long usuarioId);
    Optional<ExperienciaProfissional> findByIdAndUsuarioId(Long id, Long usuarioId);
    long countByUsuarioId(Long usuarioId);
}
