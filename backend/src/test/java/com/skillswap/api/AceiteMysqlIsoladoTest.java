package com.skillswap.api;

import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.domain.usuario.dto.RegisterDTO;
import com.skillswap.api.infra.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "SKILLSWAP_MYSQL_TEST_URL", matches = ".+")
@SpringBootTest(properties = {
        "spring.datasource.url=${SKILLSWAP_MYSQL_TEST_URL}",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.datasource.username=${SKILLSWAP_MYSQL_TEST_USER}",
        "spring.datasource.password=${SKILLSWAP_MYSQL_TEST_PASSWORD}",
        "skillswap.seed.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AceiteMysqlIsoladoTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarios;
    @Autowired JwtService jwt;

    @Test void aceitaClientesDiferentesSemDuplicarAceiteDaMesmaProposta() throws Exception {
        String sufixo = UUID.randomUUID().toString();
        String anunciante = token("Anunciante", "a-" + sufixo);
        String clienteUm = token("Cliente um", "b-" + sufixo);
        String clienteDois = token("Cliente dois", "c-" + sufixo);
        var resposta = mvc.perform(post("/api/anuncios").header("Authorization", "Bearer " + anunciante)
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"titulo":"Serviço de teste","descricao":"Atendimento local","categoria":"MANUTENCAO",
                         "cidade":"Florianópolis","modalidade":"VENDA","preco":120}
                        """)).andExpect(status().isCreated()).andReturn();
        long anuncio = id(resposta.getResponse().getContentAsString());
        long primeira = proposta(anuncio, clienteUm);
        long segunda = proposta(anuncio, clienteDois);

        var prontos = new CountDownLatch(2);
        var iniciar = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> aceitar(primeira, anunciante, prontos, iniciar));
            var b = executor.submit(() -> aceitar(primeira, anunciante, prontos, iniciar));
            assertThat(prontos.await(10, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            assertThat(java.util.List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 400);
        }
        mvc.perform(get("/api/propostas/{id}", segunda).header("Authorization", "Bearer " + clienteDois))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDENTE"));
        mvc.perform(patch("/api/propostas/{id}", segunda).header("Authorization", "Bearer " + anunciante)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACEITA\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/acordos").header("Authorization", "Bearer " + anunciante))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(2));
    }

    private String token(String nome, String identificador) {
        var usuario = usuarios.register(new RegisterDTO(nome, identificador + "@skillswap.invalid", "senha-teste-123"));
        return jwt.generateToken(usuario.getEmail());
    }

    private long proposta(long anuncio, String token) throws Exception {
        var resposta = mvc.perform(post("/api/anuncios/{id}/propostas", anuncio)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"mensagemInicial\":\"Tenho interesse\"}"))
                .andExpect(status().isCreated()).andReturn();
        return id(resposta.getResponse().getContentAsString());
    }

    private long id(String json) {
        return tools.jackson.databind.json.JsonMapper.builder().build().readTree(json).get("id").asLong();
    }

    private int aceitar(long proposta, String token, CountDownLatch prontos, CountDownLatch iniciar) throws Exception {
        prontos.countDown();
        if (!iniciar.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Início expirou");
        return mvc.perform(patch("/api/propostas/{id}", proposta).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACEITA\"}"))
                .andReturn().getResponse().getStatus();
    }

}
