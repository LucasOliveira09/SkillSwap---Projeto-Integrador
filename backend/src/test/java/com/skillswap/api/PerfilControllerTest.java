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
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "skillswap.seed.enabled=false",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PerfilControllerTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarios;
    @Autowired JwtService jwt;
    @Autowired EntityManagerFactory entityManagerFactory;

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
    void rotasDeAnunciosPropriosExigemAutenticacao() throws Exception {
        mvc.perform(get("/api/anuncios/meus"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/anuncios/meus/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void buscaProfissionaisCarregaAReputacaoDaPaginaEmUmaAgregacao() throws Exception {
        var outraProfissional = usuarios.register(new RegisterDTO(
                "Bia Profissional", "bia-perfil@skillswap.invalid", "senha-teste-123"));
        String outroToken = jwt.generateToken(outraProfissional.getEmail());
        mvc.perform(patch("/api/perfil")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"especialidades\":\"Aulas de violão\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/perfil")
                        .header("Authorization", "Bearer " + outroToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"especialidades\":\"Aulas de piano\"}"))
                .andExpect(status().isOk());

        var estatisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        estatisticas.clear();

        mvc.perform(get("/api/profissionais").param("tamanho", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItens").value(2))
                .andExpect(jsonPath("$.itens[0].mediaNotas").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.itens[0].quantidadeAvaliacoes").value(0))
                .andExpect(jsonPath("$.itens[1].mediaNotas").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.itens[1].quantidadeAvaliacoes").value(0));

        org.junit.jupiter.api.Assertions.assertEquals(4, estatisticas.getPrepareStatementCount(),
                "a página de dois profissionais deve manter o número de consultas limitado");
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
    void pausarPerfilRetiraOfertasEPerfilDaVitrineSemApagarDadosDoDono() throws Exception {
        var oferta = mvc.perform(post("/api/anuncios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Aulas de violão","descricao":"Para iniciantes","categoria":"AULAS",
                                 "cidade":"Florianópolis","modalidade":"VENDA","preco":80,"habilidadeDesejada":""}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        Long anuncioId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(oferta.getResponse().getContentAsString()).get("id").asLong();
        var capa = new org.springframework.mock.web.MockMultipartFile(
                "file", "capa.png", "image/png",
                java.util.Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aQU0AAAAASUVORK5CYII="));
        var perfilComCapa = mvc.perform(multipart("/api/perfil/capa").file(capa).with(request -> {
                    request.setMethod("PUT");
                    return request;
                }).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        String capaUrl = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(perfilComCapa.getResponse().getContentAsString()).get("capaUrl").asText();
        var publicacao = mvc.perform(post("/api/publicacoes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"legenda":"Trabalho concluído","fotos":["%s"]}
                                """.formatted(capaUrl)))
                .andExpect(status().isCreated()).andReturn();
        Long publicacaoId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(publicacao.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(patch("/api/perfil")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"perfilPublico\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfilPublico").value(false));

        mvc.perform(get("/api/perfil").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.perfilPublico").value(false));
        mvc.perform(get("/api/perfis/{id}", usuarioId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/usuarios/{id}", usuarioId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/perfis/{id}/anuncios", usuarioId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/perfis/{id}/publicacoes", usuarioId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/usuarios/{id}/avaliacoes", usuarioId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/publicacoes/{id}", publicacaoId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/anuncios/{id}", anuncioId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/anuncios").param("busca", "Aulas"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(0));
        mvc.perform(get(capaUrl)).andExpect(status().isNotFound());
        mvc.perform(get(capaUrl).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/publicacoes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/publicacoes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1));
        mvc.perform(get("/api/anuncios/meus").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1));

        mvc.perform(patch("/api/perfil")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"perfilPublico\":true}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/perfis/{id}", usuarioId)).andExpect(status().isOk());
        mvc.perform(get("/api/publicacoes/{id}", publicacaoId)).andExpect(status().isOk());
        mvc.perform(get(capaUrl)).andExpect(status().isOk());
    }

    @Test
    void perfilPublicaLinksHttpsDoProfissionalERejeitaEsquemaInseguro() throws Exception {
        mvc.perform(patch("/api/perfil")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"linkedinUrl":"https://www.linkedin.com/in/ana-lima",
                                 "instagramUrl":"https://www.instagram.com/ana.profissional/",
                                 "siteUrl":"https://ana-profissional.com.br"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkedinUrl").value("https://www.linkedin.com/in/ana-lima"))
                .andExpect(jsonPath("$.instagramUrl").value("https://www.instagram.com/ana.profissional/"))
                .andExpect(jsonPath("$.siteUrl").value("https://ana-profissional.com.br"));

        mvc.perform(get("/api/perfis/{id}", usuarioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkedinUrl").value("https://www.linkedin.com/in/ana-lima"))
                .andExpect(jsonPath("$.instagramUrl").value("https://www.instagram.com/ana.profissional/"))
                .andExpect(jsonPath("$.siteUrl").value("https://ana-profissional.com.br"));

        mvc.perform(patch("/api/perfil")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"linkedinUrl\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest());
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

    @Test
    void atualizaERemoveCapaDoPerfilComAcessoPublicoControlado() throws Exception {
        var capa = new org.springframework.mock.web.MockMultipartFile(
                "file", "capa.png", "image/png",
                java.util.Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aQU0AAAAASUVORK5CYII="));

        var resultado = mvc.perform(multipart("/api/perfil/capa").file(capa).with(request -> {
                    request.setMethod("PUT");
                    return request;
                }).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capaUrl").isNotEmpty())
                .andReturn();
        String capaUrl = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(resultado.getResponse().getContentAsString()).get("capaUrl").asText();

        mvc.perform(get(capaUrl)).andExpect(status().isOk());
        mvc.perform(get("/api/perfil").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.capaUrl").value(capaUrl));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/perfil/capa")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get(capaUrl)).andExpect(status().isNotFound());
        mvc.perform(get(capaUrl).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }
}
