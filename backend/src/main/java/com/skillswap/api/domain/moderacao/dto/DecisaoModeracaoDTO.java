package com.skillswap.api.domain.moderacao.dto;

import jakarta.validation.constraints.NotNull;

public record DecisaoModeracaoDTO(@NotNull AcaoModeracao acao) {
    public enum AcaoModeracao { OCULTAR, DESCARTAR, REEXIBIR }
}
