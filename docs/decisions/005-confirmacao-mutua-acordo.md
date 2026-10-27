# ADR-005: Exigir confirmação mútua para concluir acordos

Status: implementada em 26/10/2026; janela de contestação e fluxo de disputa continuam fora desta decisão.

## Contexto

Antes desta decisão, qualquer participante podia marcar o acordo como concluído. A avaliação era liberada imediatamente, embora a outra parte não tivesse confirmado que o serviço ou a troca aconteceu. Isso enfraquecia o vínculo entre reputação e operação real.

## Decisão

- Cada participante confirma a conclusão por `PATCH /api/acordos/{id}` com `{"status":"CONCLUIDO"}`.
- A primeira confirmação é persistida no respectivo campo e o acordo continua `EM_ANDAMENTO`; o cliente informa que falta a outra pessoa.
- A segunda confirmação conclui o acordo e registra `concluidoEm`. Avaliações continuam indisponíveis até essa transição.
- O servidor identifica o participante pelo JWT e bloqueia a linha do acordo durante confirmação/cancelamento para impedir perda de atualização em concorrência.
- As colunas de confirmação são anuláveis para preservar a incerteza em acordos anteriores à migração. Acordos novos começam com as duas confirmações falsas.
- Não há prazo de contestação nem moderação de divergência nesta entrega. Nenhuma confirmação libera ou processa pagamento.

## Consequências

- Avaliações representam uma conclusão reconhecida pelos dois participantes.
- Uma confirmação isolada não encerra o acordo; se a outra parte não responder, o acordo permanece em andamento. Notificações, prazo, contestação e resolução de disputa permanecem trabalho futuro.
- A migração Flyway V3 é aditiva e preserva os registros existentes.

