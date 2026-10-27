package com.skillswap.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="skillswap.seed.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class IntegracaoAnuncioTest {
    @Autowired MockMvc mvc;
    @Autowired com.skillswap.api.domain.usuario.UsuarioService usuarios;
    @Autowired com.skillswap.api.infra.security.JwtService jwt;
    String token;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired com.skillswap.api.domain.anuncio.IndiceAnunciosInicial indice;
    @Autowired jakarta.persistence.EntityManager entidades;
    @org.junit.jupiter.api.BeforeEach void autenticar() {
        usuarios.register(new com.skillswap.api.domain.usuario.dto.RegisterDTO("Teste", "teste@skillswap.invalid", "senha-teste-123"));
        token = jwt.generateToken("teste@skillswap.invalid");
    }


    private String venda(String titulo, String preco) {
        return """
            {"titulo":"%s","descricao":"Música para iniciantes","categoria":"AULAS",
             "cidade":"Florianópolis","modalidade":"VENDA","preco":%s,"habilidadeDesejada":""}
            """.formatted(titulo, preco);
    }

    @Test void persisteVendaELocalizaSemAcentos() throws Exception {
        mvc.perform(post("/api/anuncios").header("Authorization", "Bearer " + token).contentType("application/json").content(venda("  Aulas de violão  ", "90.50")))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").isNumber()).andExpect(jsonPath("$.titulo").value("Aulas de violão"))
            .andExpect(jsonPath("$.exemplo").value(false)).andExpect(jsonPath("$.estrelas").isEmpty());
        mvc.perform(get("/api/anuncios").param("busca", "VIOLAO").param("categoria", "AULAS"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1))
            .andExpect(jsonPath("$.itens[0].preco").value(90.5));
        mvc.perform(get("/api/anuncios").param("categoria", "MANUTENCAO"))
            .andExpect(jsonPath("$.totalItens").value(0));
    }

    @Test void gravaTrocaSemPreco() throws Exception {
        mvc.perform(post("/api/anuncios").header("Authorization", "Bearer " + token).contentType("application/json").content("""
            {"titulo":"Aula","descricao":"Aula prática","categoria":"AULAS","cidade":"Ourinhos",
             "modalidade":"TROCA","preco":90,"habilidadeDesejada":"  Pintura  "}
            """))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.preco").isEmpty())
            .andExpect(jsonPath("$.habilidadeDesejada").value("Pintura"));
    }

    @Test void rejeitaEspacosCategoriaInvalidaEPrecoInvalido() throws Exception {
        for (String corpo : new String[]{venda("   ", "90"), venda("Aula", "0"), venda("Aula", "1.001"),
                venda("Aula", "90").replace("AULAS", "inexistente"), venda("Aula", "100000000")}) {
            mvc.perform(post("/api/anuncios").header("Authorization", "Bearer " + token).contentType("application/json").content(corpo))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
        }
        mvc.perform(get("/api/anuncios")).andExpect(jsonPath("$.totalItens").value(0));
    }

    @Test void filtraOfertasPorModalidadeEFaixaDePreco() throws Exception {
        mvc.perform(post("/api/anuncios").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(venda("Aula de violão", "90")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/anuncios").header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {"titulo":"Troca por aula de inglês","descricao":"Aulas particulares",
                                 "categoria":"AULAS","cidade":"Florianópolis","modalidade":"TROCA",
                                 "preco":null,"habilidadeDesejada":"Conversação em inglês"}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/anuncios").param("modalidade", "VENDA")
                        .param("cidade", "Florianópolis")
                        .param("precoMinimo", "80").param("precoMaximo", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].titulo").value("Aula de violão"));
        mvc.perform(get("/api/anuncios").param("cidade", "Ourinhos"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(0));
        mvc.perform(get("/api/anuncios").param("modalidade", "TROCA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].preco").isEmpty());
        mvc.perform(get("/api/anuncios").param("precoMinimo", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(0));
        mvc.perform(get("/api/anuncios").param("precoMinimo", "100").param("precoMaximo", "50"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/anuncios").param("modalidade", "INVALIDA"))
                .andExpect(status().isBadRequest());
    }

    @Test void persisteEFiltraDisponibilidadeSemanalDeclaradaNaOferta() throws Exception {
        mvc.perform(post("/api/anuncios").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("""
                                {"titulo":"Aula de violão","descricao":"Aulas presenciais",
                                 "categoria":"AULAS","cidade":"Florianópolis","modalidade":"VENDA",
                                 "preco":90,"habilidadeDesejada":"",
                                 "disponibilidades":[
                                   {"diaSemana":"SEGUNDA","periodo":"MANHA"},
                                   {"diaSemana":"QUARTA","periodo":"TARDE"}]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.disponibilidades.length()").value(2))
                .andExpect(jsonPath("$.disponibilidades[0].diaSemana").value("SEGUNDA"));

        mvc.perform(post("/api/anuncios").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(venda("Aula sem horário informado", "90")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.disponibilidades.length()").value(0));

        mvc.perform(get("/api/anuncios").param("diaSemana", "SEGUNDA").param("periodo", "MANHA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].titulo").value("Aula de violão"));
        mvc.perform(get("/api/anuncios").param("diaSemana", "SEGUNDA").param("periodo", "TARDE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(0));
        mvc.perform(get("/api/anuncios").param("diaSemana", "INVALIDO"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/anuncios").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("""
                                {"titulo":"Aula com período repetido","descricao":"Aulas presenciais",
                                 "categoria":"AULAS","cidade":"Florianópolis","modalidade":"VENDA",
                                 "preco":90,"habilidadeDesejada":"",
                                 "disponibilidades":[
                                   {"diaSemana":"SEGUNDA","periodo":"MANHA"},
                                   {"diaSemana":"SEGUNDA","periodo":"MANHA"}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test void proprietarioListaEditaEReativaSomenteSeusAnuncios() throws Exception {
        var criado = mvc.perform(post("/api/anuncios").header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(venda("Aula de violão", "90")))
                .andExpect(status().isCreated()).andReturn();
        long id = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(criado.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(delete("/api/anuncios/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/anuncios/meus").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].status").value("INATIVO"));
        mvc.perform(get("/api/anuncios/meus/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(put("/api/anuncios/{id}", id).header("Authorization", "Bearer " + token)
                        .contentType("application/json").content(venda("Aula de piano", "90")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INATIVO"))
                .andExpect(jsonPath("$.titulo").value("Aula de piano"));
        mvc.perform(patch("/api/anuncios/{id}", id).header("Authorization", "Bearer " + token)
                        .contentType("application/json").content("{\"status\":\"ATIVO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ATIVO"));
        mvc.perform(get("/api/anuncios/{id}", id)).andExpect(status().isOk());

        usuarios.register(new com.skillswap.api.domain.usuario.dto.RegisterDTO(
                "Outra pessoa", "outra@skillswap.invalid", "senha-teste-123"));
        String outroToken = jwt.generateToken("outra@skillswap.invalid");
        mvc.perform(patch("/api/anuncios/{id}", id).header("Authorization", "Bearer " + outroToken)
                        .contentType("application/json").content("{\"status\":\"INATIVO\"}"))
                .andExpect(status().isForbidden());
    }

    @Test void rejeitaTrocaSemHabilidadeECamposGerenciadosPeloServidor() throws Exception {
        mvc.perform(post("/api/anuncios").header("Authorization", "Bearer " + token).contentType("application/json")
                .content(venda("Aula", "90").replace("VENDA", "TROCA")))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/anuncios").header("Authorization", "Bearer " + token).contentType("application/json")
                .content(venda("Aula", "90").replace("\"titulo\"", "\"exemplo\":true,\"titulo\"")))
            .andExpect(status().isBadRequest());
    }

    @Test void paginaComOrdemEstavelETrataBuscaLiteral() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/anuncios").header("Authorization", "Bearer " + token).contentType("application/json").content(venda("Aula " + i, "10")))
                .andExpect(status().isCreated());
        }
        mvc.perform(get("/api/anuncios").param("tamanho", "2").param("pagina", "1"))
            .andExpect(jsonPath("$.totalItens").value(3)).andExpect(jsonPath("$.totalPaginas").value(2))
            .andExpect(jsonPath("$.itens.length()").value(1)).andExpect(jsonPath("$.itens[0].titulo").value("Aula 0"));
        mvc.perform(get("/api/anuncios").param("busca", "%"))
            .andExpect(jsonPath("$.totalItens").value(0));
    }

    @Test void validaParametrosENaoExpoeFalhasInternas() throws Exception {
        for (String query : new String[]{"pagina=-1", "tamanho=51", "pagina=abc", "busca=" + "a".repeat(101)}) {
            mvc.perform(get("/api/anuncios?" + query)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
        }
        mvc.perform(get("/api/anuncios/999999")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test void preservaStatusDeRotaEMetodoInexistentes() throws Exception {
        mvc.perform(get("/api/inexistente").header("Authorization", "Bearer " + token)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/categorias").header("Authorization", "Bearer " + token)).andExpect(status().isMethodNotAllowed());
    }

    @Test void protegeCadastroTokenInvalidoEPropriedade() throws Exception {
        mvc.perform(post("/api/anuncios").contentType("application/json").content(venda("Aula", "10")))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/anuncios").header("Authorization", "Bearer invalido"))
            .andExpect(status().isUnauthorized());
        var resposta = mvc.perform(post("/api/anuncios").header("Authorization", "Bearer " + token)
            .contentType("application/json").content(venda("Aula própria", "10"))).andExpect(status().isCreated()).andReturn();
        long id = tools.jackson.databind.json.JsonMapper.builder().build()
            .readTree(resposta.getResponse().getContentAsString()).get("id").asLong();
        usuarios.register(new com.skillswap.api.domain.usuario.dto.RegisterDTO("Outro", "outro@skillswap.invalid", "senha-teste-123"));
        mvc.perform(delete("/api/anuncios/"+id).header("Authorization", "Bearer "+jwt.generateToken("outro@skillswap.invalid")))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/anuncios/"+id).header("Authorization", "Bearer "+token))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/anuncios/"+id)).andExpect(status().isNotFound());
    }

    @Test void autenticaComRotasDeRecursosERejeitaSenhaErrada() throws Exception {
        mvc.perform(post("/api/sessoes").contentType("application/json").content("""
            {"email":"teste@skillswap.invalid","senha":"senha-errada"}
            """)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/usuarios").contentType("application/json").content("""
            {"nome":"Nova conta","email":"nova@skillswap.invalid","senha":"senha-valida-123"}
            """)).andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.tipo").value("Bearer"));
    }

    @Test @org.junit.jupiter.api.Timeout(30)
    void preservaAnuncioAnteriorAoPreencherIndiceNovo() throws Exception {
        mvc.perform(post("/api/anuncios").header("Authorization", "Bearer " + token)
            .contentType("application/json").content(venda("Violão anterior", "90"))).andExpect(status().isCreated());
        jdbc.update("UPDATE anuncios SET texto_busca = NULL");
        entidades.clear();
        indice.run();
        mvc.perform(get("/api/anuncios").param("busca","VIOLAO"))
            .andExpect(jsonPath("$.totalItens").value(1))
            .andExpect(jsonPath("$.itens[0].titulo").value("Violão anterior"));
    }

    @Test
    void realizaUploadDeImagemEPersisteNoAnuncio() throws Exception {
        org.springframework.mock.web.MockMultipartFile arquivo = new org.springframework.mock.web.MockMultipartFile(
                "file", "foto.png", "image/png",
                java.util.Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aQU0AAAAASUVORK5CYII=")
        );

        var resultadoUpload = mvc.perform(multipart("/api/uploads")
                .file(arquivo)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").isNotEmpty())
                .andReturn();

        String url = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(resultadoUpload.getResponse().getContentAsString()).get("url").asText();

        mvc.perform(get(url))
                .andExpect(status().isOk());

        String payloadComImagem = """
            {"titulo":"Fotografia Profissional","descricao":"Ensaio externo com fotos editadas",
             "categoria":"AULAS","cidade":"São Paulo","modalidade":"VENDA","preco":250.00,
             "habilidadeDesejada":"","imagem":"%s"}
            """.formatted(url);

        var resultadoAnuncio = mvc.perform(post("/api/anuncios")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content(payloadComImagem))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imagem").value(url))
                .andReturn();

        long id = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(resultadoAnuncio.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/anuncios/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagem").value(url))
                .andExpect(jsonPath("$.titulo").value("Fotografia Profissional"));
    }

    @Test
    void rejeitaArquivoQueNaoContemImagemMesmoComMimePermitido() throws Exception {
        var arquivo = new org.springframework.mock.web.MockMultipartFile(
                "file", "foto.png", "image/png", new byte[]{1, 2, 3, 4});

        mvc.perform(multipart("/api/uploads").file(arquivo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
    }
}

