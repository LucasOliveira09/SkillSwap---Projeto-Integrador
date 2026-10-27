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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "skillswap.seed.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NegociacaoControllerTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarios;
    @Autowired JwtService jwt;

    private Long anuncianteId;
    private String anuncianteToken;
    private String proponenteToken;
    private Long anuncioId;

    @BeforeEach
    void prepararParticipantesEOferta() throws Exception {
        var anunciante = usuarios.register(new RegisterDTO("Ana Profissional", "ana-negociacao@skillswap.invalid", "senha-teste-123"));
        var proponente = usuarios.register(new RegisterDTO("Bruno Cliente", "bruno-negociacao@skillswap.invalid", "senha-teste-123"));
        anuncianteId = anunciante.getId();
        anuncianteToken = jwt.generateToken(anunciante.getEmail());
        proponenteToken = jwt.generateToken(proponente.getEmail());

        var resposta = mvc.perform(post("/api/anuncios")
                        .header("Authorization", "Bearer " + anuncianteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titulo":"Manutenção elétrica","descricao":"Atendimento residencial",
                                 "categoria":"MANUTENCAO","cidade":"Florianópolis","modalidade":"VENDA",
                                 "preco":120,"habilidadeDesejada":""}
                                """))
                .andExpect(status().isCreated()).andReturn();
        anuncioId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(resposta.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void conduzPropostaAcordoConversaConclusaoEAvaliacaoComRecursosRest() throws Exception {
        var respostaProposta = mvc.perform(post("/api/anuncios/{id}/propostas", anuncioId)
                        .header("Authorization", "Bearer " + proponenteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mensagemInicial\":\"Tenho interesse neste serviço\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andReturn();
        long propostaId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(respostaProposta.getResponse().getContentAsString()).get("id").asLong();
        org.junit.jupiter.api.Assertions.assertEquals("/api/propostas/" + propostaId,
                respostaProposta.getResponse().getHeader("Location"));

        mvc.perform(get("/api/propostas").param("direcao", "ENVIADAS")
                        .header("Authorization", "Bearer " + proponenteToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1));
        mvc.perform(get("/api/propostas").param("direcao", "RECEBIDAS")
                        .header("Authorization", "Bearer " + anuncianteToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].id").value(propostaId));

        mvc.perform(patch("/api/propostas/{id}", propostaId)
                        .header("Authorization", "Bearer " + anuncianteToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACEITA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACEITA"));
        var paginaAcordos = mvc.perform(get("/api/acordos").header("Authorization", "Bearer " + proponenteToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1)).andReturn();
        long acordoId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(paginaAcordos.getResponse().getContentAsString()).get("itens").get(0).get("id").asLong();

        var respostaMensagem = mvc.perform(post("/api/acordos/{id}/mensagens", acordoId)
                        .header("Authorization", "Bearer " + proponenteToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"conteudo\":\"Podemos agendar para sexta?\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.remetenteNome").value("Bruno Cliente"))
                .andReturn();
        long mensagemId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(respostaMensagem.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/acordos/{acordoId}/mensagens/{mensagemId}", acordoId, mensagemId)
                        .header("Authorization", "Bearer " + proponenteToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(mensagemId));
        mvc.perform(get("/api/acordos/{id}/mensagens", acordoId)
                        .param("pagina", "0").param("tamanho", "10")
                        .header("Authorization", "Bearer " + anuncianteToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].conteudo").value("Podemos agendar para sexta?"));

        mvc.perform(patch("/api/acordos/{id}", acordoId)
                        .header("Authorization", "Bearer " + proponenteToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONCLUIDO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONCLUIDO"));
        var respostaAvaliacao = mvc.perform(post("/api/acordos/{id}/avaliacoes", acordoId)
                        .header("Authorization", "Bearer " + proponenteToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nota\":5,\"notaAnuncio\":4,\"depoimento\":\"Serviço cuidadoso e pontual\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.avaliadoId").value(anuncianteId))
                .andExpect(jsonPath("$.nota").value(5)).andExpect(jsonPath("$.notaAnuncio").value(4)).andReturn();
        long avaliacaoId = tools.jackson.databind.json.JsonMapper.builder().build()
                .readTree(respostaAvaliacao.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/acordos/{id}/avaliacoes", acordoId)
                        .header("Authorization", "Bearer " + anuncianteToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].depoimento").value("Serviço cuidadoso e pontual"));
        mvc.perform(get("/api/acordos/{acordoId}/avaliacoes/{avaliacaoId}", acordoId, avaliacaoId)
                        .header("Authorization", "Bearer " + anuncianteToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(avaliacaoId));
        mvc.perform(get("/api/anuncios/{id}", anuncioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaAvaliacoesAnuncio").value(4.0))
                .andExpect(jsonPath("$.quantidadeAvaliacoesAnuncio").value(1));
        mvc.perform(get("/api/anuncios").param("pagina", "0").param("tamanho", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].mediaAvaliacoesAnuncio").value(4.0))
                .andExpect(jsonPath("$.itens[0].quantidadeAvaliacoesAnuncio").value(1));
    }

    @Test
    void recursosDaNegociacaoExigemParticipacaoNoAcordo() throws Exception {
        var cliente = usuarios.register(new RegisterDTO("Cliente externo", "externo-negociacao@skillswap.invalid", "senha-teste-123"));
        String externoToken = jwt.generateToken(cliente.getEmail());
        mvc.perform(get("/api/acordos/9999").header("Authorization", "Bearer " + externoToken))
                .andExpect(status().isNotFound());
    }
}
