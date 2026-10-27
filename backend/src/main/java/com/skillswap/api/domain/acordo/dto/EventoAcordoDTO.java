package com.skillswap.api.domain.acordo.dto;

import com.skillswap.api.domain.acordo.TipoEventoAcordo;
import java.time.LocalDateTime;

public record EventoAcordoDTO(Long id, TipoEventoAcordo tipo, Long atorId, String atorNome, LocalDateTime criadoEm) {}
