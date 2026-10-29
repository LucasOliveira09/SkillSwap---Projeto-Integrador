package com.skillswap.api;

import com.skillswap.api.domain.usuario.*;
import com.skillswap.api.domain.usuario.dto.RegisterDTO;
import com.skillswap.api.domain.anuncio.*;
import com.skillswap.api.domain.proposta.*;
import com.skillswap.api.domain.mensagem.*;
import com.skillswap.api.infra.security.JwtService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class ConversaAnuncioTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarios;
    @Autowired AnuncioRepository anuncios;
    @Autowired PropostaRepository propostas;
    @Autowired MensagemRepository mensagens;
    @Autowired JwtService jwt;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired jakarta.persistence.EntityManager em;
    @Autowired com.skillswap.api.domain.conversa.ReconciliadorMensagensLegadas reconciliador;
    private Usuario profissional, cliente;
    private String tokenProfissional, tokenCliente, tokenTerceiro;
    private Anuncio anuncio;
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach void preparar() {
        profissional = usuarios.register(new RegisterDTO("Ana Anunciante", "anunciante-chat@teste.invalid", "senha-teste-123"));
        cliente = usuarios.register(new RegisterDTO("Bruno Cliente", "cliente-chat@teste.invalid", "senha-teste-123"));
        var terceiro = usuarios.register(new RegisterDTO("Terceiro", "terceiro-chat@teste.invalid", "senha-teste-123"));
        tokenProfissional = jwt.generateToken(profissional.getEmail());
        tokenCliente = jwt.generateToken(cliente.getEmail());
        tokenTerceiro = jwt.generateToken(terceiro.getEmail());
        anuncio = anuncios.save(Anuncio.builder().usuario(profissional).titulo("Aula de violão")
                .descricao("Aulas individuais").categoria(CategoriaAnuncio.AULAS).cidade("Ourinhos")
                .modalidade(ModalidadeAnuncio.VENDA).preco(new BigDecimal("80")).build());
    }
    private long abrir() throws Exception {
        var resultado = mvc.perform(post("/api/anuncios/{id}/conversas", anuncio.getId())
                .header("Authorization", "Bearer " + tokenCliente).contentType("application/json")
                .content("{\"conteudo\":\"Olá, atende aos sábados?\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.anuncioId").value(anuncio.getId()))
                .andExpect(jsonPath("$.anuncioTitulo").value("Aula de violão"))
                .andExpect(jsonPath("$.acordoId").isEmpty()).andExpect(jsonPath("$.podeEnviar").value(true))
                .andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }
    @Test void primeiroContatoChegaAoAnuncianteAntesDeAceitarProposta() throws Exception {
        long id = abrir();
        mvc.perform(get("/api/conversas").header("Authorization", "Bearer " + tokenProfissional))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].naoLidas").value(1))
                .andExpect(jsonPath("$.itens[0].ultimaMensagem.conteudo").value("Olá, atende aos sábados?"));
        mvc.perform(post("/api/conversas/{id}/mensagens", id).header("Authorization", "Bearer " + tokenProfissional)
                .contentType("application/json").content("{\"conteudo\":\"Sim, podemos combinar às 10h.\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/conversas/{id}/mensagens", id).header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(2))
                .andExpect(jsonPath("$.itens[1].conteudo").value("Sim, podemos combinar às 10h."));
        mvc.perform(get("/api/conversas/resumo").header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.naoLidas").value(1));
        mvc.perform(get("/api/notificacoes").header("Authorization", "Bearer " + tokenProfissional))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[0].mensagem", org.hamcrest.Matchers.containsString("Aula de violão")))
                .andExpect(jsonPath("$.itens[0].recursoUrl").value("/mensagens?conversa="+id));
    }
    @Test void repetirContatoReutilizaConversaSemPerderMensagens() throws Exception {
        long id = abrir();
        mvc.perform(post("/api/anuncios/{id}/conversas", anuncio.getId()).header("Authorization", "Bearer " + tokenCliente)
                .contentType("application/json").content("{\"conteudo\":\"Também gostaria de saber o local.\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        assertThat(propostas.findByAnuncioId(anuncio.getId())).hasSize(1);
        mvc.perform(get("/api/conversas/{id}/mensagens", id).header("Authorization", "Bearer " + tokenProfissional))
                .andExpect(jsonPath("$.totalItens").value(2));
    }
    @Test void terceirosNaoLeemNemEnviamOuMarcamLeitura() throws Exception {
        long id = abrir();
        mvc.perform(get("/api/conversas/{id}", id).header("Authorization", "Bearer " + tokenTerceiro)).andExpect(status().isForbidden());
        mvc.perform(get("/api/conversas/{id}/mensagens", id).header("Authorization", "Bearer " + tokenTerceiro)).andExpect(status().isForbidden());
        mvc.perform(post("/api/conversas/{id}/mensagens", id).header("Authorization", "Bearer " + tokenTerceiro)
                .contentType("application/json").content("{\"conteudo\":\"Intrusão\"}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/conversas/{id}/leitura", id).header("Authorization", "Bearer " + tokenTerceiro)
                .contentType("application/json").content("{\"ultimaMensagemId\":1}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/conversas").header("Authorization", "Bearer " + tokenTerceiro)).andExpect(jsonPath("$.totalItens").value(0));
        mvc.perform(get("/api/conversas/resumo")).andExpect(status().isUnauthorized());
    }
    @Test void leituraMonotonicaConfereMensagemDaConversa() throws Exception {
        long id = abrir();
        var primeira = mensagens.ultimas(java.util.List.of(id)).getFirst();
        mvc.perform(patch("/api/conversas/{id}/leitura", id).header("Authorization", "Bearer " + tokenProfissional)
                .contentType("application/json").content("{\"ultimaMensagemId\":"+primeira.getId()+"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/conversas/resumo").header("Authorization", "Bearer " + tokenProfissional))
                .andExpect(jsonPath("$.naoLidas").value(0));
        mvc.perform(post("/api/conversas/{id}/mensagens", id).header("Authorization", "Bearer " + tokenCliente)
                .contentType("application/json").content("{\"conteudo\":\"Nova pergunta\"}")).andExpect(status().isCreated());
        mvc.perform(patch("/api/conversas/{id}/leitura", id).header("Authorization", "Bearer " + tokenProfissional)
                .contentType("application/json").content("{\"ultimaMensagemId\":"+primeira.getId()+"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/conversas/resumo").header("Authorization", "Bearer " + tokenProfissional))
                .andExpect(jsonPath("$.naoLidas").value(1));
        mvc.perform(patch("/api/conversas/{id}/leitura", id).header("Authorization", "Bearer " + tokenProfissional)
                .contentType("application/json").content("{\"ultimaMensagemId\":999999}"))
                .andExpect(status().isNotFound());
    }
    @Test void mantemHistoricoDepoisDoAceiteENaoPermiteEnviarNoAcordoEncerrado() throws Exception {
        long id = abrir();
        mvc.perform(patch("/api/propostas/{id}", id).header("Authorization", "Bearer " + tokenProfissional)
                .contentType("application/json").content("{\"status\":\"ACEITA\"}")).andExpect(status().isOk());
        var acordoResposta = mvc.perform(get("/api/acordos").header("Authorization", "Bearer " + tokenCliente)).andReturn();
        long acordo = json.readTree(acordoResposta.getResponse().getContentAsString()).get("itens").get(0).get("id").asLong();
        mvc.perform(get("/api/acordos/{id}/mensagens", acordo).header("Authorization", "Bearer " + tokenProfissional))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1));
        mvc.perform(post("/api/acordos/{id}/mensagens", acordo).header("Authorization", "Bearer " + tokenProfissional)
                .contentType("application/json").content("{\"conteudo\":\"Resposta pela rota anterior\"}")).andExpect(status().isCreated());
        mvc.perform(get("/api/conversas/{id}/mensagens", id).header("Authorization", "Bearer " + tokenCliente)).andExpect(jsonPath("$.totalItens").value(2));
        mvc.perform(patch("/api/acordos/{id}", acordo).header("Authorization", "Bearer " + tokenProfissional)
                .contentType("application/json").content("{\"status\":\"CANCELADO\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/conversas/{id}/mensagens", id).header("Authorization", "Bearer " + tokenCliente)
                .contentType("application/json").content("{\"conteudo\":\"Depois de encerrar\"}")).andExpect(status().isBadRequest());
    }
    @Test void naoPermiteContatoComProprioAnuncioOuExemplo() throws Exception {
        mvc.perform(post("/api/anuncios/{id}/conversas", anuncio.getId()).header("Authorization", "Bearer " + tokenProfissional)
                .contentType("application/json").content("{\"conteudo\":\"Teste\"}")).andExpect(status().isBadRequest());
        anuncio.setExemplo(true); anuncios.saveAndFlush(anuncio);
        mvc.perform(post("/api/anuncios/{id}/conversas", anuncio.getId()).header("Authorization", "Bearer " + tokenCliente)
                .contentType("application/json").content("{\"conteudo\":\"Teste\"}")).andExpect(status().isBadRequest());
        assertThat(propostas.findByAnuncioId(anuncio.getId())).isEmpty();
    }
    @Test void leituraConsideraDataDasMensagensImportadasENaoSomenteId() throws Exception {
        long id = abrir();
        mvc.perform(post("/api/conversas/{id}/mensagens", id).header("Authorization", "Bearer " + tokenCliente)
                .contentType("application/json").content("{\"conteudo\":\"Mensagem recente\"}")).andExpect(status().isCreated());
        var ultima = mensagens.ultimas(java.util.List.of(id)).getFirst();
        long idImportado = ultima.getId() + 100;
        jdbc.update("insert into mensagens (id,proposta_id,remetente_id,conteudo,enviado_em) values (?,?,?,?,?)",
                idImportado, id, cliente.getId(), "Inicial importada com ID maior", java.sql.Timestamp.valueOf(ultima.getEnviadoEm().minusDays(1)));
        mvc.perform(patch("/api/conversas/{id}/leitura", id).header("Authorization", "Bearer " + tokenProfissional)
                .contentType("application/json").content("{\"ultimaMensagemId\":"+ultima.getId()+"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/conversas/resumo").header("Authorization", "Bearer " + tokenProfissional))
                .andExpect(jsonPath("$.naoLidas").value(0));
    }

    @Test void recuperaVinculoDeMensagemGravadaPorApiAnteriorDuranteReversao() throws Exception {
        long id = abrir();
        mvc.perform(patch("/api/propostas/{id}", id).header("Authorization", "Bearer " + tokenProfissional)
                .contentType("application/json").content("{\"status\":\"ACEITA\"}")).andExpect(status().isOk());
        long acordo = jdbc.queryForObject("select id from acordos where proposta_id=?", Long.class, id);
        jdbc.update("insert into mensagens (acordo_id,remetente_id,conteudo,enviado_em) values (?,?,?,?)",
                acordo, profissional.getId(), "Mensagem da API anterior", java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
        reconciliador.reconciliar();
        em.clear();
        mvc.perform(get("/api/conversas/{id}/mensagens", id).header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(2));
    }

}
