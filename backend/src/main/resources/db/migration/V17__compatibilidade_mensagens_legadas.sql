-- Permite que uma reversão para a API anterior continue gravando mensagens de acordos.
-- Ao reiniciar a API atual, o reconciliador preenche o vínculo pela FK do acordo.
ALTER TABLE mensagens MODIFY COLUMN proposta_id BIGINT NULL;
