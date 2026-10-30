CREATE TABLE conversas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    participante_menor_id BIGINT NOT NULL,
    participante_maior_id BIGINT NOT NULL,
    anuncio_contexto_id BIGINT NULL,
    criado_em DATETIME(6) NOT NULL,
    ultima_atividade_em DATETIME(6) NOT NULL,
    ultima_lida_menor BIGINT NOT NULL DEFAULT 0,
    ultima_leitura_menor_em DATETIME(6) NULL,
    ultima_lida_maior BIGINT NOT NULL DEFAULT 0,
    ultima_leitura_maior_em DATETIME(6) NULL,
    CONSTRAINT pk_conversas PRIMARY KEY (id),
    CONSTRAINT uk_conversas_par UNIQUE (participante_menor_id, participante_maior_id),
    CONSTRAINT ck_conversas_participantes CHECK (participante_menor_id < participante_maior_id),
    CONSTRAINT fk_conversas_menor FOREIGN KEY (participante_menor_id) REFERENCES usuarios(id),
    CONSTRAINT fk_conversas_maior FOREIGN KEY (participante_maior_id) REFERENCES usuarios(id),
    CONSTRAINT fk_conversas_anuncio FOREIGN KEY (anuncio_contexto_id) REFERENCES anuncios(id)
);

-- Reuse the first proposal ID in each pair. This makes every legacy ?conversa=<propostaId>
-- resolve unambiguously to the canonical conversation or the legacy proposal resource.
INSERT INTO conversas (id, participante_menor_id, participante_maior_id, criado_em, ultima_atividade_em)
SELECT MIN(p.id), LEAST(p.proponente_id, a.usuario_id), GREATEST(p.proponente_id, a.usuario_id),
       MIN(p.criado_em), MAX(COALESCE(m.enviado_em, p.criado_em))
FROM propostas p
JOIN anuncios a ON a.id = p.anuncio_id
LEFT JOIN mensagens m ON m.proposta_id = p.id
GROUP BY LEAST(p.proponente_id, a.usuario_id), GREATEST(p.proponente_id, a.usuario_id);

ALTER TABLE propostas ADD COLUMN conversa_id BIGINT NULL;
UPDATE propostas
SET conversa_id = (
    SELECT c.id
    FROM anuncios a
    JOIN conversas c ON c.participante_menor_id = LEAST(propostas.proponente_id, a.usuario_id)
                    AND c.participante_maior_id = GREATEST(propostas.proponente_id, a.usuario_id)
    WHERE a.id = propostas.anuncio_id
);
-- Carry the most recent read cursor from all the pair's legacy proposal conversations.
UPDATE conversas
SET ultima_lida_menor = COALESCE((
        SELECT MAX(CASE WHEN p.proponente_id < a.usuario_id
                        THEN p.ultima_lida_proponente ELSE p.ultima_lida_anunciante END)
        FROM propostas p JOIN anuncios a ON a.id = p.anuncio_id WHERE p.conversa_id = conversas.id
    ), 0),
    ultima_leitura_menor_em = (
        SELECT MAX(CASE WHEN p.proponente_id < a.usuario_id
                        THEN p.ultima_leitura_proponente_em ELSE p.ultima_leitura_anunciante_em END)
        FROM propostas p JOIN anuncios a ON a.id = p.anuncio_id WHERE p.conversa_id = conversas.id
    ),
    ultima_lida_maior = COALESCE((
        SELECT MAX(CASE WHEN p.proponente_id > a.usuario_id
                        THEN p.ultima_lida_proponente ELSE p.ultima_lida_anunciante END)
        FROM propostas p JOIN anuncios a ON a.id = p.anuncio_id WHERE p.conversa_id = conversas.id
    ), 0),
    ultima_leitura_maior_em = (
        SELECT MAX(CASE WHEN p.proponente_id > a.usuario_id
                        THEN p.ultima_leitura_proponente_em ELSE p.ultima_leitura_anunciante_em END)
        FROM propostas p JOIN anuncios a ON a.id = p.anuncio_id WHERE p.conversa_id = conversas.id
    );
ALTER TABLE propostas ADD CONSTRAINT fk_propostas_conversa FOREIGN KEY (conversa_id) REFERENCES conversas(id);
CREATE INDEX idx_propostas_conversa_status ON propostas(conversa_id, status, criado_em, id);

ALTER TABLE mensagens ADD COLUMN conversa_id BIGINT NULL;
ALTER TABLE mensagens ADD COLUMN anuncio_contexto_id BIGINT NULL;
UPDATE mensagens
SET conversa_id = (
        SELECT p.conversa_id FROM propostas p
        WHERE p.id = COALESCE(mensagens.proposta_id, (SELECT a.proposta_id FROM acordos a WHERE a.id = mensagens.acordo_id))
    ),
    anuncio_contexto_id = (
        SELECT p.anuncio_id FROM propostas p
        WHERE p.id = COALESCE(mensagens.proposta_id, (SELECT a.proposta_id FROM acordos a WHERE a.id = mensagens.acordo_id))
    )
WHERE COALESCE(proposta_id, (SELECT a.proposta_id FROM acordos a WHERE a.id = mensagens.acordo_id)) IS NOT NULL;
ALTER TABLE mensagens ADD CONSTRAINT fk_mensagens_conversa FOREIGN KEY (conversa_id) REFERENCES conversas(id);
ALTER TABLE mensagens ADD CONSTRAINT fk_mensagens_anuncio_contexto FOREIGN KEY (anuncio_contexto_id) REFERENCES anuncios(id);
CREATE INDEX idx_mensagens_conversa_data_id ON mensagens(conversa_id, enviado_em, id);
CREATE INDEX idx_mensagens_conversa_nao_lida ON mensagens(conversa_id, remetente_id, enviado_em, id);
