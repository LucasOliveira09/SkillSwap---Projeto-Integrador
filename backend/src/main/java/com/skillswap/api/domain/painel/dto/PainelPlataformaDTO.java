package com.skillswap.api.domain.painel.dto;

import java.util.Map;

/** KPIs gerais da plataforma, restritos ao perfil ADMIN. */
public record PainelPlataformaDTO(
        long usuarios,
        long anunciosAtivos,
        Map<String, Long> propostasPorStatus,
        Map<String, Long> acordosPorStatus,
        Double taxaConclusaoAcordos,
        Double mediaAvaliacoes,
        Map<String, Long> anunciosAtivosPorCategoria
) {}
