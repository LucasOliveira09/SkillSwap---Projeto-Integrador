package com.skillswap.api.domain.comentario;

import java.time.LocalDateTime;

public record ComentarioAnuncioDTO(Long id, Long anuncioId, Long respostaAId,
        String autorNome, String conteudo, LocalDateTime criadoEm, boolean podeResponder,
        boolean podeDenunciar) {}
