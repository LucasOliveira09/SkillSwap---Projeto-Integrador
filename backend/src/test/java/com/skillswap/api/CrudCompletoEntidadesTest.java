package com.skillswap.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Edição e exclusão das entidades de negociação (proposta, conversa, mensagem, avaliação, contestação,
 * denúncia e notificação), com as regras de domínio que limitam cada operação.
 */
// Banco H2 próprio: as contas de demonstração são gravadas na inicialização e não podem vazar para outros testes.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:skillswap-demo;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "skillswap.seed.enabled=false",
        "skillswap.demo.enabled=true",
        "skillswap.demo.admin-password=Admin@Teste2026",
        "skillswap.demo.user-password=Usuario@Teste2026"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CrudCompletoEntidadesTest {
    private static final RequestPostProcessor PROFISSIONAL = httpBasic("profissional@skillswap.dev", "Usuario@Teste2026");
    private static final RequestPostProcessor CLIENTE = httpBasic("cliente@skillswap.dev", "Usuario@Teste2026");
    private static final RequestPostProcessor ADMIN = httpBasic("admin@skillswap.dev", "Admin@Teste2026");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired MockMvc mvc;

    private long anuncioId;
    private long conversaId;

    @BeforeEach
    void anuncioEConversa() throws Exception {
        anuncioId = criar(post("/api/anuncios").with(PROFISSIONAL), """
                {"titulo":"Aulas de violão","descricao":"Aulas para iniciantes com repertório popular.",
                 "categoria":"AULAS","cidade":"Florianópolis","modalidade":"VENDA","preco":80.00}
                """).get("id").asLong();
        conversaId = criar(post("/api/anuncios/{id}/conversas", anuncioId).with(CLIENTE),
                "{\"conteudo\":\"Tem horário no sábado?\"}").get("id").asLong();
    }

    @Test
    void propostaPendenteEditadaEExcluidaPeloProponente() throws Exception {
        long propostaId = enviarProposta();

        mvc.perform(json(put("/api/propostas/{id}", propostaId).with(CLIENTE), termos("PARCELADO", "240.00", 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formaPagamento").value("PARCELADO"))
                .andExpect(jsonPath("$.parcelas").value(3));
        mvc.perform(json(put("/api/propostas/{id}", propostaId).with(PROFISSIONAL), termos("A_VISTA", "90.00", null)))
                .andExpect(status().isForbidden());
        mvc.perform(json(put("/api/propostas/{id}", propostaId).with(CLIENTE), termos("PARCELADO", "240.00", 20)))
                .andExpect(status().isBadRequest());
        mvc.perform(json(put("/api/propostas/{id}", propostaId).with(CLIENTE), termos("A_VISTA", "240.00", 3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Esta forma de cobrança não deve informar parcelas"));

        mvc.perform(delete("/api/propostas/{id}", propostaId).with(PROFISSIONAL)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/propostas/{id}", propostaId).with(CLIENTE)).andExpect(status().isNoContent());
        mvc.perform(get("/api/propostas/{id}", propostaId).with(CLIENTE)).andExpect(status().isNotFound());

        // Sem propostas, a conversa pode ser excluída por um participante.
        mvc.perform(delete("/api/conversas/{id}", conversaId).with(ADMIN)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/conversas/{id}", conversaId).with(CLIENTE)).andExpect(status().isNoContent());
        mvc.perform(get("/api/conversas/{id}", conversaId).with(CLIENTE)).andExpect(status().isNotFound());
    }

    @Test
    void propostaAceitaEConversaComPropostaNaoPodemSerExcluidas() throws Exception {
        long propostaId = enviarProposta();
        mvc.perform(patch("/api/propostas/{id}/aceitar", propostaId).with(PROFISSIONAL)).andExpect(status().isOk());

        mvc.perform(json(put("/api/propostas/{id}", propostaId).with(CLIENTE), termos("A_VISTA", "90.00", null)))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/propostas/{id}", propostaId).with(CLIENTE))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CONFLITO"));
        mvc.perform(delete("/api/conversas/{id}", conversaId).with(CLIENTE)).andExpect(status().isConflict());
    }

    @Test
    void mensagemDoChatEditadaEExcluidaSomentePeloAutor() throws Exception {
        long mensagemId = criar(post("/api/conversas/{id}/mensagens", conversaId).with(CLIENTE),
                "{\"conteudo\":\"Pode ser às 10h?\"}").get("id").asLong();

        mvc.perform(json(put("/api/conversas/{id}/mensagens/{m}", conversaId, mensagemId).with(CLIENTE),
                        "{\"conteudo\":\"Pode ser às 11h?\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conteudo").value("Pode ser às 11h?"));
        mvc.perform(json(put("/api/conversas/{id}/mensagens/{m}", conversaId, mensagemId).with(PROFISSIONAL),
                        "{\"conteudo\":\"Alterado\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(json(put("/api/conversas/{id}/mensagens/{m}", conversaId, mensagemId).with(CLIENTE),
                        "{\"conteudo\":\"\"}"))
                .andExpect(status().isBadRequest());

        enviarProposta();
        long mensagemDaProposta = 0;
        for (JsonNode item : ler(get("/api/conversas/{id}/mensagens", conversaId).with(CLIENTE)).get("itens"))
            if (!item.get("propostaId").isNull()) mensagemDaProposta = item.get("id").asLong();
        mvc.perform(delete("/api/conversas/{id}/mensagens/{m}", conversaId, mensagemDaProposta).with(CLIENTE))
                .andExpect(status().isConflict());

        mvc.perform(delete("/api/conversas/{id}/mensagens/{m}", conversaId, mensagemId).with(CLIENTE))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/conversas/{id}/mensagens/{m}", conversaId, mensagemId).with(CLIENTE))
                .andExpect(status().isNotFound());
    }

    @Test
    void avaliacaoExcluidaPeloClienteExcetoEnquantoContestada() throws Exception {
        long propostaId = enviarProposta();
        mvc.perform(patch("/api/propostas/{id}/aceitar", propostaId).with(PROFISSIONAL)).andExpect(status().isOk());
        long acordoId = ler(get("/api/acordos").with(CLIENTE)).get("itens").get(0).get("id").asLong();
        for (var parte : new RequestPostProcessor[]{CLIENTE, PROFISSIONAL})
            mvc.perform(json(patch("/api/acordos/{id}", acordoId).with(parte), "{\"status\":\"CONCLUIDO\"}"))
                    .andExpect(status().isOk());
        long avaliacaoId = criar(post("/api/acordos/{id}/avaliacoes", acordoId).with(CLIENTE),
                "{\"nota\":5,\"notaAnuncio\":5,\"depoimento\":\"Aula excelente\"}").get("id").asLong();

        mvc.perform(delete("/api/acordos/{a}/avaliacoes/{id}", acordoId, avaliacaoId).with(PROFISSIONAL))
                .andExpect(status().isForbidden());

        long contestacaoId = criar(post("/api/avaliacoes/{id}/contestacoes", avaliacaoId).with(PROFISSIONAL),
                "{\"justificativa\":\"A aula combinada não aconteceu nesse formato.\"}").get("id").asLong();
        mvc.perform(delete("/api/acordos/{a}/avaliacoes/{id}", acordoId, avaliacaoId).with(CLIENTE))
                .andExpect(status().isConflict());

        mvc.perform(json(put("/api/avaliacoes/{a}/contestacoes/{id}", avaliacaoId, contestacaoId).with(PROFISSIONAL),
                        "{\"justificativa\":\"Revisei: a avaliação cita uma aula de outro aluno.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.justificativa").value("Revisei: a avaliação cita uma aula de outro aluno."));
        mvc.perform(json(put("/api/avaliacoes/{a}/contestacoes/{id}", avaliacaoId, contestacaoId).with(PROFISSIONAL),
                        "{\"justificativa\":\"curta\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/avaliacoes/{a}/contestacoes/{id}", avaliacaoId, contestacaoId).with(CLIENTE))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/avaliacoes/{a}/contestacoes/{id}", avaliacaoId, contestacaoId).with(PROFISSIONAL))
                .andExpect(status().isNoContent());

        mvc.perform(delete("/api/acordos/{a}/avaliacoes/{id}", acordoId, avaliacaoId).with(CLIENTE))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/acordos/{a}/avaliacoes/{id}", acordoId, avaliacaoId).with(CLIENTE))
                .andExpect(status().isNotFound());
    }

    @Test
    void denunciaEditadaEExcluidaPeloAutorSomenteEnquantoAberta() throws Exception {
        long comentarioId = criar(post("/api/anuncios/{id}/comentarios", anuncioId).with(PROFISSIONAL),
                "{\"conteudo\":\"Chame no privado para desconto\"}").get("id").asLong();
        String denuncia = "{\"tipoConteudo\":\"COMENTARIO_ANUNCIO\",\"recursoId\":%d,\"motivo\":\"SPAM\"}"
                .formatted(comentarioId);
        long doCliente = criar(post("/api/denuncias").with(CLIENTE), denuncia).get("id").asLong();
        long doAdmin = criar(post("/api/denuncias").with(ADMIN), denuncia).get("id").asLong();

        mvc.perform(get("/api/denuncias").with(CLIENTE))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1));
        mvc.perform(json(put("/api/denuncias/{id}", doCliente).with(CLIENTE),
                        "{\"motivo\":\"FRAUDE\",\"descricao\":\"Tenta levar o pagamento para fora da plataforma\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.motivo").value("FRAUDE"));
        mvc.perform(json(put("/api/denuncias/{id}", doCliente).with(PROFISSIONAL), "{\"motivo\":\"OUTRO\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(json(put("/api/denuncias/{id}", doCliente).with(CLIENTE), "{\"descricao\":\"sem motivo\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(json(patch("/api/moderacao/denuncias/{id}", doCliente).with(ADMIN), "{\"acao\":\"DESCARTAR\"}"))
                .andExpect(status().isOk());
        mvc.perform(json(put("/api/denuncias/{id}", doCliente).with(CLIENTE), "{\"motivo\":\"OUTRO\"}"))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/denuncias/{id}", doCliente).with(CLIENTE)).andExpect(status().isConflict());

        mvc.perform(delete("/api/denuncias/{id}", doAdmin).with(ADMIN)).andExpect(status().isNoContent());
        mvc.perform(get("/api/denuncias/{id}", doAdmin).with(ADMIN)).andExpect(status().isNotFound());
    }

    @Test
    void notificacaoExcluidaSomentePeloDestinatario() throws Exception {
        enviarProposta();
        long notificacaoId = ler(get("/api/notificacoes").with(PROFISSIONAL)).get("itens").get(0).get("id").asLong();

        mvc.perform(delete("/api/notificacoes/{id}", notificacaoId).with(CLIENTE)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/notificacoes/{id}", notificacaoId).with(PROFISSIONAL)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/notificacoes/{id}", notificacaoId).with(PROFISSIONAL)).andExpect(status().isNotFound());
    }

    @Test
    void consultaIndividualDeExperienciaComentarioENotificacao() throws Exception {
        long experienciaId = criar(post("/api/experiencias").with(PROFISSIONAL),
                "{\"cargo\":\"Professor de violão\",\"organizacao\":\"Escola Livre\",\"dataInicio\":\"2020-03-01\"}")
                .get("id").asLong();
        mvc.perform(get("/api/experiencias/{id}", experienciaId).with(PROFISSIONAL))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cargo").value("Professor de violão"));
        mvc.perform(get("/api/experiencias/{id}", experienciaId).with(CLIENTE)).andExpect(status().isNotFound());

        long comentarioId = criar(post("/api/anuncios/{id}/comentarios", anuncioId).with(CLIENTE),
                "{\"conteudo\":\"Atende aos domingos?\"}").get("id").asLong();
        mvc.perform(get("/api/anuncios/{a}/comentarios/{id}", anuncioId, comentarioId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.conteudo").value("Atende aos domingos?"));
        mvc.perform(get("/api/anuncios/{a}/comentarios/{id}", anuncioId, 999_999)).andExpect(status().isNotFound());

        long notificacaoId = ler(get("/api/notificacoes").with(PROFISSIONAL)).get("itens").get(0).get("id").asLong();
        mvc.perform(get("/api/notificacoes/{id}", notificacaoId).with(PROFISSIONAL)).andExpect(status().isOk());
        mvc.perform(get("/api/notificacoes/{id}", notificacaoId).with(CLIENTE)).andExpect(status().isNotFound());
    }

    private long enviarProposta() throws Exception {
        return criar(post("/api/conversas/{id}/propostas", conversaId).with(CLIENTE), termos("A_VISTA", "80.00", null))
                .get("id").asLong();
    }

    private String termos(String forma, String valor, Integer parcelas) {
        return """
                {"anuncioId":%d,"servico":"Aulas de violão","escopo":"Quatro aulas de 1 hora",
                 "formaPagamento":"%s","valor":%s,"parcelas":%s,"duracao":"1 mês"}
                """.formatted(anuncioId, forma, valor, parcelas);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String corpo) {
        return req.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private JsonNode criar(MockHttpServletRequestBuilder req, String corpo) throws Exception {
        var resposta = mvc.perform(json(req, corpo)).andExpect(status().isCreated()).andReturn();
        return JSON.readTree(resposta.getResponse().getContentAsString());
    }

    private JsonNode ler(MockHttpServletRequestBuilder req) throws Exception {
        return JSON.readTree(mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
}
