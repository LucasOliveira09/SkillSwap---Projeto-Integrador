ALTER TABLE propostas
    ADD COLUMN servico VARCHAR(120) NULL;
ALTER TABLE propostas ADD COLUMN escopo TEXT NULL;
ALTER TABLE propostas ADD COLUMN forma_pagamento VARCHAR(20) NULL;
ALTER TABLE propostas ADD COLUMN valor DECIMAL(10, 2) NULL;
ALTER TABLE propostas ADD COLUMN parcelas INT NULL;
ALTER TABLE propostas ADD COLUMN duracao VARCHAR(120) NULL;
ALTER TABLE propostas ADD COLUMN validade_em DATE NULL;

ALTER TABLE propostas
    ADD CONSTRAINT ck_proposta_forma_pagamento
        CHECK (forma_pagamento IS NULL OR forma_pagamento IN ('A_VISTA', 'PARCELADO', 'TROCA'));
ALTER TABLE propostas ADD CONSTRAINT ck_proposta_parcelas
        CHECK (parcelas IS NULL OR parcelas BETWEEN 2 AND 12);
ALTER TABLE propostas ADD CONSTRAINT ck_proposta_termos_pagamento
        CHECK (
            (forma_pagamento IS NULL AND valor IS NULL AND parcelas IS NULL)
            OR (forma_pagamento = 'TROCA' AND valor IS NULL AND parcelas IS NULL)
            OR (forma_pagamento = 'A_VISTA' AND valor IS NOT NULL AND valor > 0 AND parcelas IS NULL)
            OR (forma_pagamento = 'PARCELADO' AND valor IS NOT NULL AND valor > 0
                AND parcelas IS NOT NULL AND parcelas BETWEEN 2 AND 12)
        );
