package br.edu.unifio.skillswap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AnuncioApiTest {
    @Autowired MockMvc mvc;

    private String venda(String titulo, String preco) {
        return """
            {"titulo":"%s","descricao":"Música para iniciantes","categoria":"aulas",
             "cidade":"Florianópolis","modalidade":"venda","preco":%s,"habilidadeDesejada":""}
            """.formatted(titulo, preco);
    }

    @Test void persisteVendaELocalizaSemAcentos() throws Exception {
        mvc.perform(post("/api/anuncios").contentType("application/json").content(venda("  Aulas de violão  ", "90.50")))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").isNumber()).andExpect(jsonPath("$.titulo").value("Aulas de violão"))
            .andExpect(jsonPath("$.exemplo").value(false)).andExpect(jsonPath("$.estrelas").isEmpty());
        mvc.perform(get("/api/anuncios").param("busca", "VIOLAO").param("categoria", "aulas"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1))
            .andExpect(jsonPath("$.itens[0].preco").value(90.5));
        mvc.perform(get("/api/anuncios").param("categoria", "manutencao"))
            .andExpect(jsonPath("$.totalItens").value(0));
    }

    @Test void gravaTrocaSemPreco() throws Exception {
        mvc.perform(post("/api/anuncios").contentType("application/json").content("""
            {"titulo":"Aula","descricao":"Aula prática","categoria":"aulas","cidade":"Ourinhos",
             "modalidade":"troca","preco":90,"habilidadeDesejada":"  Pintura  "}
            """))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.preco").isEmpty())
            .andExpect(jsonPath("$.habilidadeDesejada").value("Pintura"));
    }

    @Test void rejeitaEspacosCategoriaInvalidaEPrecoInvalido() throws Exception {
        for (String corpo : new String[]{venda("   ", "90"), venda("Aula", "0"), venda("Aula", "1.001"),
                venda("Aula", "90").replace("aulas", "inexistente"), venda("Aula", "100000000")}) {
            mvc.perform(post("/api/anuncios").contentType("application/json").content(corpo))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
        }
        mvc.perform(get("/api/anuncios")).andExpect(jsonPath("$.totalItens").value(0));
    }

    @Test void rejeitaTrocaSemHabilidadeECamposGerenciadosPeloServidor() throws Exception {
        mvc.perform(post("/api/anuncios").contentType("application/json")
                .content(venda("Aula", "90").replace("venda", "troca")))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/anuncios").contentType("application/json")
                .content(venda("Aula", "90").replace("\"titulo\"", "\"exemplo\":true,\"titulo\"")))
            .andExpect(status().isBadRequest());
    }

    @Test void paginaComOrdemEstavelETrataBuscaLiteral() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/anuncios").contentType("application/json").content(venda("Aula " + i, "10")))
                .andExpect(status().isCreated());
        }
        mvc.perform(get("/api/anuncios").param("tamanho", "2").param("pagina", "1"))
            .andExpect(jsonPath("$.totalItens").value(3)).andExpect(jsonPath("$.totalPaginas").value(2))
            .andExpect(jsonPath("$.itens.length()").value(1)).andExpect(jsonPath("$.itens[0].titulo").value("Aula 0"));
        mvc.perform(get("/api/anuncios").param("busca", "%"))
            .andExpect(jsonPath("$.totalItens").value(0));
    }

    @Test void validaParametrosENaoExpoeFalhasInternas() throws Exception {
        for (String query : new String[]{"pagina=-1", "tamanho=51", "pagina=abc", "busca=" + "a".repeat(101)}) {
            mvc.perform(get("/api/anuncios?" + query)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
        }
        mvc.perform(get("/api/anuncios/999999")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }
}
