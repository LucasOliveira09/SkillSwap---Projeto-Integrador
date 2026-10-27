package com.skillswap.api;

import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.domain.usuario.dto.RegisterDTO;
import com.skillswap.api.infra.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "skillswap.seed.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PerfilControllerTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarios;
    @Autowired JwtService jwt;

    private Long usuarioId;
    private String token;

    @BeforeEach
    void criarUsuarioAutenticado() {
        var usuario = usuarios.register(new RegisterDTO("Ana Profissional", "perfil@skillswap.invalid", "senha-teste-123"));
        usuarioId = usuario.getId();
        token = jwt.generateToken(usuario.getEmail());
    }

    @Test
    void perfilPublicoNaoExpoeEmailNemCredenciais() throws Exception {
        mvc.perform(get("/api/perfis/{id}", usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuarioId))
                .andExpect(jsonPath("$.nome").value("Ana Profissional"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.quantidadeAvaliacoes").value(0));
    }

    @Test
    void usuarioConsultaEAtualizaSeuPerfilPeloToken() throws Exception {
        mvc.perform(get("/api/perfil").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuarioId));

        mvc.perform(patch("/api/perfil")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Ana Lima","bio":"Aulas e serviços locais","cidade":"Florianópolis",
                                 "especialidades":"Aulas de violão, iniciação musical"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Lima"))
                .andExpect(jsonPath("$.bio").value("Aulas e serviços locais"))
                .andExpect(jsonPath("$.cidade").value("Florianópolis"))
                .andExpect(jsonPath("$.especialidades").value("Aulas de violão, iniciação musical"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void editarPerfilExigeAutenticacao() throws Exception {
        mvc.perform(patch("/api/perfil")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"Texto\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void perfilPublicoListaSomenteOfertasAtivasDoProfissional() throws Exception {
        mvc.perform(post("/api/anuncios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Aulas de violão","descricao":"Para iniciantes", "categoria":"AULAS",
                                 "cidade":"Florianópolis","modalidade":"VENDA","preco":80,"habilidadeDesejada":""}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/perfis/{id}/anuncios", usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].titulo").value("Aulas de violão"))
                .andExpect(jsonPath("$.itens[0].usuarioId").value(usuarioId))
                .andExpect(jsonPath("$.itens[0].exemplo").value(false));
    }

    @Test
    void buscaProfissionaisPorEspecialidadeECidadeSemIncluirPerfisVazios() throws Exception {
        mvc.perform(patch("/api/perfil")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bio":"Atendo residências","cidade":"Florianópolis - SC",
                                 "especialidades":"Eletricista, instalações"}
                                """))
                .andExpect(status().isOk());

        usuarios.register(new RegisterDTO("Conta sem perfil", "semperfil@skillswap.invalid", "senha-teste-123"));
        mvc.perform(get("/api/profissionais").param("busca", "Eletricista").param("cidade", "Floria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(usuarioId))
                .andExpect(jsonPath("$.itens[0].especialidades").value("Eletricista, instalações"))
                .andExpect(jsonPath("$.itens[0].email").doesNotExist())
                .andExpect(jsonPath("$.itens[1]").doesNotExist());

        mvc.perform(get("/api/profissionais").param("busca", "Eletricista").param("cidade", "Palhoça"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(0));
    }

    @Test
    void atualizaFotoDoPerfilSomentePorUploadAutenticado() throws Exception {
        var foto = new org.springframework.mock.web.MockMultipartFile(
                "file", "perfil.png", "image/png",
                java.util.Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aQU0AAAAASUVORK5CYII="));

        var resultado = mvc.perform(multipart("/api/perfil/foto").file(foto).with(request -> {
                    request.setMethod("PUT");
                    return request;
                }).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl").isNotEmpty())
                .andReturn();
        String fotoUrl = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(resultado.getResponse().getContentAsString()).get("fotoUrl").asText();

        mvc.perform(get(fotoUrl)).andExpect(status().isOk());
        mvc.perform(get("/api/perfil").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fotoUrl").value(fotoUrl));
        mvc.perform(multipart("/api/perfil/foto").file(foto).with(request -> {
                    request.setMethod("PUT");
                    return request;
                }).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fotoUrl").value(fotoUrl));
        mvc.perform(multipart("/api/perfil/foto").file(foto).with(request -> {
                    request.setMethod("PUT");
                    return request;
                }))
                .andExpect(status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/perfil/foto")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/perfil").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fotoUrl").isEmpty());
    }
}
