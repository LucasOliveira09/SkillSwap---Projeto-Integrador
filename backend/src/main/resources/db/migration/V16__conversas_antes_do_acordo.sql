ALTER TABLE mensagens MODIFY COLUMN acordo_id BIGINT NULL;
ALTER TABLE mensagens ADD COLUMN proposta_id BIGINT;
UPDATE mensagens SET proposta_id = (SELECT a.proposta_id FROM acordos a WHERE a.id = mensagens.acordo_id);
ALTER TABLE mensagens MODIFY COLUMN proposta_id BIGINT NOT NULL;
ALTER TABLE mensagens ADD CONSTRAINT fk_mensagens_proposta FOREIGN KEY (proposta_id) REFERENCES propostas(id);
CREATE INDEX idx_mensagens_conversa_enviada ON mensagens(proposta_id, enviado_em, id);
ALTER TABLE propostas ADD COLUMN ultima_lida_anunciante BIGINT NOT NULL DEFAULT 0;
ALTER TABLE propostas ADD COLUMN ultima_lida_proponente BIGINT NOT NULL DEFAULT 0;
ALTER TABLE propostas ADD COLUMN ultima_leitura_anunciante_em DATETIME(6);
ALTER TABLE propostas ADD COLUMN ultima_leitura_proponente_em DATETIME(6);
INSERT INTO mensagens (acordo_id, proposta_id, remetente_id, conteudo, enviado_em)
SELECT a.id, p.id, p.proponente_id, p.mensagem_inicial, p.criado_em
FROM propostas p LEFT JOIN acordos a ON a.proposta_id = p.id;
