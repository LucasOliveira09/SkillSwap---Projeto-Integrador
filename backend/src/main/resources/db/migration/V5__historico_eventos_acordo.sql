CREATE TABLE eventos_acordo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    acordo_id BIGINT NOT NULL,
    ator_id BIGINT NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    criado_em DATETIME(6) NOT NULL,
    CONSTRAINT pk_eventos_acordo PRIMARY KEY (id),
    CONSTRAINT fk_eventos_acordo_acordo FOREIGN KEY (acordo_id) REFERENCES acordos (id),
    CONSTRAINT fk_eventos_acordo_ator FOREIGN KEY (ator_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_eventos_acordo_data ON eventos_acordo (acordo_id, criado_em, id);
