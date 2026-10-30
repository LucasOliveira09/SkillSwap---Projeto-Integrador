CREATE TABLE contestacoes_avaliacao (
    id BIGINT NOT NULL AUTO_INCREMENT,
    avaliacao_id BIGINT NOT NULL,
    contestador_id BIGINT NOT NULL,
    justificativa VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ABERTA',
    decisao VARCHAR(1000),
    criada_em DATETIME(6) NOT NULL,
    analisada_em DATETIME(6),
    analisada_por VARCHAR(320),
    PRIMARY KEY (id),
    CONSTRAINT uk_contestacao_avaliacao UNIQUE (avaliacao_id),
    CONSTRAINT fk_contestacao_avaliacao FOREIGN KEY (avaliacao_id) REFERENCES avaliacoes (id),
    CONSTRAINT fk_contestacao_contestador FOREIGN KEY (contestador_id) REFERENCES usuarios (id),
    INDEX idx_contestacao_status_data (status, criada_em, id)
);
