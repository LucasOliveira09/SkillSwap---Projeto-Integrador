package com.skillswap.api.domain.anuncio.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaDTO<T>(List<T> itens, int pagina, int tamanho, long totalItens, int totalPaginas) {
    public static <T> PaginaDTO<T> de(Page<T> p) {
        return new PaginaDTO<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
