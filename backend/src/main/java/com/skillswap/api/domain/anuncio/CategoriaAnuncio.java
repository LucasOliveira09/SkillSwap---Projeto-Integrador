package com.skillswap.api.domain.anuncio;

public enum CategoriaAnuncio {
    MANUTENCAO,
    AULAS,
    TRANSPORTE;

    public String rotulo() {
        return switch (this) {
            case MANUTENCAO -> "Manutenção e reparos";
            case AULAS -> "Aulas e instrução";
            case TRANSPORTE -> "Transporte local";
        };
    }
}
