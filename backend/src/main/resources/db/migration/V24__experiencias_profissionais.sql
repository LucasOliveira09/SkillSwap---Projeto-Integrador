CREATE TABLE experiencias_profissionais (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    cargo VARCHAR(120) NOT NULL,
    organizacao VARCHAR(120) NOT NULL,
    cidade VARCHAR(80) NULL,
    data_inicio DATE NOT NULL,
    data_fim DATE NULL,
    descricao VARCHAR(1500) NULL,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_experiencia_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT ck_experiencia_periodo CHECK (data_fim IS NULL OR data_fim >= data_inicio)
);

CREATE INDEX idx_experiencia_usuario_periodo ON experiencias_profissionais (usuario_id, data_inicio DESC, id DESC);

CREATE TABLE certificacoes_profissionais (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    nome VARCHAR(150) NOT NULL,
    emissor VARCHAR(120) NOT NULL,
    data_emissao DATE NOT NULL,
    data_validade DATE NULL,
    url_credencial VARCHAR(500) NULL,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_certificacao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT ck_certificacao_periodo CHECK (data_validade IS NULL OR data_validade >= data_emissao)
);

CREATE INDEX idx_certificacao_usuario_emissao ON certificacoes_profissionais (usuario_id, data_emissao DESC, id DESC);
