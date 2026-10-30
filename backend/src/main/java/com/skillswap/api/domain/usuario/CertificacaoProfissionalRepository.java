package com.skillswap.api.domain.usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CertificacaoProfissionalRepository extends JpaRepository<CertificacaoProfissional, Long> {
    List<CertificacaoProfissional> findByUsuarioIdOrderByDataEmissaoDescIdDesc(Long usuarioId);
    Optional<CertificacaoProfissional> findByIdAndUsuarioId(Long id, Long usuarioId);
    long countByUsuarioId(Long usuarioId);
}
