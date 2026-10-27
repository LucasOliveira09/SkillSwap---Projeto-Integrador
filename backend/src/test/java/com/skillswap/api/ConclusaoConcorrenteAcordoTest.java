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
        "spring.datasource.url=jdbc:h2:mem:skillswap-conclusao;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConclusaoConcorrenteAcordoTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarios;
    @Autowired JwtService jwt;

    @Test
    void confirmacoesSimultaneasPreservamAsDuasPartesEConcluemUmaVez() throws Exception {
        String sufixo = UUID.randomUUID().toString();
        var anunciante = usuarios.register(new RegisterDTO("Anunciante", "anunciante-" + sufixo + "@skillswap.invalid", "senha-teste-123"));
        var proponente = usuarios.register(new RegisterDTO("Proponente", "proponente-" + sufixo + "@skillswap.invalid", "senha-teste-123"));
        String tokenAnunciante = jwt.generateToken(anunciante.getEmail());
        String tokenProponente = jwt.generateToken(proponente.getEmail());

        var respostaAnuncio = mvc.perform(post("/api/anuncios")
                        .header("Authorization", "Bearer " + tokenAnunciante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Serviço confirmado","descricao":"Atendimento local",
                                 "categoria":"MANUTENCAO","cidade":"Florianópolis","modalidade":"VENDA",
                                 "preco":120,"habilidadeDesejada":""}
                                """))
                .andExpect(status().isCreated()).andReturn();
        long anuncioId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(respostaAnuncio.getResponse().getContentAsString()).get("id").asLong();
        var respostaProposta = mvc.perform(post("/api/anuncios/{id}/propostas", anuncioId)
                        .header("Authorization", "Bearer " + tokenProponente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensagemInicial\":\"Tenho interesse\"}"))
                .andExpect(status().isCreated()).andReturn();
        long propostaId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(respostaProposta.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/propostas/{id}", propostaId)
                        .header("Authorization", "Bearer " + tokenAnunciante)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACEITA\"}"))
                .andExpect(status().isOk());
        var respostaAcordos = mvc.perform(get("/api/acordos").header("Authorization", "Bearer " + tokenAnunciante))
                .andExpect(status().isOk()).andReturn();
        long acordoId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(respostaAcordos.getResponse().getContentAsString()).get("itens").get(0).get("id").asLong();

        var terceiro = usuarios.register(new RegisterDTO("Terceiro", "terceiro-" + sufixo + "@skillswap.invalid", "senha-teste-123"));
        mvc.perform(patch("/api/acordos/{id}", acordoId)
                        .header("Authorization", "Bearer " + jwt.generateToken(terceiro.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONCLUIDO\"}"))
                .andExpect(status().isForbidden());

        var pronto = new CountDownLatch(2);
        var iniciar = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var confirmaAnunciante = executor.submit(() -> confirmarQuandoIniciar(acordoId, tokenAnunciante, pronto, iniciar));
            var confirmaProponente = executor.submit(() -> confirmarQuandoIniciar(acordoId, tokenProponente, pronto, iniciar));
            assertTrue(pronto.await(5, TimeUnit.SECONDS), "As duas confirmações devem estar prontas");
            iniciar.countDown();
            assertTrue(confirmaAnunciante.get(10, TimeUnit.SECONDS) == 200);
            assertTrue(confirmaProponente.get(10, TimeUnit.SECONDS) == 200);
        }

        mvc.perform(get("/api/acordos/{id}", acordoId).header("Authorization", "Bearer " + tokenProponente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.conclusaoConfirmadaAnunciante").value(true))
                .andExpect(jsonPath("$.conclusaoConfirmadaProponente").value(true))
                .andExpect(jsonPath("$.concluidoEm").isNotEmpty());
    }

    private int confirmarQuandoIniciar(long acordoId, String token, CountDownLatch pronto, CountDownLatch iniciar) throws Exception {
        pronto.countDown();
        if (!iniciar.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Início do teste expirou");
        return mvc.perform(patch("/api/acordos/{id}", acordoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONCLUIDO\"}"))
                .andReturn().getResponse().getStatus();
    }
}
