# Plano: serviços parametrizados e propostas no chat

**Estado:** planejamento refinado; as funcionalidades de anúncio parametrizado e negociação descritas aqui ainda não foram implementadas. A limpeza de anúncios demonstrativos já feita na VPS está registrada em `docs/backend.md`.

Em 30/10/2026, a navegação por fragmento foi corrigida localmente nos atalhos do perfil, anúncio e catálogo, e o CTA de conversa foi ocultado no perfil próprio autenticado. A alteração não foi publicada na VPS; veja a situação da produção em `docs/backend.md`.

## O que encontrei

- O perfil próprio (`/meu-perfil`) não tem seletor de categorias. O campo “Especialidades” é texto livre; categorias pertencem aos anúncios. O perfil público lista os anúncios ativos do profissional e oferece links entre Sobre, Experiência, Portfólio, Serviços e Avaliações.
- O redirecionamento foi reproduzido em Chromium 149: na página `/profissionais/1`, clicar em “Serviços” mudou a URL para `https://skillswap.vizihub.com.br/#secao-servicos`. Os links de seção usam `href="#secao-..."`; com `<base href="/">`, o navegador resolve o fragmento na raiz do site, e o Angular abre o catálogo. Os outros links da mesma navegação usam a mesma forma e podem sofrer o mesmo desvio. Isso explica a navegação do perfil próprio quando aberto publicamente e a dos perfis de outras pessoas.
- A criação/edição de anúncios usa as três categorias fixas do enum Java `CategoriaAnuncio`: manutenção, aulas e transporte. Não existe tipo de serviço nem configuração de parâmetros, unidades ou cálculo.
- Editar um anúncio termina em `/anuncios/minhas`, que é uma rota existente. O formulário do perfil salva por `PATCH /api/perfil` sem navegar. A falha confirmada ocorre nos atalhos da página pública do perfil; o editor `/meu-perfil` não contém seletor de categoria. A busca de profissionais tem filtros próprios na rota `/profissionais`.
- No chat (`/mensagens`), “Enviar proposta no chat” já busca anúncios ativos do outro participante e envia para `POST /api/conversas/{id}/propostas`, mas o formulário aparece dentro da conversa, não em um modal. Só há serviço, escopo, valor, pagamento, parcelas, duração e validade; nenhum campo vem da configuração do anúncio e o total não é calculado a partir de unidades.
- A proposta atual persiste um único valor e os termos gerais. O cartão da proposta no chat não mostra itens calculados nem parâmetros estruturados. Uma proposta enviada é aceita, recusada ou cancelada; ajustes podem ser registrados por outra proposta na mesma conversa.
- As categorias persistidas são enum/string e os registros já publicados estão no MySQL público V24. A evolução deve ser aditiva, manter anúncios e propostas antigos legíveis e não alterar o fluxo financeiro. Pagamentos continuam fora da plataforma.
- Há textos genéricos de apresentação no chat. A revisão visual deve substituir slogans e instruções vagas por rótulos diretos, explicando o que será informado e como o valor foi calculado, preservando a identidade antiga azul/verde.

Arquivos que representam os fluxos atuais: `src/app/features/perfil/pages/meu-perfil/`, `src/app/features/anuncios/pages/cadastro-anuncio/`, `src/app/features/anuncios/anuncios.routes.ts`, `src/app/features/negociacao/pages/caixa-mensagens/`, `backend/src/main/java/com/skillswap/api/domain/anuncio/`, `backend/src/main/java/com/skillswap/api/domain/proposta/` e `docs/backend.md`.

## Regra central do produto

**O anúncio é a ficha completa de como o serviço funciona e como é cobrado. A proposta não cria nem redefine o serviço: ela preenche a ficha para um atendimento específico.** O anunciante configura uma vez as regras e os limites da oferta. No chat, qualquer participante da conversa pode iniciar uma proposta escolhendo uma oferta dos participantes e preenchendo apenas os campos que essa oferta permite. A outra pessoa aceita, recusa ou envia uma contraproposta baseada na mesma configuração.

### O que o anunciante configura antes de publicar

1. **Identidade do serviço:** categoria para busca, tipo de serviço, título, descrição, local/região atendida, o que está incluído, o que não está incluído e condições de agendamento. O catálogo sugere categorias e campos; o anunciante completa os detalhes da própria oferta.
2. **Como o preço é formado (modelo de cobrança):** preço único por serviço/pacote, por hora, por quilômetro, por unidade/sessão/diária, composição de itens ou “orçamento após avaliação”. Não confundir essa regra de cálculo com a forma e o momento do pagamento.
3. **Tarifas e limites:** valor por unidade, tarifa inicial, mínimo cobrado, quantidade mínima/máxima, duração incluída, adicionais e arredondamento. O anunciante define somente itens compatíveis com o modelo escolhido. A fórmula permitida é soma de componentes e tarifa × quantidade; nunca uma expressão ou código livre.
4. **Dados necessários para cada pedido:** parâmetros tipados e configurados pelo anunciante, como distância, horas, número de pessoas, quantidade de aulas, data, endereço aproximado ou opções de serviço. Para cada campo, define rótulo, tipo, unidade, obrigatoriedade, limites e opções permitidas. O cliente não pode acrescentar campos nem editar tarifas na proposta.
5. **Como o pagamento pode ser combinado:** opções aceitas (à vista ou parcelado em até 12 vezes, quando permitido), métodos aceitos pelo profissional e momento previsto (antecipado, por etapa ou após a entrega). Estes são termos declarados no anúncio; a plataforma não coleta, processa, retém nem confirma pagamentos nesta fase. Em troca, a oferta informa a habilidade desejada e não permite complemento em dinheiro.
6. **Margem de negociação, se houver:** o anunciante pode declarar se aceita contraproposta e que partes podem variar. A tarifa-base não muda em silêncio. Para cobrança calculável, uma contraproposta recalcula quantidades ou escolhe outra condição já permitida; um preço manual só existe se a oferta configurar explicitamente “orçamento após avaliação” ou uma margem/valor negociável.

O catálogo começa administrado e extensível, sem enum Java para cada novo tipo. “Outro serviço” pode usar título livre dentro de uma categoria existente. Os templates sugerem campos, mas os campos/tarifas efetivamente aceitos são persistidos na versão do anúncio e revisados pelo anunciante antes da publicação.

### O que a pessoa preenche na proposta

- Seleciona uma oferta participante da conversa e vê, em contexto, o profissional, o serviço e as condições que já foram publicados.
- Informa apenas os dados daquele atendimento: por exemplo, km estimados, horas, quantidade, data e opções definidas no anúncio.
- Escolhe somente condições de pagamento que o anúncio permite. A interface separa claramente **modelo de cobrança** (por km/hora/fixo) de **condição de pagamento** (à vista/parcelado e quando pagar).
- Confere a memória de cálculo e o total estimado antes de enviar. Se o modelo depender de vistoria, envia um pedido de orçamento sem exibir um total fictício.

O backend valida todos os campos contra a versão do anúncio e calcula com precisão decimal. A tela pode pré-visualizar o cálculo, mas o valor devolvido pelo servidor é a referência. O anúncio continua sendo a fonte das regras; proposta e acordo guardam snapshots para que futuras edições não mudem o que foi negociado.

### Aceite e contraproposta no chat

- O anúncio abre ou reutiliza a conversa canônica do par. Não se cria outra conversa para negociar outro serviço com a mesma pessoa.
- Ambos os participantes podem iniciar uma proposta na conversa. Cada proposta identifica autor, destinatário e oferta; o serviço selecionado pode pertencer a qualquer um dos participantes, nunca a uma terceira pessoa.
- Quem recebe pode aceitar, recusar ou responder com contraproposta. A contraproposta referencia a anterior e usa a mesma definição de campos, tarifas, opções de pagamento e limites do anúncio; o autor vê lado a lado o que mudou.
- Recomendação para evitar duas condições concorrentes: uma proposta pendente por oferta/negociação. Ao enviar uma contraproposta, a anterior passa a “substituída”, continua no histórico e deixa de poder ser aceita; a nova fica pendente. Um aceite gera o acordo a partir do snapshot aceito.
- Para um anúncio “orçamento após avaliação”, o primeiro envio solicita o orçamento; o profissional informa a cotação e os termos permitidos no fluxo da conversa. Só depois do aceite há acordo. Nenhum status de proposta significa que houve pagamento.

### Exemplos de ponta a ponta

| Serviço e configuração do anúncio | Campos da proposta | Cálculo/resultado |
| --- | --- | --- |
| Corrida/táxi: tarifa inicial + R$/km, mínimo de cobrança, região e horários; métodos e condições de pagamento aceitos | Origem/destino ou distância estimada, data e horário | Tarifa inicial + km × tarifa/km, respeitando o mínimo; estimativa identificada como tal |
| Mudança: R$/km + R$/hora de equipe, mínimo de horas, quantidade de ajudantes e taxa de deslocamento | Distância, horas previstas, tamanho da equipe, andar/elevador, data; ou pedido de vistoria | Soma dos componentes configurados; se volume/distância só puder ser medido no local, retorna “aguardando orçamento” sem inventar valor |
| Aulas: preço por hora/sessão ou pacote, duração e limite de alunos | Número de sessões, duração, quantidade de alunos e datas desejadas | Tarifa × unidades configuradas, com total e duração apresentados antes do envio |

**Exemplo numérico ilustrativo — corrida:** no anúncio, o profissional define R$ 5 de tarifa inicial, R$ 3/km, mínimo de R$ 15 e Pix após a corrida. O cliente informa rota estimada de 10 km e horário; a API calcula R$ 5 + (10 × R$ 3) = R$ 35. O profissional aceita ou contrapropõe, por exemplo ajustando a distância para 12 km quando a rota justifica e a regra do anúncio permite; a API recalcula para R$ 41. O cliente aceita ou envia outra contraproposta. O aceite registra os termos e o cálculo; o SkillSwap não recebe nem confirma o Pix.

### Revisão antes de cada ação

- **Revisão do anúncio:** antes de publicar, mostra serviço, escopo, unidades, cálculo com exemplo, tarifas, limites, condições de pagamento e campos que o cliente terá de preencher.
- **Revisão da proposta:** antes de enviar, mostra as respostas, itens calculados, total/estimativa, condição de pagamento escolhida, contraparte e anúncio de origem. A pessoa confirma explicitamente o envio.
- Os rótulos devem explicar consequências concretas (“R$ 80 por hora”, “mínimo de 2 horas”, “estimativa baseada em 12 km”), sem slogans ou texto promocional genérico.

## Contrato e dados candidatos

O contrato deve ser fechado antes da implementação, após revisar todas as consultas/DTOs atuais. Proposta REST compatível:

- `GET /api/categorias` e novo recurso paginado `GET /api/tipos-servico?categoria=...` retornam catálogo persistido. Manter os códigos atuais de categorias durante a transição.
- `POST /api/anuncios` e `PUT /api/anuncios/{id}` recebem a definição do serviço completa: tipo, escopo, regras de cobrança, tarifas, limites, parâmetros que serão preenchidos na proposta e opções de pagamento. O backend valida e versiona a definição. A leitura do anúncio fornece a mesma configuração ao cliente para exibição e preenchimento.
- `POST /api/conversas/{id}/propostas` continua sendo a criação canônica. O corpo recebe somente `anuncioId`, versão da configuração, respostas aos campos publicados, condição de pagamento permitida e, em uma contraproposta, a referência à proposta anterior. Não recebe fórmula nem tarifa arbitrária. Qualquer participante pode criar a proposta com um anúncio ativo de um dos dois participantes; anúncio de terceiros é recusado. O servidor valida configuração, unidades, limites, opções de pagamento e autorização, calcula o valor e responde `201 Created` com itens, memória do cálculo e snapshot.
- `GET /api/propostas/{id}` e as mensagens da conversa devolvem autor/destinatário, anúncio de origem, respostas, pagamento, snapshot do cálculo e vínculo de contraproposta. Os status indicam pendente, aceita, recusada, cancelada ou substituída; não indicam pagamento realizado. O acordo deriva do snapshot aceito.
- O modelo precisa persistir uma versão da definição do anúncio e um snapshot imutável por proposta. Catálogo relacional, definições tipadas de campos/tarifas e snapshots em tabelas ou JSON validado são opções a comparar na auditoria de consultas MySQL/H2; o Angular nunca envia nem executa definição de fórmula.

Migrações Flyway futuras devem ser apenas aditivas (V25 em diante), com configuração opcional/versionada para anúncios existentes. Não renomear nem remover colunas usadas por propostas, acordos e mensagens. Mapear as três categorias antigas para o novo catálogo sem reclassificar silenciosamente os anúncios. Uma oferta antiga sem regras configuradas permanece no fluxo simples atual até que o proprietário a edite; o backend nunca inventa tarifas ou condições retroativamente.

## Entrega por incrementos

### 0. Navegação corrigida localmente; decisões de produto registradas

- [x] Substituir fragmentos relativos à raiz por `RouterLink` na rota atual do perfil, anúncio e catálogo; habilitar a rolagem Angular e preservar query params onde há paginação.
- [x] Cobrir todos os atalhos do perfil público (Sobre, Experiência, Portfólio, Serviços e Avaliações), mais comentários/avaliações do anúncio e a chamada do hero do catálogo.
- [x] Ocultar o CTA de conversa no perfil próprio autenticado, usando `proprioPerfil` devolvido pela API.
- A correção está apenas na branch local/GitHub e não foi promovida ao site público. A publicação depende da aprovação explícita exigida para produção.
- A revisão solicitada inclui tanto o anúncio antes de publicar quanto a proposta antes de enviar.
- Ambos podem iniciar propostas na mesma conversa. A configuração pertence ao anúncio; toda proposta apenas preenche os campos aceitos por essa configuração.
- Contraproposta responde à última proposta pendente daquele anúncio e a substitui, mantendo a anterior no histórico. Essa regra evita dois totais incompatíveis aguardando aceite ao mesmo tempo.
- O catálogo inicial é administrado, com título livre para “Outro serviço” dentro de categoria válida. O anunciante personaliza o funcionamento e os campos da própria oferta, não cria novas categorias globais.

### 1. Catálogo e configuração de oferta

- Criar catálogo de categorias/tipos e sugestões de parâmetros por tipo.
- No formulário de anúncio, configurar o serviço completo: escopo/inclusões, região/agendamento, modelo de cobrança, tarifas, mínimos/máximos, adicionais, parâmetros a preencher, condição/método/momento de pagamento e regras de negociação.
- Separar campos de preço calculável (fixo, hora, km, unidade, combinação) de “orçamento após avaliação”; cada modelo exige só seus próprios dados.
- Apresentar uma revisão clara antes de publicar: tipo, como o serviço funciona, o que será cobrado, como o cálculo é feito, opções de pagamento, campos que aparecerão na proposta e exemplo numérico.
- Preservar os anúncios existentes e o fluxo de edição.

### 2. Cálculo e snapshot REST

- Validar autorização, versão do anúncio, respostas, limites, tarifas e condição de pagamento no backend; calcular totais em `BigDecimal` e arredondar para centavos somente segundo regra explícita.
- Na proposta, receber respostas aos campos do anúncio e uma das condições publicadas. Rejeitar campo extra, opção não permitida, tarifa enviada pelo navegador ou quantidade fora dos limites.
- Persistir versão da configuração e snapshot imutável de cada proposta; alterações do anúncio só afetam novas propostas. Propostas antigas continuam legíveis.
- Manter até 12 parcelas quando habilitadas no anúncio, trocas sem complemento em dinheiro e ausência de pagamentos integrados. Não exibir “pago” nem sugerir custódia/garantia pela plataforma.
- Cobrir combinações de unidade, limites, arredondamento, anúncio editado/inativado, oferta de troca, concorrência e submissão inválida.

### 3. Modal no chat e revisão legível

- Trocar o formulário embutido por diálogo responsivo. A pessoa seleciona uma oferta ativa de um dos participantes e o modal monta os campos exclusivamente a partir da configuração/versionamento do anúncio.
- Na primeira proposta, qualquer participante preenche os dados do atendimento e escolhe uma condição permitida. O modal calcula a prévia pela API e exibe a revisão antes do envio.
- Na conversa, o destinatário aceita, recusa ou abre contraproposta. A contraproposta reaproveita a definição do anúncio, mostra os valores anteriores e só permite alterar parâmetros/condições negociáveis; enviar substitui a pendente sem apagar o histórico.
- Para “orçamento após avaliação”, o cliente solicita vistoria/orçamento e o profissional apresenta o valor no mesmo fio. O acordo só é formado depois do aceite explícito.
- Exibir no histórico o snapshot de serviço, parâmetros, cálculo e condição de pagamento aceita. Deixar claro que isso registra os termos combinados, sem transação financeira no SkillSwap.
- Usar mensagens curtas e concretas; retirar slogans genéricos e repetição de avisos sem função.
- Confirmar acessibilidade do modal: foco inicial, contenção/restauração de foco, Escape, nomes dos campos e leitura dos erros.

### 4. Validação e publicação

- Typecheck/build e uma rodada das suítes Angular/Java depois de implementar os incrementos.
- Ensaiar as migrações em MySQL isolado restaurado de backup; comparar registros anteriores e revisar a memória de cálculo.
- Conferir perfil, edição de anúncio e proposta em Chromium desktop/mobile; revisar contraste e teclado.
- Após aceite do plano e conclusão dos critérios, publicar com backup verificado, preservar MySQL/uploads/imagens anteriores e manter rollback documentado.

## Critérios de aceite

- Uma pessoa consegue anunciar qualquer serviço dentro de uma categoria/tipo válido, configurar os modelos e parâmetros que aceita e editar a oferta sem perder dados.
- Antes de publicar, o anunciante consegue definir integralmente como o serviço funciona, como o preço é calculado e quais condições/métodos/momentos de pagamento aceita.
- A proposta não cadastra serviço, campo, tarifa nem pagamento novo: exibe e preenche somente os parâmetros e opções previamente publicados no anúncio.
- O chat continua único por par; ambos podem propor, e cada proposta aponta para um anúncio de um participante. O backend rejeita anúncios de terceiros e dados fora da configuração.
- Valores e parcelas são validados pelo servidor; a soma exibida é reproduzível pelos itens persistidos e não muda quando o anúncio é alterado depois.
- Aceite, recusa e contraproposta ficam no mesmo histórico; contraproposta substitui a pendente preservando-a como “substituída”. Trocas permanecem sem dinheiro; serviço sob avaliação não apresenta valor final antes da cotação; não há cobrança dentro da plataforma.
- Erros de API mantêm o modal e dados preenchidos; cada envio persistido aparece no histórico e só é confirmado após sucesso REST.
- Categorias antigas, anúncios e propostas existentes permanecem consultáveis; nenhuma métrica, tipo ou parâmetro fictício aparece como dado real.
- A rota de editar anúncio/perfil não leva o usuário a uma página inicial sem explicar o que ocorreu.
- Modal e revisões funcionam por teclado, leitor de tela e em telas estreitas; textos usam ações e dados concretos.

## Riscos

| Risco | Mitigação |
| --- | --- |
| Fórmula ou taxa alterada pelo cliente | Calcular somente no servidor a partir da configuração do anúncio e guardar snapshot. |
| Mudança de preço alterar propostas anteriores | Snapshot imutável e revisão de contrato aceito. |
| Escopo infinito de campos dinâmicos | Tipos primitivos permitidos, limites e catálogo de unidades; sem código/fórmula livre. |
| Categorias novas quebrarem enum/cliente antigo | Catálogo REST aditivo e compatibilidade dos códigos antigos; migração em cópia isolada. |
| Valor estimado ser interpretado como preço garantido | Identificar estimativa, origem e componentes; proposta só se torna acordo após aceite dos dois lados. |
| Corrigir atalhos pode interferir na navegação/rolagem | Manter a rota do perfil, testar teclado e rolagem em todas as seções, desktop e mobile. |

## Decisões confirmadas e pendência

- A navegação com falha é a troca entre seções do perfil, tanto no perfil consultado quanto no próprio perfil aberto publicamente; o desvio foi reproduzido.
- A “tela de análise” inclui a revisão do anúncio e a revisão da proposta.
- Ambos podem enviar propostas dentro da conversa única.
- Configuração completa do serviço, cobrança e condições de pagamento acontece no anúncio; proposta só preenche os campos e opções que essa oferta publicou.
- Ambos podem iniciar proposta na conversa. Quem recebe aceita, recusa ou envia contraproposta baseada na mesma oferta; a contraproposta substitui a pendente e preserva o histórico.
- A plataforma registra os termos, mas não processa nem confirma pagamentos nesta fase.
- O catálogo continua controlado/extensível, com “Outro serviço” e título livre; cada anunciante configura os parâmetros operacionais da sua oferta.
