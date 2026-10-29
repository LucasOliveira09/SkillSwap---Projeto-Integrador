# ADR-007: Entregar contatos no chat antes do aceite da proposta

Status: implementada em 28/10/2026 a pedido do usuário.

## Problema observado

O botão “Enviar mensagem” no anúncio criava uma proposta pendente. A caixa de mensagens listava somente acordos aceitos e a mensagem inicial não era um item do histórico. Assim, o primeiro contato não aparecia no chat do anunciante e não podia ser respondido ali.

## Decisão

- Expor o recurso `/api/conversas` a partir do interesse/proposta, mantendo uma conversa para cada proposta e identificando explicitamente anúncio, anunciante e interessado. O ID da conversa corresponde ao ID da proposta, sem criar acordo automaticamente.
- `POST /api/anuncios/{id}/conversas` recebe o primeiro texto de até 500 caracteres, cria interesse pendente e primeira mensagem na mesma transação; responde 201 e Location. Se já existe uma proposta pendente/aceita para esse interessado e anúncio, reutiliza sua conversa, grava a mensagem e responde 200. Anúncios ilustrativos, indisponíveis e o próprio anúncio não recebem contato.
- Ambos os participantes podem responder antes do aceite, com mensagens de até 2000 caracteres. Recusa/cancelamento e acordo encerrado mantêm o histórico para leitura, sem novo envio. O aceite continua disponível na conta e cria o acordo conforme as regras anteriores.
- Cada mensagem pertence à proposta; o acordo é opcional até o aceite. A V16 vincula o histórico existente, cria os campos de leitura e recupera a mensagem inicial de todas as propostas, preservando os IDs/conteúdos/datas das mensagens anteriores. O aceite associa ao acordo também as mensagens anteriores ao aceite.
- A V17 permite vínculo de proposta nulo no banco para uma eventual reversão da API: imagens antigas continuam gravando mensagens em acordos. Ao iniciar a API atual, um reconciliador idempotente deriva o vínculo pela FK do acordo; não inventa participantes. O fluxo atual sempre grava o vínculo da proposta.
- URLs anteriores `/mensagens?acordo=...` continuam resolvidas com autorização pela API; a URL atual usa `conversa=...`. Rotas anteriores de mensagens por acordo compartilham o mesmo histórico.

## Entrega, contexto e leitura

- A API deriva destinatário e anúncio no servidor. Clientes não escolhem remetente nem destinatário no corpo. Toda leitura, envio, resumo e confirmação exige JWT e participação; nenhum e-mail ou conteúdo privado é público.
- O histórico e a lista mostram anúncio e interlocutor, última mensagem, contagem real de não lidas e acesso à oferta. A notificação persistida identifica o título do anúncio e leva à conversa correspondente.
- O contexto fixo do chat informa que o contato começou pelo anúncio, com ID/título, modalidade e cidade. Imagem é omitida quando a oferta/perfil deixa de ser público, respeitando a política de uploads.
- Cursores de leitura combinam data e ID da mensagem, são monotônicos e só aceitam mensagem pertencente à conversa. Isso trata mensagens iniciais migradas cujo ID é maior que o de respostas antigas, embora sua data seja anterior.
- Lista, mensagens recentes e indicador no cabeçalho são atualizados periodicamente a cada oito segundos com a página visível. A busca abrange conversas carregadas; pode carregar páginas adicionais. Falha de atualização preserva o histórico e oferece nova tentativa.
- Sucesso significa persistência confirmada no servidor, sem afirmar que o destinatário leu. A contagem de não lidas é para o próprio usuário; não há confirmação de leitura ao remetente, presença online, e-mail ou push fora do site nesta entrega.

## Verificações

83 testes Java (envio/resposta antes do aceite, autorização negativa, reutilização, leitura, recuperação de legado e migrações), 117 Angular e verificação do fluxo em Chromium com MySQL 8.4.11. O teste de migração prepara V15, inclui conversa aceita e contato pendente, aplica V16/V17 e confere IDs/conteúdo/datas e reexecução sem duplicar. O cenário entre duas contas confirmou envio pelo anúncio, aviso de não lida, contexto de origem, resposta e leitura pelo cliente com a proposta ainda pendente.
