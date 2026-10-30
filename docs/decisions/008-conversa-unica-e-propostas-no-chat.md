# ADR-008: Conversa única por pessoa, propostas no chat e comentários do anúncio

Status: aprovada pelo usuário em 30/10/2026; implementação concluída e suítes completas locais aprovadas. Migration e API já passaram em MySQL 8.4.11 isolado com dados sintéticos. Uma cópia V17 do banco público está isolada em MySQL 8.0.46 para o ensaio final antes da publicação autorizada.

## Contexto

A implementação anterior usa uma proposta como identidade da conversa. Isso duplica a caixa de entrada quando o mesmo par conversa sobre anúncios diferentes e mistura o primeiro contato com negociação comercial. O anúncio também mantém um formulário de mensagem que desvia do chat.

## Decisão

- Uma conversa persistente por par global de usuários, independentemente de papel ou anúncio. A chave canônica ordena os IDs e o banco impõe unicidade; auto-conversa é inválida.
- Contato pelo anúncio abre ou reutiliza a conversa com o anunciante e leva o anúncio como contexto. O texto é enviado na página de chat. Contato pelo perfil abre o mesmo chat sem inventar um anúncio.
- Conversas, mensagens e propostas são recursos diferentes. Propostas e mensagens guardam seus contextos comerciais e acordos; IDs históricos permanecem consultáveis e URLs antigas são adaptadas enquanto durarem os consumidores legados.
- A proposta comercial pode registrar serviço/escopo, valor à vista ou parcelado, quantidade de parcelas de 2 a 12, duração e validade. Uma troca não aceita complemento em dinheiro. Termos não significam pagamento processado, custódia ou garantia.
- Comentários do anúncio podem ser lidos publicamente; escrever exige sessão. O autor do comentário e o anunciante podem responder. Comentários não são avaliações nem alteram reputação, que continua baseada em avaliações válidas de acordos concluídos.
- Comentários têm paginação, podem ser denunciados e ocultados/reexibidos pela moderação existente; avaliação do serviço permanece uma seção independente.
- Preservar anúncio/serviço, mensagens, propostas, acordos, avaliações e cursores de leitura existentes. Mudanças de schema são aditivas e devem passar por ensaio isolado MySQL antes de qualquer banco ativo.
- Pagamentos, vídeo, presença online e notificações externas ficam fora desta decisão.

## Consequências

- A API precisa de uma entidade `Conversa`, autorização por participante e adaptação do feed/lista para o novo ID canônico.
- Migrações devem relacionar propostas e mensagens históricas à conversa sem alterar seus IDs ou conteúdo. Cursores e contagens de não lidas precisam ser comparados antes/depois.
- Rotas antigas de contato, acordo e mensagem precisam resolver a conversa canônica sem expor recursos privados.
- Cada fatia deve ser validada com testes backend/frontend e MySQL isolado. Publicação somente após backup verificado e verificação de compatibilidade do frontend/API.
