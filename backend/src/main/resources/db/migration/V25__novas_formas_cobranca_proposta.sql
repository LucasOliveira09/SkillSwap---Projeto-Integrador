ALTER TABLE propostas DROP CONSTRAINT ck_proposta_forma_pagamento;
ALTER TABLE propostas DROP CONSTRAINT ck_proposta_termos_pagamento;
ALTER TABLE propostas ADD CONSTRAINT ck_proposta_forma_pagamento
    CHECK (forma_pagamento IS NULL OR forma_pagamento IN ('A_VISTA', 'PARCELADO', 'TROCA', 'POR_HORA', 'POR_DIA', 'ASSINATURA'));
ALTER TABLE propostas ADD CONSTRAINT ck_proposta_termos_pagamento
    CHECK (
        (forma_pagamento IS NULL AND valor IS NULL AND parcelas IS NULL)
        OR (forma_pagamento = 'TROCA' AND valor IS NULL AND parcelas IS NULL)
        OR (forma_pagamento IN ('A_VISTA', 'POR_HORA', 'POR_DIA', 'ASSINATURA') AND valor IS NOT NULL AND valor > 0 AND parcelas IS NULL)
        OR (forma_pagamento = 'PARCELADO' AND valor IS NOT NULL AND valor > 0 AND parcelas IS NOT NULL AND parcelas BETWEEN 2 AND 12)
    );
