package com.skillswap.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "skillswap.security.rate-limit.login.max-attempts=1",
        "skillswap.security.rate-limit.login.window=1m",
        "skillswap.security.rate-limit.registration.max-attempts=1",
        "skillswap.security.rate-limit.registration.window=1h"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRateLimitTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void bloqueiaTentativasDeLoginCom429ERetryAfterCompartilhandoAliases() throws Exception {
        var corpo = "{\"email\":\"inexistente@skillswap.invalid\",\"senha\":\"senha-invalida\"}";

        mvc.perform(post("/api/sessoes").with(ip("192.0.2.30"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login").with(ip("192.0.2.30"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo").value("MUITAS_REQUISICOES"))
                .andExpect(jsonPath("$.mensagem").isNotEmpty());

        mvc.perform(post("/api/sessoes").with(ip("192.0.2.31"))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void limitaCadastroCompartilhandoAliasesSemBloquearLogin() throws Exception {
        mvc.perform(post("/api/usuarios").with(ip("192.0.2.40"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/registrar").with(ip("192.0.2.40"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Pessoa\",\"email\":\"pessoa@skillswap.invalid\",\"senha\":\"senha-teste-123\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.codigo").value("MUITAS_REQUISICOES"));

        mvc.perform(post("/api/sessoes").with(ip("192.0.2.40"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"inexistente@skillswap.invalid\",\"senha\":\"senha-invalida\"}"))
                .andExpect(status().isUnauthorized());
    }

    private static RequestPostProcessor ip(String endereco) {
        return request -> {
            request.setRemoteAddr(endereco);
            return request;
        };
    }
}
