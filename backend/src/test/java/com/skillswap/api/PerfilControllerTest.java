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
                                 "especialidades":"Aulas de violão, iniciação musical",
                                 "tituloProfissional":"Instrutora de violão"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Lima"))
                .andExpect(jsonPath("$.bio").value("Aulas e serviços locais"))
                .andExpect(jsonPath("$.cidade").value("Florianópolis"))
                .andExpect(jsonPath("$.especialidades").value("Aulas de violão, iniciação musical"))
                .andExpect(jsonPath("$.tituloProfissional").value("Instrutora de violão"))
                .andExpect(jsonPath("$.email").doesNotExist());

        mvc.perform(get("/api/perfis/{id}", usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tituloProfissional").value("Instrutora de violão"))
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
    void consultaUsoDaContaEExigeAutenticacao() throws Exception {
        mvc.perform(get("/api/uploads/uso"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/uploads/uso").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usoBytes").value(0))
                .andExpect(jsonPath("$.limiteBytes").value(104857600))
                .andExpect(jsonPath("$.limitePorArquivoBytes").value(5242880));
    }

    @Test
    void uploadContaNaQuotaUmaUnicaVezParaConteudoReenviado() throws Exception {
        var foto = new org.springframework.mock.web.MockMultipartFile(
                "file", "portfolio.png", "image/png",
                java.util.Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aQU0AAAAASUVORK5CYII="));

        for (int tentativa = 0; tentativa < 2; tentativa++) {
            mvc.perform(multipart("/api/uploads").file(foto).header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tamanhoBytes").value(foto.getSize()))
                    .andExpect(jsonPath("$.usoBytes").value(foto.getSize()));
        }

        mvc.perform(get("/api/uploads/uso").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usoBytes").value(foto.getSize()));
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
    void profissionalSemEspecialidadePodeSerEncontradoPeloTituloProfissional() throws Exception {
        mvc.perform(patch("/api/perfil")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tituloProfissional\":\"Instrutora de violão\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/profissionais").param("busca", "Instrutora de violão"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].tituloProfissional").value("Instrutora de violão"));
    }

    @Test
    void tituloProfissionalEOpcionalTemLimiteERejeitaSomenteEspacos() throws Exception {
        mvc.perform(patch("/api/perfil")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tituloProfissional\":\"%s\"}".formatted("x".repeat(121))))
                .andExpect(status().isBadRequest());

        mvc.perform(patch("/api/perfil")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tituloProfissional\":\"   \"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(patch("/api/perfil")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tituloProfissional\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tituloProfissional").value(""));
    }

    @Test
    void filtroDeNotaMinimaIgnoraPerfisSemAmostraSuficienteEValidaLimites() throws Exception {
        mvc.perform(patch("/api/perfil")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"especialidades\":\"Eletricista\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/profissionais").param("notaMinima", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(0));
        mvc.perform(get("/api/profissionais").param("notaMinima", "5.5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criaEListaPublicacaoDePortfolioComFotosPersistidasEProtecaoDoAutor() throws Exception {
        var foto = new org.springframework.mock.web.MockMultipartFile(
                "file", "portfolio.png", "image/png",
                java.util.Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aQU0AAAAASUVORK5CYII="));
        var upload = mvc.perform(multipart("/api/uploads").file(foto)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tamanhoBytes").isNumber())
                .andExpect(jsonPath("$.usoBytes").isNumber())
                .andExpect(jsonPath("$.limiteBytes").value(104857600))
                .andReturn();
        String fotoUrl = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(upload.getResponse().getContentAsString()).get("url").asText();

        mvc.perform(get(fotoUrl)).andExpect(status().isNotFound());
        mvc.perform(get(fotoUrl).header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        var publicacao = mvc.perform(post("/api/publicacoes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"legenda":"Instalação concluída", "fotos":["%s"]}
                                """.formatted(fotoUrl)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.legenda").value("Instalação concluída"))
                .andExpect(jsonPath("$.fotos[0]").value(fotoUrl))
                .andExpect(jsonPath("$.profissionalId").value(usuarioId))
                .andReturn();
        long publicacaoId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(publicacao.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/perfis/{id}/publicacoes", usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].fotos[0]").value(fotoUrl))
                .andReturn();
        mvc.perform(get("/api/publicacoes/{id}", publicacaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(publicacaoId))
                .andExpect(jsonPath("$.profissionalNome").value("Ana Profissional"))
                .andExpect(jsonPath("$.fotos[0]").value(fotoUrl))
                .andExpect(jsonPath("$.email").doesNotExist());

        mvc.perform(patch("/api/publicacoes/{id}", publicacaoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"legenda\":\"Instalação revisada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legenda").value("Instalação revisada"))
                .andExpect(jsonPath("$.fotos[0]").value(fotoUrl));

        var outraConta = usuarios.register(new RegisterDTO("Outro usuário", "outro@skillswap.invalid", "senha-teste-123"));
        String tokenOutro = jwt.generateToken(outraConta.getEmail());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/publicacoes/{id}", publicacaoId)
                        .header("Authorization", "Bearer " + tokenOutro))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/publicacoes/{id}", publicacaoId)
                        .header("Authorization", "Bearer " + tokenOutro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"legenda\":\"Alteração indevida\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get(fotoUrl)).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Cache-Control", "no-store"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/publicacoes/{id}", publicacaoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get(fotoUrl)).andExpect(status().isNotFound());
    }

    @Test
    void publicacaoRejeitaFotoExternaOuNaoEnviada() throws Exception {
        mvc.perform(post("/api/publicacoes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"legenda\":\"teste\",\"fotos\":[\"https://exemplo.invalid/foto.png\"]}"))
                .andExpect(status().isBadRequest());
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
        mvc.perform(get("/api/uploads/uso").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usoBytes").isNumber());
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
        mvc.perform(get(fotoUrl)).andExpect(status().isNotFound());
        mvc.perform(get(fotoUrl).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }
}
