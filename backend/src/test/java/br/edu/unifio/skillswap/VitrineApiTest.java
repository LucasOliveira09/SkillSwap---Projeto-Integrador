package br.edu.unifio.skillswap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import br.edu.unifio.skillswap.config.VitrineInicial;
import br.edu.unifio.skillswap.repositories.AnuncioRepository;
import br.edu.unifio.skillswap.repositories.ItemVitrineRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"skillswap.seed.enabled=true", "spring.datasource.url=jdbc:h2:mem:vitrine;MODE=MySQL;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VitrineApiTest {
    @Autowired MockMvc mvc;
    @Autowired VitrineInicial carga;
    @Autowired AnuncioRepository anuncios;
    @Autowired ItemVitrineRepository itens;

    @Test @Transactional void repetirCargaNaoDuplicaNemSobrescreveDados() throws Exception {
        var existente = anuncios.findAll().getFirst();
        existente.setTitulo("Título preservado");
        anuncios.saveAndFlush(existente);
        carga.run();
        carga.run();
        assertThat(anuncios.count()).isEqualTo(5);
        assertThat(itens.count()).isEqualTo(21);
        assertThat(anuncios.findById(existente.getId()).orElseThrow().getTitulo()).isEqualTo("Título preservado");
    }

    @Test void serveExemplosEColecoesDoBanco() throws Exception {
        mvc.perform(get("/api/anuncios"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(5))
            .andExpect(jsonPath("$.itens[0].exemplo").value(true));
        mvc.perform(get("/api/vitrine"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.categorias.length()").value(9))
            .andExpect(jsonPath("$.destaques.length()").value(6))
            .andExpect(jsonPath("$.servicos.length()").value(6))
            .andExpect(jsonPath("$.categorias[0].titulo").value("Eletrônicos"));
        mvc.perform(get("/api/categorias")).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].valor").value("manutencao"));
    }
}
