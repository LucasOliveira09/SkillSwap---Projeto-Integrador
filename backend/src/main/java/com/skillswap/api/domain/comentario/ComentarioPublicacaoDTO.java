package com.skillswap.api.domain.comentario;

import java.time.LocalDateTime;
import java.util.List;

public record ComentarioPublicacaoDTO(Long id, Long publicacaoId, Long respostaAId,
        String autorNome, String conteudo, LocalDateTime criadoEm,
        List<ComentarioPublicacaoDTO> respostas, boolean podeResponder, boolean podeDenunciar) {}
