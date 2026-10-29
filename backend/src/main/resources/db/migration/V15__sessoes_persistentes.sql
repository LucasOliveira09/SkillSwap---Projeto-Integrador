CREATE TABLE sessoes_persistentes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expira_em DATETIME(6) NOT NULL,
    CONSTRAINT fk_sessao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    INDEX idx_sessao_expiracao (expira_em)
);
