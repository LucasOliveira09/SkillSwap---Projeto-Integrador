package com.skillswap.api;

import com.skillswap.api.domain.usuario.UsuarioService;
import com.skillswap.api.domain.avaliacao.AvaliacaoRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"skillswap.seed.enabled=false", "skillswap.moderacao.emails=mod@skillswap.invalid"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ModeracaoControllerTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarios;
    @Autowired AvaliacaoRepository avaliacoes;
    @Autowired JwtService jwt;

    private Long profissionalId;
    private Long publicacaoId;
    private String tokenAutor;
    private String tokenDenunciante;
    private String tokenModerador;

    @BeforeEach
    void prepararConteudoEContas() throws Exception {
        var profissional = usuarios.register(new RegisterDTO("Profissional", "autor@skillswap.invalid", "senha-teste-123"));
        var denunciante = usuarios.register(new RegisterDTO("Cliente", "cliente@skillswap.invalid", "senha-teste-123"));
        var moderador = usuarios.register(new RegisterDTO("Moderação", "mod@skillswap.invalid", "senha-teste-123"));
        profissionalId = profissional.getId();
        tokenAutor = jwt.generateToken(profissional.getEmail());
        tokenDenunciante = jwt.generateToken(denunciante.getEmail());
        tokenModerador = jwt.generateToken(moderador.getEmail());

        var foto = new org.springframework.mock.web.MockMultipartFile("file", "foto.png", "image/png",
                java.util.Base64.getDecoder().decode(
                        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aQU0AAAAASUVORK5CYII="));
        var upload = mvc.perform(multipart("/api/uploads").file(foto)
                        .header("Authorization", "Bearer " + tokenAutor))
                .andExpect(status().isCreated()).andReturn();
        String url = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(upload.getResponse().getContentAsString()).get("url").asText();
        var criada = mvc.perform(post("/api/publicacoes").header("Authorization", "Bearer " + tokenAutor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"legenda\":\"Serviço de pintura\",\"fotos\":[\"%s\"]}").formatted(url)))
                .andExpect(status().isCreated()).andReturn();
        publicacaoId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(criada.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void denunciaPodeSerOcultadaSomentePorModeradorSemApagarEvidencia() throws Exception {
        var denuncia = mvc.perform(post("/api/denuncias").header("Authorization", "Bearer " + tokenDenunciante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"tipoConteudo\":\"PUBLICACAO\",\"recursoId\":%d,\"motivo\":\"SPAM\",\"descricao\":\"Link repetido\"}").formatted(publicacaoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andReturn();
        long denunciaId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(denuncia.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/denuncias/{id}", denunciaId).header("Authorization", "Bearer " + tokenDenunciante))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ABERTA"));
        mvc.perform(get("/api/denuncias/{id}", denunciaId).header("Authorization", "Bearer " + tokenAutor))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/denuncias").header("Authorization", "Bearer " + tokenDenunciante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"tipoConteudo\":\"PUBLICACAO\",\"recursoId\":%d,\"motivo\":\"SPAM\"}").formatted(publicacaoId)))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/moderacao/denuncias").header("Authorization", "Bearer " + tokenDenunciante))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/moderacao/acesso").header("Authorization", "Bearer " + tokenModerador))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("true"));
        mvc.perform(get("/api/moderacao/denuncias").header("Authorization", "Bearer " + tokenModerador))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].trechoConteudo").value("Serviço de pintura"));

        mvc.perform(patch("/api/moderacao/denuncias/{id}", denunciaId)
                        .header("Authorization", "Bearer " + tokenModerador)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"OCULTAR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OCULTADA"));
        mvc.perform(get("/api/publicacoes/{id}", publicacaoId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/perfis/{id}/publicacoes", profissionalId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(0));
        mvc.perform(get("/api/moderacao/denuncias").header("Authorization", "Bearer " + tokenModerador)
                        .param("status", "OCULTADA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].status").value("OCULTADA"));
        mvc.perform(patch("/api/moderacao/denuncias/{id}", denunciaId).header("Authorization", "Bearer " + tokenModerador)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"REEXIBIR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REEXIBIDA"))
                .andExpect(jsonPath("$.analisadaPor").value("mod@skillswap.invalid"));
        mvc.perform(get("/api/publicacoes/{id}", publicacaoId)).andExpect(status().isOk());
    }

    @Test
    void autorNaoPodeDenunciarPublicacaoPropria() throws Exception {
        mvc.perform(post("/api/denuncias").header("Authorization", "Bearer " + tokenAutor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"tipoConteudo\":\"PUBLICACAO\",\"recursoId\":%d,\"motivo\":\"OUTRO\"}").formatted(publicacaoId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void denunciaNaoExpõePublicacaoDePerfilPausado() throws Exception {
        mvc.perform(patch("/api/perfil").header("Authorization", "Bearer " + tokenAutor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"perfilPublico\":false}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/denuncias").header("Authorization", "Bearer " + tokenDenunciante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"tipoConteudo\":\"PUBLICACAO\",\"recursoId\":%d,\"motivo\":\"OUTRO\"}").formatted(publicacaoId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void denunciaNaoExpõeAvaliacaoDePerfilPausado() throws Exception {
        long avaliacaoId = criarAvaliacao();
        mvc.perform(patch("/api/perfil").header("Authorization", "Bearer " + tokenAutor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"perfilPublico\":false}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/denuncias").header("Authorization", "Bearer " + tokenModerador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"tipoConteudo\":\"AVALIACAO\",\"recursoId\":%d,\"motivo\":\"OUTRO\"}").formatted(avaliacaoId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void avaliacaoOcultadaSaiDasMediasPublicasMasPodeSerReexibida() throws Exception {
        var respostaAnuncio = mvc.perform(post("/api/anuncios").header("Authorization", "Bearer " + tokenAutor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Pintura residencial\",\"descricao\":\"Pintura interna\",\"categoria\":\"MANUTENCAO\",\"cidade\":\"Florianópolis\",\"modalidade\":\"VENDA\",\"preco\":200,\"habilidadeDesejada\":\"\"}"))
                .andExpect(status().isCreated()).andReturn();
        long anuncioId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(respostaAnuncio.getResponse().getContentAsString()).get("id").asLong();
        var respostaProposta = mvc.perform(post("/api/anuncios/{id}/propostas", anuncioId)
                        .header("Authorization", "Bearer " + tokenDenunciante)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mensagemInicial\":\"Tenho interesse\"}"))
                .andExpect(status().isCreated()).andReturn();
        long propostaId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(respostaProposta.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/propostas/{id}", propostaId).header("Authorization", "Bearer " + tokenAutor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACEITA\"}"))
                .andExpect(status().isOk());
        var acordos = mvc.perform(get("/api/acordos").header("Authorization", "Bearer " + tokenDenunciante))
                .andExpect(status().isOk()).andReturn();
        var itensAcordos = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(acordos.getResponse().getContentAsString()).get("itens");
        long acordoId = 0;
        for (var item : itensAcordos) {
            if (item.get("anuncioId").asLong() == anuncioId) {
                acordoId = item.get("id").asLong();
                break;
            }
        }
        if (acordoId == 0) throw new AssertionError("Acordo da proposta não encontrado");
        mvc.perform(patch("/api/acordos/{id}", acordoId).header("Authorization", "Bearer " + tokenDenunciante)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONCLUIDO\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/acordos/{id}", acordoId).header("Authorization", "Bearer " + tokenAutor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONCLUIDO\"}"))
                .andExpect(status().isOk());
        var avaliacao = mvc.perform(post("/api/acordos/{id}/avaliacoes", acordoId)
                        .header("Authorization", "Bearer " + tokenDenunciante).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":5,\"notaAnuncio\":5,\"depoimento\":\"Trabalho excelente\"}"))
                .andExpect(status().isCreated()).andReturn();
        long avaliacaoId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(avaliacao.getResponse().getContentAsString()).get("id").asLong();

        var denuncia = mvc.perform(post("/api/denuncias").header("Authorization", "Bearer " + tokenModerador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"tipoConteudo\":\"AVALIACAO\",\"recursoId\":%d,\"motivo\":\"FRAUDE\"}").formatted(avaliacaoId)))
                .andExpect(status().isCreated()).andReturn();
        long denunciaId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(denuncia.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/moderacao/denuncias/{id}", denunciaId).header("Authorization", "Bearer " + tokenModerador)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"OCULTAR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.trechoConteudo").value("Trabalho excelente"));
        mvc.perform(get("/api/usuarios/{id}/avaliacoes", profissionalId))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().json("[]"));
        mvc.perform(get("/api/perfis/{id}", profissionalId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaNotas").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.quantidadeAvaliacoes").value(0));
        mvc.perform(get("/api/anuncios/{id}", anuncioId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeAvaliacoesAnuncio").value(0));

        mvc.perform(patch("/api/moderacao/denuncias/{id}", denunciaId).header("Authorization", "Bearer " + tokenModerador)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"acao\":\"REEXIBIR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REEXIBIDA"));
        mvc.perform(get("/api/usuarios/{id}/avaliacoes", profissionalId))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(avaliacaoId));
        mvc.perform(get("/api/perfis/{id}", profissionalId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaNotas").value(5)).andExpect(jsonPath("$.quantidadeAvaliacoes").value(1));
    }

    @Test
    void profissionalContestaAvaliacaoEModeradorDecideSemExporJustificativaPublicamente() throws Exception {
        long avaliacaoId = criarAvaliacao();
        String justificativa = "O texto relata uma etapa que não fazia parte do serviço combinado.";

        mvc.perform(get("/api/avaliacoes/{id}/contestacoes", avaliacaoId)
                        .header("Authorization", "Bearer " + tokenAutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        var criada = mvc.perform(post("/api/avaliacoes/{id}/contestacoes", avaliacaoId)
                        .header("Authorization", "Bearer " + tokenAutor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"justificativa\":\"%s\"}").formatted(justificativa)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Location", org.hamcrest.Matchers.matchesPattern("/api/avaliacoes/\\d+/contestacoes/\\d+")))
                .andReturn();
        long contestacaoCriadaId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(criada.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/avaliacoes/{avaliacaoId}/contestacoes/{contestacaoId}", avaliacaoId, contestacaoCriadaId)
                        .header("Authorization", "Bearer " + tokenAutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(contestacaoCriadaId));

        mvc.perform(get("/api/avaliacoes/{id}/contestacoes", avaliacaoId)
                        .header("Authorization", "Bearer " + tokenDenunciante))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/moderacao/contestacoes").header("Authorization", "Bearer " + tokenDenunciante))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/moderacao/contestacoes").header("Authorization", "Bearer " + tokenModerador))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].justificativa").value(justificativa))
                .andExpect(jsonPath("$.itens[0].trechoAvaliacao").value("Trabalho excelente"));
        mvc.perform(get("/api/usuarios/{id}/avaliacoes", profissionalId))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].depoimento").value("Trabalho excelente"))
                .andExpect(jsonPath("$[0].justificativa").doesNotExist());
        mvc.perform(post("/api/avaliacoes/{id}/contestacoes", avaliacaoId)
                        .header("Authorization", "Bearer " + tokenDenunciante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificativa\":\"O profissional foi mal avaliado.\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/avaliacoes/{id}/contestacoes", avaliacaoId)
                        .header("Authorization", "Bearer " + tokenAutor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificativa\":\"Tentativa duplicada desta mesma contestação.\"}"))
                .andExpect(status().isConflict());

        long contestacaoId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(mvc.perform(get("/api/moderacao/contestacoes")
                                .header("Authorization", "Bearer " + tokenModerador))
                        .andReturn().getResponse().getContentAsString()).get("itens").get(0).get("id").asLong();
        mvc.perform(patch("/api/moderacao/contestacoes/{id}", contestacaoId)
                        .header("Authorization", "Bearer " + tokenModerador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\":\"OCULTAR\",\"decisao\":\"A avaliação descreve cobrança não acordada e foi ocultada.\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OCULTADA"));

        mvc.perform(get("/api/usuarios/{id}/avaliacoes", profissionalId))
                .andExpect(status().isOk()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().json("[]"));
        mvc.perform(get("/api/avaliacoes/{id}/contestacoes", avaliacaoId)
                        .header("Authorization", "Bearer " + tokenAutor))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("OCULTADA"))
                .andExpect(jsonPath("$[0].decisao").exists());
    }

    @Test
    void contestacaoSoPodeSerAbertaNoPrazoEManterPreservaAvaliacaoPublica() throws Exception {
        long avaliacaoId = criarAvaliacao();
        mvc.perform(post("/api/avaliacoes/{id}/contestacoes", avaliacaoId)
                        .header("Authorization", "Bearer " + tokenAutor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificativa\":\"A nota menciona prazo que não foi combinado.\"}"))
                .andExpect(status().isCreated());
        var pagina = mvc.perform(get("/api/moderacao/contestacoes")
                        .header("Authorization", "Bearer " + tokenModerador))
                .andExpect(status().isOk()).andReturn();
        long contestacaoId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(pagina.getResponse().getContentAsString()).get("itens").get(0).get("id").asLong();
        mvc.perform(patch("/api/moderacao/contestacoes/{id}", contestacaoId)
                        .header("Authorization", "Bearer " + tokenModerador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\":\"MANTER\",\"decisao\":\"A avaliação corresponde ao relato da negociação.\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("MANTIDA"));
        mvc.perform(get("/api/usuarios/{id}/avaliacoes", profissionalId))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(avaliacaoId));
        mvc.perform(patch("/api/moderacao/contestacoes/{id}", contestacaoId)
                        .header("Authorization", "Bearer " + tokenModerador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acao\":\"OCULTAR\",\"decisao\":\"A avaliação descreve cobrança não acordada e foi ocultada.\"}"))
                .andExpect(status().isConflict());

        long avaliacaoExpiradaId = criarAvaliacao();
        var avaliacaoExpirada = avaliacoes.findById(avaliacaoExpiradaId).orElseThrow();
        avaliacaoExpirada.setCriadoEm(java.time.LocalDateTime.now().minusDays(8));
        avaliacoes.save(avaliacaoExpirada);
        mvc.perform(post("/api/avaliacoes/{id}/contestacoes", avaliacaoExpiradaId)
                        .header("Authorization", "Bearer " + tokenAutor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justificativa\":\"A nota menciona prazo que não foi combinado.\"}"))
                .andExpect(status().isConflict());
    }

    private long criarAvaliacao() throws Exception {
        var respostaAnuncio = mvc.perform(post("/api/anuncios").header("Authorization", "Bearer " + tokenAutor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Pintura residencial\",\"descricao\":\"Pintura interna\",\"categoria\":\"MANUTENCAO\",\"cidade\":\"Florianópolis\",\"modalidade\":\"VENDA\",\"preco\":200,\"habilidadeDesejada\":\"\"}"))
                .andExpect(status().isCreated()).andReturn();
        long anuncioId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(respostaAnuncio.getResponse().getContentAsString()).get("id").asLong();
        var respostaProposta = mvc.perform(post("/api/anuncios/{id}/propostas", anuncioId)
                        .header("Authorization", "Bearer " + tokenDenunciante)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mensagemInicial\":\"Tenho interesse\"}"))
                .andExpect(status().isCreated()).andReturn();
        long propostaId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(respostaProposta.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(patch("/api/propostas/{id}", propostaId).header("Authorization", "Bearer " + tokenAutor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACEITA\"}"))
                .andExpect(status().isOk());
        var acordos = mvc.perform(get("/api/acordos").header("Authorization", "Bearer " + tokenDenunciante))
                .andExpect(status().isOk()).andReturn();
        long acordoId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(acordos.getResponse().getContentAsString()).get("itens").get(0).get("id").asLong();
        mvc.perform(patch("/api/acordos/{id}", acordoId).header("Authorization", "Bearer " + tokenDenunciante)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONCLUIDO\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/acordos/{id}", acordoId).header("Authorization", "Bearer " + tokenAutor)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONCLUIDO\"}"))
                .andExpect(status().isOk());
        var avaliacao = mvc.perform(post("/api/acordos/{id}/avaliacoes", acordoId)
                        .header("Authorization", "Bearer " + tokenDenunciante).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":5,\"notaAnuncio\":5,\"depoimento\":\"Trabalho excelente\"}"))
                .andExpect(status().isCreated()).andReturn();
        return tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(avaliacao.getResponse().getContentAsString()).get("id").asLong();
    }
}
