package com.skillswap.api.domain.acordo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventoAcordoRepository extends JpaRepository<EventoAcordo, Long> {
    List<EventoAcordo> findByAcordoIdOrderByCriadoEmAscIdAsc(Long acordoId);
}
