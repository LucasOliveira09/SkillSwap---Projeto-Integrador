package com.skillswap.api;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.assertThat;

class MigracaoConversasTest {
    @Test void migraPrimeirosContatosPreservandoIdsDatasEHistorico() {
        String url = System.getenv("SKILLSWAP_MIGRATION_DB_URL");
        String usuario = "sa", senha = "";
        if (url == null) url = "jdbc:h2:mem:chat_migracao_" + java.util.UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        else {
            if (!url.matches("jdbc:mysql://[^/]+/skillswap_chat_migracao_[0-9_]+(?:\\?.*)?"))
                throw new IllegalArgumentException("Use exclusivamente um schema descartavel de migracao");
            usuario = System.getenv("SKILLSWAP_MIGRATION_DB_USER");
            senha = System.getenv("SKILLSWAP_MIGRATION_DB_PASSWORD");
        }
        var fonte = new DriverManagerDataSource(url, usuario, senha);
        var jdbc = new JdbcTemplate(fonte);
        var flyway = Flyway.configure().dataSource(fonte).target("15").cleanDisabled(true).load();
        flyway.migrate();
        jdbc.update("insert into usuarios (id,nome,email,senha,criado_em) values (1,'Ana','ana@teste.invalid','hash-teste','2026-10-20 10:00:00'),(2,'Bruno','bruno@teste.invalid','hash-teste','2026-10-20 10:00:00')");
        jdbc.update("insert into anuncios (id,titulo,descricao,categoria,cidade,modalidade,preco,status,usuario_id,criado_em,exemplo) values (3,'Aula de violao','Aula individual','AULAS','Ourinhos','VENDA',80,'ATIVO',1,'2026-10-20 10:00:00',false)");
        jdbc.update("insert into propostas (id,anuncio_id,proponente_id,mensagem_inicial,status,criado_em) values (5,3,2,'Mensagem inicial aceita','ACEITA','2026-10-21 10:00:00'),(6,3,2,'Primeiro contato pendente','PENDENTE','2026-10-22 10:00:00')");
        jdbc.update("insert into acordos (id,proposta_id,status,criado_em) values (7,5,'EM_ANDAMENTO','2026-10-22 10:00:00')");
        jdbc.update("insert into mensagens (id,acordo_id,remetente_id,conteudo,enviado_em) values (41,7,1,'Resposta antiga preservada','2026-10-23 10:00:00')");
        Flyway.configure().dataSource(fonte).cleanDisabled(true).load().migrate();
        assertThat(jdbc.queryForObject("select count(*) from mensagens", Long.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select conteudo from mensagens where id=41", String.class)).isEqualTo("Resposta antiga preservada");
        assertThat(jdbc.queryForObject("select proposta_id from mensagens where id=41", Long.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select count(*) from mensagens where proposta_id=6 and acordo_id is null and conteudo='Primeiro contato pendente' and enviado_em='2026-10-22 10:00:00'", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from mensagens where proposta_id=5 and acordo_id=7", Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select ultima_lida_anunciante from propostas where id=5", Long.class)).isZero();
        Flyway.configure().dataSource(fonte).cleanDisabled(true).load().migrate();
        assertThat(jdbc.queryForObject("select count(*) from mensagens", Long.class)).isEqualTo(3);
    }
}
