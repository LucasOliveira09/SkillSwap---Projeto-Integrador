ALTER TABLE avaliacoes
    ADD COLUMN edicao_realizada BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE avaliacoes
    ADD COLUMN editada_em DATETIME(6);

CREATE TABLE historico_avaliacao (
    id BIGINT NOT NULL AUTO_INCREMENT,
    avaliacao_id BIGINT NOT NULL,
    editor_id BIGINT NOT NULL,
    nota_anterior INT NOT NULL,
    nota_anuncio_anterior INT,
    depoimento_anterior VARCHAR(1000),
    registrada_em DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_historico_avaliacao UNIQUE (avaliacao_id),
    CONSTRAINT fk_historico_avaliacao FOREIGN KEY (avaliacao_id) REFERENCES avaliacoes (id),
    CONSTRAINT fk_historico_editor FOREIGN KEY (editor_id) REFERENCES usuarios (id)
);
