package com.skillswap.api;

import com.skillswap.api.domain.sessao.SessaoPersistenteRepository;
import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.domain.usuario.dto.RegisterDTO;
import com.skillswap.api.infra.security.SessaoCookie;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "skillswap.security.rate-limit.login.max-attempts=100")
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class SessaoPersistenteTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarios;
    @Autowired SessaoPersistenteRepository sessoes;
    private static final String LOGIN = "{\"email\":\"cookies@skillswap.invalid\",\"senha\":\"senha-teste-123\"}";
    @BeforeEach void preparar() {
        usuarios.register(new RegisterDTO("Conta Teste", "cookies@skillswap.invalid", "senha-teste-123"));
    }
    private Cookie entrar() throws Exception {
        var resposta = mvc.perform(post("/api/sessoes").contentType("application/json").content(LOGIN)
                .header(SessaoCookie.HEADER, "1"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse();
        String cookie = resposta.getHeader("Set-Cookie");
        assertThat(cookie).contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/api/sessoes", "Max-Age=2592000");
        return new Cookie(SessaoCookie.NOME, cookie.split("=", 2)[1].split(";", 2)[0]);
    }
    @Test void restauraSessaoERevogaAoSair() throws Exception {
        Cookie cookie = entrar();
        assertThat(sessoes.findAll()).hasSize(1);
        assertThat(sessoes.findAll().getFirst().getTokenHash()).hasSize(64).isNotEqualTo(cookie.getValue());
        mvc.perform(post("/api/sessoes/atual").cookie(cookie).header(SessaoCookie.HEADER, "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());
        mvc.perform(get("/api/perfil").cookie(cookie)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/sessoes/atual").cookie(cookie).header(SessaoCookie.HEADER, "1"))
                .andExpect(status().isNoContent()).andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
        mvc.perform(post("/api/sessoes/atual").cookie(cookie).header(SessaoCookie.HEADER, "1"))
                .andExpect(status().isUnauthorized());
        assertThat(sessoes.findAll()).isEmpty();
    }
    @Test void rejeitaSessaoExpiradaEApagaCookie() throws Exception {
        Cookie cookie = entrar();
        var sessao = sessoes.findAll().getFirst();
        sessao.setExpiraEm(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        sessoes.saveAndFlush(sessao);
        mvc.perform(post("/api/sessoes/atual").cookie(cookie).header(SessaoCookie.HEADER, "1"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));
    }
    @Test void exigeHeaderContraCsrfENaoRevogaPorFormularioExterno() throws Exception {
        Cookie cookie = entrar();
        mvc.perform(post("/api/sessoes/atual").cookie(cookie)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/sessoes/atual").cookie(cookie)).andExpect(status().isForbidden());
        mvc.perform(post("/api/sessoes/atual").cookie(cookie).header(SessaoCookie.HEADER, "1")
                .header("Sec-Fetch-Site", "cross-site")).andExpect(status().isForbidden());
        mvc.perform(post("/api/sessoes/atual").cookie(cookie).header(SessaoCookie.HEADER, "1")
                .header("Origin", "https://untrusted.invalid")).andExpect(status().isForbidden());
        assertThat(sessoes.findAll()).hasSize(1);
    }
    @Test void visitanteSemCookieELoginLegadoContinuamCompativeis() throws Exception {
        mvc.perform(post("/api/sessoes/atual").header(SessaoCookie.HEADER, "1"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/sessoes").contentType("application/json").content(LOGIN))
                .andExpect(status().isOk()).andExpect(header().doesNotExist("Set-Cookie"));
        assertThat(sessoes.findAll()).isEmpty();
    }
}
