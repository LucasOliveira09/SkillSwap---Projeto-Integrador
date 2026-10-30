CREATE TABLE comentarios_anuncio (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    anuncio_id BIGINT NOT NULL,
    autor_id BIGINT NOT NULL,
    resposta_a_id BIGINT NULL,
    conteudo VARCHAR(1000) NOT NULL,
    moderada_oculta BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comentario_anuncio FOREIGN KEY (anuncio_id) REFERENCES anuncios(id),
    CONSTRAINT fk_comentario_autor FOREIGN KEY (autor_id) REFERENCES usuarios(id),
    CONSTRAINT fk_comentario_resposta FOREIGN KEY (resposta_a_id) REFERENCES comentarios_anuncio(id)
);

CREATE INDEX idx_comentario_anuncio_data ON comentarios_anuncio (anuncio_id, criado_em, id);
CREATE INDEX idx_comentario_resposta_data ON comentarios_anuncio (resposta_a_id, criado_em, id);
