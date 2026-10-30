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

@SpringBootTest(properties = "skillswap.moderacao.emails=moderador@teste.invalid") @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
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
    private long criarProposta() throws Exception {
        var resultado = mvc.perform(post("/api/anuncios/{id}/propostas", anuncio.getId())
                .header("Authorization", "Bearer " + tokenCliente).contentType("application/json")
                .content("{\"mensagemInicial\":\"Tenho interesse no serviço\"}"))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }
    private long conversaDaProposta(long propostaId) throws Exception {
        var resultado = mvc.perform(get("/api/propostas/{id}/conversa", propostaId)
                .header("Authorization", "Bearer " + tokenCliente)).andExpect(status().isOk()).andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test void umParUsaUmaConversaEmAnunciosDiferentesEContatoDePerfil() throws Exception {
        var segundoAnuncio = anuncios.save(Anuncio.builder().usuario(profissional).titulo("Manutenção de violão")
                .descricao("Ajuste de instrumento").categoria(CategoriaAnuncio.MANUTENCAO).cidade("Ourinhos")
                .modalidade(ModalidadeAnuncio.VENDA).preco(new BigDecimal("45")).build());
        var primeira = mvc.perform(post("/api/conversas").header("Authorization", "Bearer " + tokenCliente)
                .contentType("application/json").content("{\"destinatarioId\":" + profissional.getId()
                        + ",\"anuncioId\":" + anuncio.getId() + "}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.anuncioId").value(anuncio.getId()))
                .andReturn();
        long id = json.readTree(primeira.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/conversas/{id}/mensagens", id).header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(0));

        mvc.perform(post("/api/conversas").header("Authorization", "Bearer " + tokenCliente)
                .contentType("application/json").content("{\"destinatarioId\":" + profissional.getId()
                        + ",\"anuncioId\":" + segundoAnuncio.getId() + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(post("/api/conversas").header("Authorization", "Bearer " + tokenProfissional)
                .contentType("application/json").content("{\"destinatarioId\":" + cliente.getId() + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));

        mvc.perform(post("/api/conversas/{id}/mensagens", id).header("Authorization", "Bearer " + tokenCliente)
                .contentType("application/json").content("{\"conteudo\":\"Tenho interesse neste ajuste\",\"anuncioId\":" + segundoAnuncio.getId() + "}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.conversaId").value(id))
                .andExpect(jsonPath("$.anuncioId").value(segundoAnuncio.getId()))
                .andExpect(jsonPath("$.anuncioTitulo").value("Manutenção de violão"));
        assertThat(jdbc.queryForObject("select count(*) from mensagens where conversa_id=? and remetente_id=?", Long.class, id, cliente.getId())).isEqualTo(1);
        assertThat(mensagens.contarNaoLidas(profissional.getId())).isEqualTo(1);
        mvc.perform(get("/api/conversas").header("Authorization", "Bearer " + tokenProfissional))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1))
                .andExpect(jsonPath("$.itens[0].naoLidas").value(1));
    }

    @Test void comentarioPublicoPodeSerDenunciadoEOcultadoPelaModeracao() throws Exception {
        var criado = mvc.perform(post("/api/anuncios/{id}/comentarios", anuncio.getId())
                .header("Authorization", "Bearer " + tokenCliente).contentType("application/json")
                .content("{\"conteudo\":\"Atende durante a semana?\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.conteudo").value("Atende durante a semana?"))
                .andExpect(jsonPath("$.podeResponder").value(true)).andReturn();
        long comentarioId = json.readTree(criado.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(get("/api/anuncios/{id}/comentarios", anuncio.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1));

        var denuncia = mvc.perform(post("/api/denuncias").header("Authorization", "Bearer " + tokenProfissional)
                .contentType("application/json").content("{\"tipoConteudo\":\"COMENTARIO_ANUNCIO\",\"recursoId\":"
                        + comentarioId + ",\"motivo\":\"OUTRO\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.tipoConteudo").value("COMENTARIO_ANUNCIO"))
                .andReturn();
        long denunciaId = json.readTree(denuncia.getResponse().getContentAsString()).get("id").asLong();
        var moderador = usuarios.register(new RegisterDTO("Moderadora", "moderador@teste.invalid", "senha-teste-123"));
        String tokenModerador = jwt.generateToken(moderador.getEmail());

        mvc.perform(patch("/api/moderacao/denuncias/{id}", denunciaId).header("Authorization", "Bearer " + tokenModerador)
                .contentType("application/json").content("{\"acao\":\"OCULTAR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OCULTADA"));
        mvc.perform(get("/api/anuncios/{id}/comentarios", anuncio.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(0));

        mvc.perform(patch("/api/moderacao/denuncias/{id}", denunciaId).header("Authorization", "Bearer " + tokenModerador)
                .contentType("application/json").content("{\"acao\":\"REEXIBIR\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REEXIBIDA"));
        mvc.perform(get("/api/anuncios/{id}/comentarios", anuncio.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(1));
    }

    @Test void propostaNoChatPersisteTermosComParcelamentoLimitado() throws Exception {
        long conversaId = abrir();
        var proposta = mvc.perform(post("/api/conversas/{id}/propostas", conversaId)
                .header("Authorization", "Bearer " + tokenCliente).contentType("application/json")
                .content("""
                        {"anuncioId":%d,"servico":"Aulas de violão","escopo":"Quatro aulas individuais",
                         "formaPagamento":"PARCELADO","valor":320.00,"parcelas":4,
                         "duracao":"1 mês","validadeEm":"2027-01-15"}
                        """.formatted(anuncio.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.servico").value("Aulas de violão"))
                .andExpect(jsonPath("$.escopo").value("Quatro aulas individuais"))
                .andExpect(jsonPath("$.formaPagamento").value("PARCELADO"))
                .andExpect(jsonPath("$.valor").value(320.00))
                .andExpect(jsonPath("$.parcelas").value(4))
                .andExpect(jsonPath("$.duracao").value("1 mês"))
                .andReturn();
        long propostaId = json.readTree(proposta.getResponse().getContentAsString()).get("id").asLong();
        assertThat(propostas.findById(propostaId).orElseThrow().getConversa().getId()).isEqualTo(conversaId);
        mvc.perform(get("/api/conversas/{id}/mensagens", conversaId).header("Authorization", "Bearer " + tokenProfissional))
                .andExpect(status().isOk()).andExpect(jsonPath("$.itens[1].propostaId").value(propostaId))
                .andExpect(jsonPath("$.itens[1].anuncioTitulo").value("Aula de violão"));

        mvc.perform(post("/api/conversas/{id}/propostas", conversaId)
                .header("Authorization", "Bearer " + tokenCliente).contentType("application/json")
                .content("""
                        {"anuncioId":%d,"servico":"Aulas","escopo":"Plano semanal",
                         "formaPagamento":"PARCELADO","valor":100,"parcelas":13}
                        """.formatted(anuncio.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test void trocaNaoAceitaComplementoEmDinheiroNaProposta() throws Exception {
        anuncio.setModalidade(ModalidadeAnuncio.TROCA);
        anuncio.setPreco(null);
        anuncios.saveAndFlush(anuncio);
        long conversaId = abrir();
        mvc.perform(post("/api/conversas/{id}/propostas", conversaId)
                .header("Authorization", "Bearer " + tokenCliente).contentType("application/json")
                .content("""
                        {"anuncioId":%d,"servico":"Aula de violão","escopo":"Em troca de aulas de francês",
                         "formaPagamento":"TROCA","valor":10}
                        """.formatted(anuncio.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test void propostaVencidaNaoPodeSerAceitaMesmoSeAindaConstarPendente() throws Exception {
        long conversaId = abrir();
        var resultado = mvc.perform(post("/api/conversas/{id}/propostas", conversaId)
                .header("Authorization", "Bearer " + tokenCliente).contentType("application/json")
                .content("""
                        {"anuncioId":%d,"servico":"Aula de violão","escopo":"Duas aulas",
                         "formaPagamento":"A_VISTA","valor":160,"validadeEm":"2027-01-15"}
                        """.formatted(anuncio.getId())))
                .andExpect(status().isCreated()).andReturn();
        long propostaId = json.readTree(resultado.getResponse().getContentAsString()).get("id").asLong();
        propostas.findById(propostaId).orElseThrow().setValidadeEm(
                java.time.LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")).minusDays(1));
        em.flush();

        mvc.perform(patch("/api/propostas/{id}/aceitar", propostaId)
                .header("Authorization", "Bearer " + tokenProfissional))
                .andExpect(status().isBadRequest());
        assertThat(propostas.findById(propostaId).orElseThrow().getStatus()).isEqualTo(StatusProposta.PENDENTE);
    }

    @Test void comentariosDoAnuncioSaoPublicosERespostasFicamRestritasAoAutorEAnunciante() throws Exception {
        mvc.perform(get("/api/anuncios/{id}/comentarios", anuncio.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(0));
        mvc.perform(post("/api/anuncios/{id}/comentarios", anuncio.getId()).contentType("application/json")
                .content("{\"conteudo\":\"Tenho interesse\"}")).andExpect(status().isUnauthorized());

        var raiz = mvc.perform(post("/api/anuncios/{id}/comentarios", anuncio.getId())
                .header("Authorization", "Bearer " + tokenCliente).contentType("application/json")
                .content("{\"conteudo\":\"Atende aos sábados?\"}"))
                .andExpect(status().isCreated()).andReturn();
        long raizId = json.readTree(raiz.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/anuncios/{id}/comentarios", anuncio.getId())
                .header("Authorization", "Bearer " + tokenTerceiro).contentType("application/json")
                .content("{\"conteudo\":\"Resposta indevida\",\"respostaAId\":" + raizId + "}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/anuncios/{id}/comentarios", anuncio.getId())
                .header("Authorization", "Bearer " + tokenProfissional).contentType("application/json")
                .content("{\"conteudo\":\"Sim, tenho horário\",\"respostaAId\":" + raizId + "}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.podeResponder").value(false));
        mvc.perform(post("/api/anuncios/{id}/comentarios", anuncio.getId())
                .header("Authorization", "Bearer " + tokenCliente).contentType("application/json")
                .content("{\"conteudo\":\"Obrigado!\",\"respostaAId\":" + raizId + "}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/anuncios/{id}/comentarios", anuncio.getId())
                .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(3))
                .andExpect(jsonPath("$.itens[0].podeResponder").value(true));
    }

    @Test void avaliacoesDoAnuncioExigemOfertaPublica() throws Exception {
        mvc.perform(get("/api/anuncios/{id}/avaliacoes", anuncio.getId()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        anuncio.setStatus(StatusAnuncio.INATIVO);
        anuncios.saveAndFlush(anuncio);
        mvc.perform(get("/api/anuncios/{id}/avaliacoes", anuncio.getId()))
                .andExpect(status().isNotFound());
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
                .andExpect(jsonPath("$.itens[0].recursoUrl").value("/mensagens?conversa=chat-"+id));
    }
    @Test void repetirContatoReutilizaConversaSemPerderMensagens() throws Exception {
        long id = abrir();
        mvc.perform(post("/api/anuncios/{id}/conversas", anuncio.getId()).header("Authorization", "Bearer " + tokenCliente)
                .contentType("application/json").content("{\"conteudo\":\"Também gostaria de saber o local.\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        assertThat(propostas.findByAnuncioId(anuncio.getId())).isEmpty();
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
        long proposta = criarProposta();
        long id = conversaDaProposta(proposta);
        mvc.perform(patch("/api/propostas/{id}", proposta).header("Authorization", "Bearer " + tokenProfissional)
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
                .contentType("application/json").content("{\"conteudo\":\"Continuar falando depois do acordo\"}")).andExpect(status().isCreated());
    }
    @Test void naoPermiteContatoComProprioAnuncioOuExemplo() throws Exception {
        mvc.perform(post("/api/anuncios/{id}/conversas", anuncio.getId()).header("Authorization", "Bearer " + tokenProfissional)
                .contentType("application/json").content("{\"conteudo\":\"Teste\"}")).andExpect(status().isBadRequest());
        anuncio.setExemplo(true); anuncios.saveAndFlush(anuncio);
        mvc.perform(post("/api/anuncios/{id}/conversas", anuncio.getId()).header("Authorization", "Bearer " + tokenCliente)
                .contentType("application/json").content("{\"conteudo\":\"Teste\"}")).andExpect(status().isNotFound());
        assertThat(propostas.findByAnuncioId(anuncio.getId())).isEmpty();
    }
    @Test void leituraConsideraDataDasMensagensImportadasENaoSomenteId() throws Exception {
        long id = abrir();
        mvc.perform(post("/api/conversas/{id}/mensagens", id).header("Authorization", "Bearer " + tokenCliente)
                .contentType("application/json").content("{\"conteudo\":\"Mensagem recente\"}")).andExpect(status().isCreated());
        var ultima = mensagens.ultimas(java.util.List.of(id)).getFirst();
        long idImportado = ultima.getId() + 100;
        jdbc.update("insert into mensagens (id,conversa_id,remetente_id,conteudo,enviado_em) values (?,?,?,?,?)",
                idImportado, id, cliente.getId(),
                "Inicial importada com ID maior", java.sql.Timestamp.valueOf(ultima.getEnviadoEm().minusDays(1)));
        mvc.perform(patch("/api/conversas/{id}/leitura", id).header("Authorization", "Bearer " + tokenProfissional)
                .contentType("application/json").content("{\"ultimaMensagemId\":"+ultima.getId()+"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/conversas/resumo").header("Authorization", "Bearer " + tokenProfissional))
                .andExpect(jsonPath("$.naoLidas").value(0));
    }

    @Test void recuperaVinculoDeMensagemGravadaPorApiAnteriorDuranteReversao() throws Exception {
        long proposta = criarProposta();
        long id = conversaDaProposta(proposta);
        mvc.perform(patch("/api/propostas/{id}", proposta).header("Authorization", "Bearer " + tokenProfissional)
                .contentType("application/json").content("{\"status\":\"ACEITA\"}")).andExpect(status().isOk());
        long acordo = jdbc.queryForObject("select id from acordos where proposta_id=?", Long.class, proposta);
        jdbc.update("insert into mensagens (acordo_id,remetente_id,conteudo,enviado_em) values (?,?,?,?)",
                acordo, profissional.getId(), "Mensagem da API anterior", java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
        reconciliador.reconciliar();
        em.clear();
        mvc.perform(get("/api/conversas/{id}/mensagens", id).header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalItens").value(2));
    }

}
