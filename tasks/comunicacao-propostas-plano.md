# Plano: conversas por pessoa, propostas no chat e comentários do anúncio

Data: 30/10/2026
Estado: implementação concluída e suítes locais aprovadas. Os backups do banco e dos uploads foram validados; uma cópia isolada da base pública (MySQL 8.0.46, V17) foi restaurada para ensaio. Falta iniciar nela a API candidata, verificar preservação e publicar a versão autorizada.

## Objetivo confirmado

- Uma conversa persistente por par de usuários, sem criar uma conversa nova a cada anúncio.
- A ação “Entrar em contato” no anúncio deve abrir o chat com o anunciante; o primeiro texto é enviado dentro do chat e mantém o anúncio de origem como contexto.
- A página do anúncio deixa de conter o formulário de envio de mensagem. Terá uma seção/aba de comentários e avaliações.
- Propostas comerciais são criadas e respondidas dentro da conversa, vinculadas ao anúncio/serviço e ao histórico.
- A proposta pode descrever serviço/escopo, preço à vista ou parcelado e duração. Esses termos registram uma negociação; o SkillSwap não cobra, processa nem garante pagamento nesta fase.

## Diagnóstico que afeta a mudança

Hoje `/api/anuncios/{id}/conversas` cria ou reutiliza uma proposta ativa daquele usuário e anúncio, e o ID da conversa corresponde ao ID da proposta. `mensagens` aponta para proposta e opcionalmente acordo. Portanto, o mesmo par pode ter conversas diferentes se falar por anúncios diferentes. A tela do anúncio ainda tem formulário de primeiro contato. Não existe modelo/API de comentários. Avaliações verificadas já existem: são associadas a acordo concluído, têm nota do profissional e da oferta, ocultação por moderação e resposta pública do profissional.

A alteração muda o agregado central e a chave de identidade das conversas. Não é uma tarefa apenas de navegação: exige uma relação persistida única por par, atualização dos contratos, adaptação dos consumidores e migração histórica. Não se deve simplesmente deduplicar ou apagar propostas, acordos ou mensagens.

## Contrato de domínio proposto

### Conversa

- Uma conversa tem os dois participantes e uma chave canônica única `(menor_usuario_id, maior_usuario_id)`, independente de quem iniciou o contato. Auto-conversa é inválida. A restrição única no MySQL resolve corridas de criação; serviço retorna a conversa existente se dois contatos simultâneos tentarem criá-la.
- Criar/obter conversa recebe destinatário e, opcionalmente, anúncio de origem. O servidor deriva o remetente da sessão, valida o destinatário e confere que o anúncio está público/ativo, que pertence ao destinatário e que não é exemplo.
- Anúncio é contexto da mensagem inicial ou de uma proposta, não identidade da conversa. O mesmo chat pode tratar outro anúncio sem gerar outra conversa; cada mensagem/proposta guarda seu próprio contexto quando aplicável.
- Lista, detalhe, mensagens, resumo de não lidas e marcação de leitura ficam autorizados somente aos dois participantes. Cada conversa tem um cursor de leitura por participante.
- A lista preserva notificação, atualização periódica e contexto. Quando há mais de um anúncio no histórico, deve indicar claramente o contexto da mensagem/proposta selecionada, sem declarar um contexto único falso.
- Contato iniciado pelo perfil público pode abrir conversa sem anúncio; a pessoa poderá escolher uma oferta dentro do chat para formalizar proposta. Contato iniciado pelo anúncio já entra no mesmo chat com o anúncio indicado.

Rotas candidatas (contrato precisa ser fechado antes de codificar):

| Método e rota | Uso esperado |
| --- | --- |
| `POST /api/conversas` | Criar ou obter o chat do usuário autenticado com o destinatário, com `anuncioId` opcional. Retorna `201 Created` para um chat criado e `200 OK` para um já existente; uma repetição não cria mensagem duplicada. O primeiro texto é enviado depois pelo endpoint de mensagens. |
| `GET /api/conversas` | Caixa de entrada paginada, uma linha por par. |
| `GET /api/conversas/{id}` | Contexto e participantes públicos mínimos, autorizado por participação. |
| `GET /api/conversas/{id}/mensagens` | Histórico cronológico paginado. |
| `POST /api/conversas/{id}/mensagens` | Enviar texto, com referência opcional ao anúncio atual. |
| `PATCH /api/conversas/{id}/leitura` | Atualizar cursor monotônico do participante. |

Nenhuma dessas alterações deve expor e-mail, sessão, dados de pagamento ou mensagens em perfil/anúncio público.

### Propostas comerciais no chat

- Uma proposta é um recurso persistido e privado aos participantes, sempre ligada a uma conversa e a um anúncio ativo do profissional. O cliente ou profissional pode iniciar uma proposta ou contraproposta.
- Campos previstos: autor, destinatário derivado, anúncio/serviço, descrição do escopo, modalidade, valor total quando houver, forma/condição combinada (`AVISTA`, `PARCELADO` ou `A_COMBINAR`), quantidade de parcelas quando parcelado, duração/período, validade e estado.
- Valor da parcela deve ser derivado do total e quantidade, com regra explícita para centavos residuais; não aceitar valores negativos, moeda arbitrária, parcela sem quantidade nem conteúdo fora dos limites. Limite máximo de parcelas e unidade de duração devem ser decididos antes do DTO.
- Para troca, preço e habilidade/oferta de troca precisam respeitar modalidade; aceitar dinheiro complementar numa troca fica em aberto. Não simular equivalência monetária.
- Estados propostos: `PENDENTE`, `ACEITA`, `RECUSADA`, `RETIRADA`, `EXPIRADA` e `SUBSTITUIDA`. Somente o destinatário aceita/recusa; somente o autor retira; contraproposta cria novo registro ligado ao anterior. Aceitação concorrente é serializada no backend.
- Aceitação cria o acordo existente e associa seu histórico à conversa. Avaliação continua bloqueada até conclusão mútua do acordo.
- Os valores e condições ficam como registro do que foi acordado entre pessoas. Não haverá captura, cobrança, parcelamento financeiro, PIX, cartão, custódia, garantia ou liberação de valores sem decisão e integração financeira próprias.

Rotas candidatas:

| Método e rota | Uso esperado |
| --- | --- |
| `POST /api/conversas/{id}/propostas` | Criar uma proposta ou contraproposta dentro do chat. |
| `GET /api/conversas/{id}/propostas` | Listar propostas da conversa e estados. |
| `GET /api/propostas/{id}` | Consultar proposta para os participantes. |
| `PATCH /api/propostas/{id}` | Atualizar estado por destinatário/autor conforme transições permitidas. Os termos ficam imutáveis; uma contraproposta cria um novo recurso ligado à proposta anterior. |

Revisar os nomes do contrato com as convenções REST do projeto antes de fixá-los. Não aceitar usuário, destinatário, proprietário do serviço ou estado privilegiado apenas com base no corpo enviado pelo navegador.

### Comentários e avaliações no anúncio

- A área “Comentários e avaliações” separa perguntas/comentários públicos de avaliações verificadas. Comentários não recebem estrelas nem selo de contratação; avaliações seguem o fluxo atual e continuam disponíveis só após acordo concluído.
- A média de avaliação do anúncio continua baseada em `notaAnuncio` visível de trabalhos concluídos e não inclui comentários.
- Comentários têm leitura pública, autoria autenticada, até 1.000 caracteres, paginação e moderação. Autor e dono do anúncio podem responder. Aplicar denúncia e controles antiabuso existentes.
- A transição deve preservar o resumo atual de avaliação no anúncio e não criar comentários/avaliações sintéticos.

## Migração e compatibilidade histórica

1. Criar entidade/tabela de conversa e restrição única do par, com migration Flyway aditiva.
2. Criar conversa canônica para cada par encontrado em propostas existentes; normalizar a ordenação de IDs e mesclar todos os anúncios daquele par no mesmo chat.
3. Ligar cada proposta antiga à nova conversa, preservando as propostas, estados e acordos. Proposta existente continua consultável pelo ID antigo.
4. Associar todas as mensagens à conversa sem mudar seus IDs, remetente, texto ou data. Preservar `proposta_id` e `acordo_id` para explicar a origem comercial e manter as avaliações ligadas aos acordos originais.
5. Consolidar cursores de leitura por participante usando data e ID monotônicos; não zerar não lidas silenciosamente.
6. Manter links antigos (`?acordo=` e `?conversa=<id de proposta>`) redirecionáveis à conversa canônica por um período de compatibilidade.
7. Validar em cópia isolada MySQL 8.0.46/8.4.11: pares únicos, contagens e hashes das mensagens anteriores, vínculos de acordo/avaliação, cursores e reexecução sem duplicatas. Nenhuma migração destrutiva ou remoção de volume.

Risco mais alto: vários históricos e cursores antigos convergirem para o mesmo par. O ensaio deve provar preservação antes de qualquer promoção. Atualizar API e frontend em sequência compatível ou janela coordenada; versões antigas podem depender de IDs de proposta como IDs de conversa.

## Entrega incremental proposta

### C0 — Fechar contrato e decisões

Publicar ADR com modelo de conversa única, contratos REST, transições de proposta, compatibilidade e política de comentários. Critério: nenhuma ambiguidade em como iniciar chat, ligar anúncio, responder/counterpropor ou marcar leitura.

### C1 — Persistir conversa por par e migrar histórico

Migration aditiva, entidades, repositórios e serviço para criar/reutilizar um chat único, migrar mensagens/propostas existentes e preservar autorização. Cobrir criação concorrente, autorizações negativas, dados históricos, IDs antigos e idempotência.

### C2 — API e mensageiro com conversas canônicas

Atualizar caixa de entrada, leitura, notificação, indicador, polling, URLs, acordo e links de conta para os IDs canônicos. Mostrar contexto de anúncio por mensagem/proposta. Testar dois anúncios entre o mesmo par continuam no mesmo chat.

### C3 — Contato inicia/resgata chat

Trocar o formulário de mensagem do anúncio por botão “Entrar em contato”; após sessão, o servidor cria/retorna o chat do par e o site navega para esse chat. Anúncio entra como contexto pré-selecionado; o texto é escrito e enviado no chat. Visitante vai ao login e retorna ao anúncio/ação. Do perfil, o contato abre o mesmo chat sem anunciar contexto inexistente.

### C4 — Propostas e contrapropostas na conversa

Adicionar UI de cartão de proposta no histórico, formulário de termos, revisão antes de enviar, estados, aceite/recusa/retirada e conversão a acordo. Revalidar propriedade/status da oferta, valores e transições no servidor. Não processar pagamentos.

### C5 — Comentários do anúncio e área de avaliações

Definir regra de publicação/moderação e contrato; criar migração/API/testes; mostrar comentários e avaliações em áreas visualmente separadas no anúncio. O botão do anúncio permanece “Entrar em contato” e leva ao chat; nenhum formulário de conversa fica na página do anúncio.

## Critérios de aceite globais

- Uma única linha/conversa por par de pessoas mesmo ao entrar por diferentes anúncios ou simultaneamente.
- Uma mensagem do anúncio aparece no chat canônico com origem correta; envio, recebimento, leitura, notificações e histórico anterior persistem.
- Só participantes consultam chat/propostas; comentários públicos não revelam e-mail nem dados do chat.
- Oferta de proposta validada no servidor e as transições são legais, atômicas e auditáveis; o acordo existente nasce somente da aceitação válida.
- Valores e parcelas são aritmeticamente consistentes e exibidos sem sugerir que o pagamento foi processado ou garantido pelo SkillSwap.
- Comentário não vira avaliação; reputação continua baseada em acordos concluídos e avaliações não ocultadas.
- Migração não perde mensagem, acordo, avaliação ou data; rollout possui backup, verificação e rollback documentados.
- Cobertura de testes backend, Angular, MySQL real em schema isolado e Chromium responsivo antes de publicação. Não publicar sem solicitação expressa.

## Decisões abertas com recomendação

1. Conversa única vale também entre contas com papéis distintos (cliente e profissional)? **Recomendação:** sim, chave canônica global por par.
2. Conversa iniciada de perfil sem anúncio pode começar sem texto e mostrar estado vazio? **Recomendação:** abrir chat existente ou novo vazio; primeiro envio é feito pelo chat.
3. Quem pode propor: ambos os participantes? **Recomendação:** sim, sempre referenciando oferta ativa; contraproposta é novo registro e conserva versões.
4. **Decidido:** no máximo 12 parcelas; a proposta deve indicar os termos integralmente, sem sugerir processamento ou garantia de pagamento.
5. **Decidido:** anúncio de troca não aceita complemento em dinheiro.
6. **Decidido:** comentários públicos, escrita autenticada e respostas do autor ou anunciante, com denúncia/moderação.
7. **Decidido:** visitantes podem ler comentários; somente pessoas autenticadas podem publicar.
8. Rollout do banco público dependerá de solicitação e ensaio com cópia/rollback; esta tarefa não concede autorização para deploy.

## Estado de implementação (30/10/2026)

- **V19 — conversas canônicas:** migration aditiva cria uma conversa por par, associa o histórico de propostas/mensagens e mantém rotas legadas resolvíveis. Serviços/lista e DTOs usam o ID canônico.
- **Contato:** o anúncio abre/reutiliza o chat sem enviar texto fora dele; o chat começa com o anúncio de origem e cada mensagem enviada recebe contexto. O perfil público abre o mesmo chat sem anúncio inicial.
- **V20 — termos de proposta:** campos persistidos para serviço, escopo, forma de pagamento, valor, parcelas, duração e validade. `POST /api/conversas/{id}/propostas` cria a proposta, grava uma mensagem com referência e exige que o anúncio ativo pertença ao outro participante. Parcelas aceitas de 2 a 12; venda à vista/parcelada exige valor positivo; anúncio de troca rejeita qualquer valor ou parcela.
- **Cartão no chat:** proposta mostra termos e estado, e permite ao destinatário aceitar/recusar ou ao autor cancelar. A aceitação passa pelo fluxo existente que cria acordo; pagamento não é processado.
- **V21 — comentários:** leitura pública paginada e escrita autenticada; respostas de uma camada são autorizadas para o autor do comentário raiz ou dono do anúncio. O texto é persistido como texto plano e renderizado por interpolação. A listagem respeita visibilidade pública do anúncio e usa nome genérico para autor com perfil privado.
- **Avaliações do anúncio:** endpoint público lista somente notas associadas a acordos e não ocultadas por moderação; comentários aparecem separados e não alteram a média.
- **Moderação e experiência:** comentários podem ser denunciados e ocultados/reexibidos pela fila de moderação existente; a listagem não mostra comentários ocultos nem respostas sem raiz visível. O cliente carrega páginas seguintes de comentários. No chat iniciado pelo perfil, a pessoa pode escolher um anúncio ativo do interlocutor antes de enviar proposta; validade vencida desativa aceite/recusa e o servidor rejeita o aceite.
- **Validação local:** `npm run typecheck`, `npm run build`, `npm run test:ci` (121 testes) e `./mvnw.cmd test` (92 testes) passaram; `git diff --check` está limpo. Migrações e fluxo já passaram em MySQL 8.4.11 descartável com dados sintéticos.
- **Backup pré-publicação:** dump MySQL 8.0 público com checksum e arquivo de uploads conferidos em `/home/ubuntu/backups/skillswap-comunicacao-pre-20261030`; o upload público é uma montagem bind e foi arquivado sem parar ou alterar a API.
- **Ensaio pendente:** cópia do dump V17 restaurada em MySQL 8.0.46 num container e rede internos separados. Ainda falta iniciar a API candidata nessa cópia, comparar preservação de mensagens/propostas/acordos/avaliações/cursos e validar o frontend em navegador responsivo antes de promover as imagens.
