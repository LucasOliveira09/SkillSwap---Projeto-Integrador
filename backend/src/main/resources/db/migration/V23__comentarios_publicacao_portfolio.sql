CREATE TABLE comentarios_publicacao (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    publicacao_id BIGINT NOT NULL,
    autor_id BIGINT NOT NULL,
    resposta_a_id BIGINT NULL,
    conteudo VARCHAR(1000) NOT NULL,
    moderada_oculta BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_comentario_publicacao FOREIGN KEY (publicacao_id) REFERENCES publicacoes_portfolio (id) ON DELETE CASCADE,
    CONSTRAINT fk_comentario_publicacao_autor FOREIGN KEY (autor_id) REFERENCES usuarios (id),
    CONSTRAINT fk_comentario_publicacao_resposta FOREIGN KEY (resposta_a_id) REFERENCES comentarios_publicacao (id) ON DELETE CASCADE
);

CREATE INDEX idx_comentario_publicacao_data ON comentarios_publicacao (publicacao_id, resposta_a_id, criado_em, id);
