ALTER TABLE avaliacoes
    ADD COLUMN resposta_profissional VARCHAR(1000);

ALTER TABLE avaliacoes
    ADD COLUMN respondida_em DATETIME(6);
