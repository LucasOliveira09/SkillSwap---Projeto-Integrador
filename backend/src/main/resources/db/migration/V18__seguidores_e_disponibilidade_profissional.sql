ALTER TABLE usuarios
    ADD COLUMN disponivel_trabalhos BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE usuario_seguidores (
    seguidor_id BIGINT NOT NULL,
    seguido_id BIGINT NOT NULL,
    criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_usuario_seguidores PRIMARY KEY (seguidor_id, seguido_id),
    CONSTRAINT ck_usuario_seguidores_sem_auto CHECK (seguidor_id <> seguido_id),
    CONSTRAINT fk_usuario_seguidores_seguidor FOREIGN KEY (seguidor_id) REFERENCES usuarios (id),
    CONSTRAINT fk_usuario_seguidores_seguido FOREIGN KEY (seguido_id) REFERENCES usuarios (id)
);

CREATE INDEX idx_usuario_seguidores_seguido ON usuario_seguidores (seguido_id, criado_em, seguidor_id);
