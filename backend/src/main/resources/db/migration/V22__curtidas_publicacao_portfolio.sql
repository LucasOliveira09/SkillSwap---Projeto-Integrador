CREATE TABLE curtidas_publicacao (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    publicacao_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_curtida_publicacao_usuario UNIQUE (publicacao_id, usuario_id),
    CONSTRAINT fk_curtida_publicacao FOREIGN KEY (publicacao_id) REFERENCES publicacoes_portfolio (id) ON DELETE CASCADE,
    CONSTRAINT fk_curtida_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_curtida_usuario_publicacao ON curtidas_publicacao (usuario_id, publicacao_id);
