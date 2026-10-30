-- Compara os dados das tabelas legadas em duas cópias descartáveis.
-- Os schemas são fixos para impedir apontamento acidental ao banco público.
-- A consulta é somente leitura e exige os schemas criados no roteiro de migração.

SET SESSION group_concat_max_len = 65535;

SELECT GROUP_CONCAT(
           CONCAT(
               'SELECT ''', comparacao.TABLE_NAME, ''' AS tabela, ',
               '(SELECT COUNT(*) FROM `skillswap_legacy_original_20261027`.`', comparacao.TABLE_NAME,
               '` o LEFT JOIN `skillswap_legacy_migration_20261027`.`', comparacao.TABLE_NAME,
               '` m ON o.id = m.id WHERE m.id IS NULL OR (', comparacao.diferencas, ')) + ',
               '(SELECT COUNT(*) FROM `skillswap_legacy_migration_20261027`.`', comparacao.TABLE_NAME,
               '` m LEFT JOIN `skillswap_legacy_original_20261027`.`', comparacao.TABLE_NAME,
               '` o ON o.id = m.id WHERE o.id IS NULL) AS divergencias'
           )
           ORDER BY comparacao.TABLE_NAME
           SEPARATOR ' UNION ALL '
       )
INTO @sql_comparacao
FROM (
    SELECT origem.TABLE_NAME,
           GROUP_CONCAT(
               CONCAT('NOT (o.`', origem.COLUMN_NAME, '` <=> m.`', origem.COLUMN_NAME, '`)')
               ORDER BY origem.ORDINAL_POSITION
               SEPARATOR ' OR '
           ) AS diferencas
    FROM information_schema.COLUMNS origem
    WHERE origem.TABLE_SCHEMA = 'skillswap_legacy_original_20261027'
      AND origem.TABLE_NAME IN ('usuarios', 'anuncios', 'propostas', 'acordos', 'mensagens', 'avaliacoes', 'item_vitrine')
      AND EXISTS (
          SELECT 1
          FROM information_schema.COLUMNS destino
          WHERE destino.TABLE_SCHEMA = 'skillswap_legacy_migration_20261027'
            AND destino.TABLE_NAME = origem.TABLE_NAME
            AND destino.COLUMN_NAME = origem.COLUMN_NAME
      )
    GROUP BY origem.TABLE_NAME
) comparacao;

PREPARE verificar_copia FROM @sql_comparacao;
EXECUTE verificar_copia;
DEALLOCATE PREPARE verificar_copia;
