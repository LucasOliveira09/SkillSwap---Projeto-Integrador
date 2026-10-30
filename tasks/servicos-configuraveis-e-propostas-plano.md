# Plano: serviços parametrizados e propostas no chat

**Estado:** diagnóstico atualizado com as respostas do usuário; implementação ainda não iniciada. Nenhum código, dado público ou container foi alterado nesta etapa.

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

## Direção recomendada

1. **Separar categoria de tipo de serviço.** A categoria organiza a busca; o tipo descreve o serviço. Exemplos iniciais podem incluir Transporte → corrida/táxi e mudança, mas a lista precisa ser validada com o grupo. A API passa a servir um catálogo persistido e extensível, sem adicionar um enum Java a cada nova opção. Para evitar categorias duplicadas e abusivas, começar com catálogo administrado; oferecer “Outro serviço” com título livre e categoria existente. Decidir depois se haverá gestão administrativa de categorias.
2. **Configurar o anúncio como uma oferta, não como um formulário universal.** O anunciante escolhe um ou mais modelos de cobrança suportados e define valores, limites e campos que o cliente deve preencher. Tipos de campo seguros: texto curto, número/unidade, data, escolha e sim/não. O catálogo pode sugerir campos, mas o anunciante ativa e configura somente os necessários.
3. **Permitir composição de preço.** Modelos iniciais: valor fixo, por hora, por km, por unidade e orçamento após avaliação. Uma oferta pode combinar itens: mudança por km + horas de equipe; corrida por km + tarifa inicial. Cada item guarda unidade, preço unitário, mínimo/quantidade opcional e regra de arredondamento. Fórmulas ficam limitadas a multiplicação e soma de itens definidos pelo servidor; não aceitar expressões de código do usuário.
4. **Calcular no backend.** A proposta informa opção de cobrança e quantidades/campos. O servidor valida a configuração vigente do anúncio, calcula subtotais e total com `BigDecimal`, aplica limites e devolve a memória do cálculo. O navegador nunca é a autoridade do preço.
5. **Congelar os termos da proposta.** Ao enviar, salvar uma cópia dos rótulos, unidades, taxas, quantidades e subtotais. Alterar o anúncio depois não muda uma proposta já enviada. Propostas antigas, sem parâmetros, continuam sendo exibidas no formato atual.
6. **Abrir um modal de proposta no chat.** No desktop, diálogo central; no celular, tela/modal de largura total. O fluxo mostra o participante e o contexto, deixa escolher um anúncio ativo, carrega os parâmetros configurados nele, calcula uma prévia e apresenta a revisão final antes de enviar. Erros e cancelamento não apagam o rascunho nem a conversa.
7. **Revisar o destino após edição.** O sucesso de editar anúncio deve dar uma confirmação explícita em “Meus anúncios” e não parecer que descartou a alteração. Para edição de perfil, manter o usuário na própria edição e oferecer link claro para ver o perfil público. Corrigir o redirecionamento relatado depois de confirmar o fluxo exato.

### Exemplos de comportamento

| Anúncio | Configurado pelo profissional | Informado pelo cliente na proposta | Revisão calculada |
| --- | --- | --- | --- |
| Corrida/táxi | R$/km, tarifa inicial, mínimo | origem/destino ou km estimados, data/horário | tarifa inicial + km × preço/km |
| Mudança | R$/km e R$/hora; mínimo de horas; avaliação presencial opcional | distância estimada, horas/equipe ou pedido de vistoria | soma dos itens configurados, com aviso de estimativa quando aplicável |
| Aula | R$/hora, sessão fixa ou pacote | duração, quantidade de sessões e objetivo | preço por unidade × quantidade, com duração total |

Se a distância ou o volume depender de vistoria, o profissional configura “orçamento após avaliação”; a proposta não inventa um total nem confirma contratação antes de os dois lados aceitarem os termos.

## Contrato e dados candidatos

O contrato deve ser fechado antes da implementação, após revisar todas as consultas/DTOs atuais. Proposta REST compatível:

- `GET /api/categorias` e novo recurso paginado `GET /api/tipos-servico?categoria=...` retornam catálogo persistido. Manter os códigos atuais de categorias durante a transição.
- `POST /api/anuncios` e `PUT /api/anuncios/{id}` recebem tipo e configuração de preço/campos; o servidor valida tamanho, valores, unidades e tipo ativo. Leitura do anúncio retorna essa configuração para criar o modal.
- `POST /api/conversas/{id}/propostas` continua sendo a criação canônica. Qualquer participante pode criar uma proposta na conversa escolhendo um anúncio ativo de um dos dois participantes; anúncio de terceiros é recusado. O servidor valida o dono do serviço, configuração, unidades e valores, e calcula o total a partir das tarifas configuradas pelo dono do anúncio. Responde `201 Created` com proposta e itens calculados.
- `GET /api/propostas/{id}` e as mensagens da conversa devolvem snapshot tipado dos parâmetros e itens. Status continuam por `PATCH /api/propostas/{id}`. Nenhum novo chat é criado para o mesmo par.
- Os modelos podem usar catálogo relacional para categoria/tipo, definições tipadas de campo e preço por anúncio, e snapshot imutável por proposta. O schema final e a estratégia de armazenamento (tabelas/JSON validado) dependem da auditoria de consultas e do suporte MySQL/H2; o Angular nunca envia nem executa uma definição de fórmula.

Migrações Flyway futuras devem ser apenas aditivas (V25 em diante), com novos campos opcionais para anúncios existentes. Não renomear nem remover colunas usadas por propostas, acordos e mensagens. Mapear as três categorias antigas para o novo catálogo sem reclassificar silenciosamente os anúncios. Se uma oferta antiga ainda não tiver configuração, permitir que ela continue no fluxo de proposta simples até ser editada pelo dono.

## Entrega por incrementos

### 0. Corrigir navegação e fechar as regras restantes

- Substituir links de fragmento relativos à raiz por navegação Angular vinculada à rota atual do perfil; habilitar/validar rolagem até o destino e manter `/profissionais/{id}` no endereço.
- Cobrir todos os atalhos do perfil próprio/público (Sobre, Experiência, Portfólio, Serviços e Avaliações); a lista de busca mantém filtros por query params na rota `/profissionais`.
- A revisão solicitada inclui tanto o anúncio antes de publicar quanto a proposta antes de enviar.
- Aprovar catálogo administrado ou permitir tipos personalizados por qualquer anunciante; recomendação atual: categoria/tipo do catálogo e título livre em “Outro serviço”.
- Ambos os participantes podem criar propostas na mesma conversa. O dono do anúncio define taxas e campos; o cálculo é feito no servidor e aparece como estimativa quando depende de medidas informadas pelo outro participante.
- Definir se uma contraproposta substitui a proposta pendente do mesmo anúncio ou pode ficar pendente ao mesmo tempo. Recomendação: manter um fluxo pendente por anúncio na conversa; a nova proposta marca a anterior como substituída, preservando o histórico.

### 1. Catálogo e configuração de oferta

- Criar catálogo de categorias/tipos e sugestões de parâmetros por tipo.
- Permitir ao anunciante habilitar unidades/modelos de preço e configurar tarifas/campos no formulário de anúncio.
- Apresentar uma revisão clara antes de publicar: tipo, condições, campos pedidos e exemplo de cálculo.
- Preservar os anúncios existentes e o fluxo de edição.

### 2. Cálculo e snapshot REST

- Validar autorização, tipo, valores e todos os campos no backend; calcular total em centavos com precisão decimal.
- Persistir versão da configuração e snapshot imutável de cada proposta; deixar propostas antigas inalteradas.
- Manter estados, regras atuais de parcelas (até 12), troca sem complemento em dinheiro e ausência de pagamentos integrados.
- Cobrir combinações de unidade, limites, arredondamento, anúncio editado/inativado, oferta de troca, concorrência e submissão inválida.

### 3. Modal no chat e revisão legível

- Trocar o formulário embutido por diálogo responsivo com seleção entre os anúncios ativos dos dois participantes, parâmetros dinâmicos, estimativa e resumo itemizado.
- Permitir que o cliente preencha os dados de serviço e que o profissional responda ou apresente contraproposta, sem sair da conversa existente.
- Exibir no histórico o mesmo resumo persistido, com aceite/recusa/cancelamento conforme participação e status.
- Usar mensagens curtas e concretas; retirar slogans genéricos e repetição de avisos sem função.
- Confirmar acessibilidade do modal: foco inicial, contenção/restauração de foco, Escape, nomes dos campos e leitura dos erros.

### 4. Validação e publicação

- Typecheck/build e uma rodada das suítes Angular/Java depois de implementar os incrementos.
- Ensaiar as migrações em MySQL isolado restaurado de backup; comparar registros anteriores e revisar a memória de cálculo.
- Conferir perfil, edição de anúncio e proposta em Chromium desktop/mobile; revisar contraste e teclado.
- Após aceite do plano e conclusão dos critérios, publicar com backup verificado, preservar MySQL/uploads/imagens anteriores e manter rollback documentado.

## Critérios de aceite

- Uma pessoa consegue anunciar qualquer serviço dentro de uma categoria/tipo válido, configurar os modelos e parâmetros que aceita e editar a oferta sem perder dados.
- O chat continua único por pessoa; o modal só oferece anúncios ativos do participante correto e campos definidos na oferta selecionada.
- Valores e parcelas são validados pelo servidor; a soma exibida é reproduzível pelos itens persistidos e não muda quando o anúncio é alterado depois.
- Trocas permanecem sem dinheiro; serviço sob avaliação não apresenta valor calculado como preço final; não há cobrança dentro da plataforma.
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
- Falta definir como contrapropostas tratam a anterior pendente: substituição sequencial (recomendado) ou várias propostas pendentes em paralelo.
- A recomendação do catálogo continua sendo categorias/tipos controlados e extensíveis, com “Outro serviço” e título livre. Os parâmetros e modelos de cobrança são configurados pelo anunciante.
