package com.skillswap.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsPreflightTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void permitePreflightDaOrigemExataDaPreviaTailscale() throws Exception {
        mvc.perform(options("/api/usuarios")
                        .header("Origin", "http://100.97.254.9:8089")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://100.97.254.9:8089"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void rejeitaPreflightDeOrigemNaoCadastrada() throws Exception {
        mvc.perform(options("/api/usuarios")
                        .header("Origin", "https://origem-nao-autorizada.invalid")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
