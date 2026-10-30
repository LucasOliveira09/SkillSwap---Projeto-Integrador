package com.skillswap.api.domain.anuncio;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Migração aditiva do enum existente; Hibernate update não o amplia no MySQL 8.0. */
@Component
@Order(-1)
@RequiredArgsConstructor
public class CategoriaMysqlInicial implements CommandLineRunner {
    private final JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        String produto = jdbc.execute((java.sql.Connection c) -> c.getMetaData().getDatabaseProductName());
        if (!"MySQL".equals(produto)) return;
        var tipos = jdbc.queryForList("SELECT COLUMN_TYPE FROM information_schema.COLUMNS " +
            "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='anuncios' AND COLUMN_NAME='categoria'", String.class);
        if (tipos.isEmpty()) return;
        String tipo = tipos.getFirst().toUpperCase(Locale.ROOT);
        if (!tipo.startsWith("ENUM(") || tipo.contains("'TRANSPORTE'")) return;
        var valores = Pattern.compile("'([^']+)'").matcher(tipo).results()
            .map(m -> m.group(1)).collect(java.util.stream.Collectors.toSet());
        if (!valores.equals(Set.of("AULAS","MANUTENCAO")))
            throw new IllegalStateException("Enum de categoria inesperado; migração não aplicada.");
        jdbc.execute("ALTER TABLE anuncios MODIFY COLUMN categoria ENUM('AULAS','MANUTENCAO','TRANSPORTE') NOT NULL");
    }
}
