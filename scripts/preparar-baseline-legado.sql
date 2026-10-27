-- Compatibiliza somente a estrutura legada da cópia descartável previamente auditada.
-- Este arquivo é intencionalmente preso ao schema de ensaio abaixo. Para outra cópia,
-- revise-a e gere um script novo; não troque o alvo por DATABASE() ou variável livre.
-- O script não cria flyway_schema_history; faça o baseline V1 separadamente.

SET @skillswap_schema_alvo := 'skillswap_legacy_migration_20261027';
SET @skillswap_schema_esperado := (
    DATABASE() = @skillswap_schema_alvo
    AND (
        SELECT COUNT(DISTINCT TABLE_NAME)
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = @skillswap_schema_alvo
          AND TABLE_NAME IN ('usuarios', 'anuncios', 'propostas', 'acordos', 'mensagens', 'avaliacoes', 'item_vitrine')
    ) = 7
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = @skillswap_schema_alvo
          AND TABLE_NAME = 'flyway_schema_history'
    )
);

SELECT DATABASE() AS schema_conectado,
       @skillswap_schema_alvo AS schema_unico_permitido,
       @skillswap_schema_esperado AS guard_aprovado;

-- Cada DDL é preparado somente se conexão, estrutura legada e ausência de histórico
-- corresponderem ao clone aprovado. Em qualquer outro schema, executa apenas SELECT 1.
SET @skillswap_sql := IF(@skillswap_schema_esperado,
    'ALTER TABLE `skillswap_legacy_migration_20261027`.anuncios
       MODIFY COLUMN categoria VARCHAR(255) NOT NULL,
       MODIFY COLUMN modalidade VARCHAR(255) NOT NULL,
       MODIFY COLUMN status VARCHAR(255) NOT NULL',
    'SELECT 1');
PREPARE skillswap_stmt FROM @skillswap_sql;
EXECUTE skillswap_stmt;
DEALLOCATE PREPARE skillswap_stmt;

SET @skillswap_sql := IF(@skillswap_schema_esperado,
    'ALTER TABLE `skillswap_legacy_migration_20261027`.propostas MODIFY COLUMN status VARCHAR(255) NOT NULL',
    'SELECT 1');
PREPARE skillswap_stmt FROM @skillswap_sql;
EXECUTE skillswap_stmt;
DEALLOCATE PREPARE skillswap_stmt;

SET @skillswap_sql := IF(@skillswap_schema_esperado,
    'ALTER TABLE `skillswap_legacy_migration_20261027`.acordos MODIFY COLUMN status VARCHAR(255) NOT NULL',
    'SELECT 1');
PREPARE skillswap_stmt FROM @skillswap_sql;
EXECUTE skillswap_stmt;
DEALLOCATE PREPARE skillswap_stmt;

-- A V1 inclui a nota da oferta, ausente em alguns schemas anteriores.
SET @skillswap_nota_anuncio_existe := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @skillswap_schema_alvo
      AND TABLE_NAME = 'avaliacoes'
      AND COLUMN_NAME = 'nota_anuncio'
);
SET @skillswap_sql := IF(@skillswap_schema_esperado AND @skillswap_nota_anuncio_existe = 0,
    'ALTER TABLE `skillswap_legacy_migration_20261027`.avaliacoes ADD COLUMN nota_anuncio INT NULL',
    'SELECT 1');
PREPARE skillswap_stmt FROM @skillswap_sql;
EXECUTE skillswap_stmt;
DEALLOCATE PREPARE skillswap_stmt;

-- Essas tabelas foram adicionadas depois da primeira versão do schema legado.
SET @skillswap_sql := IF(@skillswap_schema_esperado,
    'CREATE TABLE IF NOT EXISTS `skillswap_legacy_migration_20261027`.publicacoes_portfolio (
        id BIGINT NOT NULL AUTO_INCREMENT,
        legenda VARCHAR(500),
        usuario_id BIGINT NOT NULL,
        anuncio_id BIGINT,
        criado_em DATETIME(6) NOT NULL,
        CONSTRAINT pk_publicacoes_portfolio PRIMARY KEY (id),
        CONSTRAINT fk_publicacoes_usuario FOREIGN KEY (usuario_id) REFERENCES `skillswap_legacy_migration_20261027`.usuarios (id),
        CONSTRAINT fk_publicacoes_anuncio FOREIGN KEY (anuncio_id) REFERENCES `skillswap_legacy_migration_20261027`.anuncios (id)
    )',
    'SELECT 1');
PREPARE skillswap_stmt FROM @skillswap_sql;
EXECUTE skillswap_stmt;
DEALLOCATE PREPARE skillswap_stmt;

SET @skillswap_sql := IF(@skillswap_schema_esperado,
    'CREATE TABLE IF NOT EXISTS `skillswap_legacy_migration_20261027`.publicacao_portfolio_fotos (
        publicacao_id BIGINT NOT NULL,
        ordem INT NOT NULL,
        foto_url VARCHAR(120) NOT NULL,
        CONSTRAINT pk_publicacao_fotos PRIMARY KEY (publicacao_id, ordem),
        CONSTRAINT fk_publicacao_fotos_publicacao FOREIGN KEY (publicacao_id)
            REFERENCES `skillswap_legacy_migration_20261027`.publicacoes_portfolio (id)
    )',
    'SELECT 1');
PREPARE skillswap_stmt FROM @skillswap_sql;
EXECUTE skillswap_stmt;
DEALLOCATE PREPARE skillswap_stmt;

-- Inspeção pós-execução. A migração V1–V14 e Hibernate validate continuam sendo
-- a validação estrutural completa antes de baselinar qualquer cópia.
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, COLUMN_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @skillswap_schema_alvo
  AND ((TABLE_NAME = 'anuncios' AND COLUMN_NAME IN ('categoria', 'modalidade', 'status'))
    OR (TABLE_NAME IN ('propostas', 'acordos') AND COLUMN_NAME = 'status')
    OR (TABLE_NAME = 'avaliacoes' AND COLUMN_NAME = 'nota_anuncio'))
ORDER BY TABLE_NAME, COLUMN_NAME;

SELECT TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = @skillswap_schema_alvo
  AND TABLE_NAME IN ('publicacoes_portfolio', 'publicacao_portfolio_fotos')
ORDER BY TABLE_NAME;
