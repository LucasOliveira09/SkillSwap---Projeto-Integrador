ALTER TABLE publicacoes_portfolio
    ADD COLUMN moderada_oculta BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE avaliacoes
    ADD COLUMN moderada_oculta BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE denuncias (
    id BIGINT NOT NULL AUTO_INCREMENT,
    autor_id BIGINT NOT NULL,
    tipo_conteudo VARCHAR(32) NOT NULL,
    recurso_id BIGINT NOT NULL,
    motivo VARCHAR(32) NOT NULL,
    descricao VARCHAR(500),
    trecho_conteudo VARCHAR(1000),
    status VARCHAR(16) NOT NULL,
    criada_em DATETIME(6) NOT NULL,
    analisada_em DATETIME(6),
    analisada_por VARCHAR(255),
    CONSTRAINT pk_denuncias PRIMARY KEY (id),
    CONSTRAINT uk_denuncia_autor_recurso UNIQUE (autor_id, tipo_conteudo, recurso_id),
    CONSTRAINT fk_denuncia_autor FOREIGN KEY (autor_id) REFERENCES usuarios (id)
);

CREATE INDEX ix_denuncias_status_criada ON denuncias (status, criada_em, id);
