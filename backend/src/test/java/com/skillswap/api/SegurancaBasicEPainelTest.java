package com.skillswap.api;

import com.skillswap.api.domain.usuario.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP Basic com perfis USER/ADMIN, dashboard e regras de exclusão de usuário. */
@SpringBootTest(properties = {
        "skillswap.seed.enabled=false",
        "skillswap.demo.enabled=true",
        "skillswap.demo.admin-password=Admin@Teste2026",
        "skillswap.demo.user-password=Usuario@Teste2026"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SegurancaBasicEPainelTest {
    private static final String ADMIN = "admin@skillswap.dev";
    private static final String USUARIO = "cliente@skillswap.dev";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;

    @Test
    void semCredenciaisOuComSenhaErradaResponde401() throws Exception {
        mvc.perform(get("/api/painel")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
        mvc.perform(get("/api/painel").with(httpBasic(USUARIO, "senha-errada")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioComumAcessaPainelMasNaoOperacoesDeAdmin() throws Exception {
        mvc.perform(get("/api/painel").with(httpBasic(USUARIO, "Usuario@Teste2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anunciosAtivos").value(0));
        mvc.perform(get("/api/painel/plataforma").with(httpBasic(USUARIO, "Usuario@Teste2026")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        mvc.perform(get("/api/usuarios").with(httpBasic(USUARIO, "Usuario@Teste2026")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/moderacao/acesso").with(httpBasic(USUARIO, "Usuario@Teste2026")))
                .andExpect(status().isOk()).andExpect(jsonPath("$").value(false));
    }

    @Test
    void adminListaUsuariosEVePainelDaPlataforma() throws Exception {
        mvc.perform(get("/api/usuarios").with(httpBasic(ADMIN, "Admin@Teste2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(3));
        mvc.perform(get("/api/painel/plataforma").with(httpBasic(ADMIN, "Admin@Teste2026")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarios").value(3));
    }

    @Test
    void exclusaoDeUsuarioRespeitaVinculosEPerfil() throws Exception {
        Long profissional = usuarios.findByEmail("profissional@skillswap.dev").orElseThrow().getId();
        mvc.perform(post("/api/anuncios").with(httpBasic("profissional@skillswap.dev", "Usuario@Teste2026"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Aulas de violão","descricao":"Aulas para iniciantes com repertório popular.",
                                 "categoria":"AULAS","cidade":"Florianópolis","modalidade":"VENDA","preco":80.00}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(delete("/api/usuarios/{id}", profissional).with(httpBasic(USUARIO, "Usuario@Teste2026")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/usuarios/{id}", profissional).with(httpBasic(ADMIN, "Admin@Teste2026")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO"));
        mvc.perform(delete("/api/usuarios/{id}", 999_999).with(httpBasic(ADMIN, "Admin@Teste2026")))
                .andExpect(status().isNotFound());

        Long cliente = usuarios.findByEmail(USUARIO).orElseThrow().getId();
        mvc.perform(delete("/api/usuarios/{id}", cliente).with(httpBasic(ADMIN, "Admin@Teste2026")))
                .andExpect(status().isNoContent());
    }

    @Test
    void validacaoDevolveMensagemPorCampo() throws Exception {
        mvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"email\":\"invalido\",\"senha\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").value("Email inválido"))
                .andExpect(jsonPath("$.campos.senha").value("Senha deve ter entre 6 e 100 caracteres"));
    }
}
