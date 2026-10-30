package com.skillswap.api.domain.anuncio;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Preenche apenas o índice novo dos anúncios já existentes no backend adotado. */
@Component
@Order(0)
@RequiredArgsConstructor
public class IndiceAnunciosInicial implements CommandLineRunner {
    private final AnuncioRepository anuncios;

    @Override @Transactional
    public void run(String... args) {
        var lote = anuncios.findTop100ByTextoBuscaIsNull();
        while (!lote.isEmpty()) {
            lote.forEach(Anuncio::indexar);
            anuncios.saveAllAndFlush(lote);
            lote = anuncios.findTop100ByTextoBuscaIsNull();
        }
    }
}
