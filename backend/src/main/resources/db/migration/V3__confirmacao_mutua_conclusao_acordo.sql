ALTER TABLE acordos
    ADD COLUMN conclusao_confirmada_anunciante BOOLEAN NULL;

ALTER TABLE acordos
    ADD COLUMN conclusao_confirmada_proponente BOOLEAN NULL;
