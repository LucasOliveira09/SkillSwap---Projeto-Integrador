CREATE TABLE notificacoes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    destinatario_id BIGINT NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    titulo VARCHAR(120) NOT NULL,
    mensagem VARCHAR(255) NOT NULL,
    recurso_url VARCHAR(160) NOT NULL,
    lida_em DATETIME(6),
    criada_em DATETIME(6) NOT NULL,
    CONSTRAINT pk_notificacoes PRIMARY KEY (id),
    CONSTRAINT fk_notificacoes_destinatario FOREIGN KEY (destinatario_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_notificacoes_destinatario_criada ON notificacoes (destinatario_id, criada_em);
