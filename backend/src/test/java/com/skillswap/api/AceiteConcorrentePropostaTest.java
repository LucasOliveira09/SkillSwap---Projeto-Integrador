package com.skillswap.api;

import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.domain.usuario.dto.RegisterDTO;
import com.skillswap.api.infra.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "skillswap.seed.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:skillswap-concorrente;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AceiteConcorrentePropostaTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarios;
    @Autowired JwtService jwt;

    @Test
    void doisAceitesSimultaneosCriamApenasUmAcordo() throws Exception {
        String sufixo = UUID.randomUUID().toString();
        var anunciante = usuarios.register(new RegisterDTO("Anunciante concorrente", "anunciante-" + sufixo + "@skillswap.invalid", "senha-teste-123"));
        var clienteUm = usuarios.register(new RegisterDTO("Cliente um", "cliente1-" + sufixo + "@skillswap.invalid", "senha-teste-123"));
        var clienteDois = usuarios.register(new RegisterDTO("Cliente dois", "cliente2-" + sufixo + "@skillswap.invalid", "senha-teste-123"));
        String tokenAnunciante = jwt.generateToken(anunciante.getEmail());
        String tokenUm = jwt.generateToken(clienteUm.getEmail());
        String tokenDois = jwt.generateToken(clienteDois.getEmail());

        var respostaAnuncio = mvc.perform(post("/api/anuncios")
                        .header("Authorization", "Bearer " + tokenAnunciante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Serviço concorrente","descricao":"Atendimento local",
                                 "categoria":"MANUTENCAO","cidade":"Florianópolis","modalidade":"VENDA",
                                 "preco":120,"habilidadeDesejada":""}
                                """))
                .andExpect(status().isCreated()).andReturn();
        long anuncioId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(respostaAnuncio.getResponse().getContentAsString()).get("id").asLong();
        long propostaUm = criarProposta(anuncioId, tokenUm);
        long propostaDois = criarProposta(anuncioId, tokenDois);

        var pronto = new CountDownLatch(2);
        var iniciar = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var aceiteUm = executor.submit(() -> aceitarQuandoIniciar(propostaUm, tokenAnunciante, pronto, iniciar));
            var aceiteDois = executor.submit(() -> aceitarQuandoIniciar(propostaDois, tokenAnunciante, pronto, iniciar));
            assertTrue(pronto.await(5, TimeUnit.SECONDS), "As duas requisições devem estar prontas");
            iniciar.countDown();

            int statusUm = aceiteUm.get(10, TimeUnit.SECONDS);
            int statusDois = aceiteDois.get(10, TimeUnit.SECONDS);
            assertTrue((statusUm == 200 && statusDois == 400) || (statusUm == 400 && statusDois == 200),
                    "Exatamente uma proposta deve ser aceita");
        }

        mvc.perform(get("/api/acordos").header("Authorization", "Bearer " + tokenAnunciante))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1));
    }

    private long criarProposta(long anuncioId, String token) throws Exception {
        var resposta = mvc.perform(post("/api/anuncios/{id}/propostas", anuncioId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensagemInicial\":\"Tenho interesse\"}"))
                .andExpect(status().isCreated()).andReturn();
        return tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(resposta.getResponse().getContentAsString()).get("id").asLong();
    }

    private int aceitarQuandoIniciar(long propostaId, String token, CountDownLatch pronto, CountDownLatch iniciar) throws Exception {
        pronto.countDown();
        if (!iniciar.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Início do teste expirou");
        return mvc.perform(patch("/api/propostas/{id}", propostaId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACEITA\"}"))
                .andReturn().getResponse().getStatus();
    }
}
