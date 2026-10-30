# Plano: serviços parametrizados e propostas no chat

**Estado:** diagnóstico concluído; plano para revisão do usuário. Nenhum código, dado público ou container foi alterado nesta etapa.

## O que encontrei

- O perfil próprio (`/meu-perfil`) não tem seletor de categorias. O campo “Especialidades” é texto livre; categorias pertencem aos anúncios. O perfil público lista os anúncios ativos do profissional.
- A criação/edição de anúncios usa as três categorias fixas do enum Java `CategoriaAnuncio`: manutenção, aulas e transporte. Não existe tipo de serviço nem configuração de parâmetros, unidades ou cálculo.
- Editar um anúncio termina em `/anuncios/minhas`, que é uma rota existente. O formulário do perfil, por sua vez, salva por `PATCH /api/perfil` sem navegar. A causa do redirecionamento relatado não está confirmada; é necessário saber qual tela e ação o reproduzem. Uma rota desconhecida cai no catálogo, e algumas telas levam ao login quando a sessão não está pronta.
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
- `POST /api/conversas/{id}/propostas` continua sendo a criação canônica. Acrescenta a opção escolhida e os valores dos campos; o servidor deriva participante, dono do anúncio, taxa e total. Responde `201 Created` com proposta e itens calculados.
- `GET /api/propostas/{id}` e as mensagens da conversa devolvem snapshot tipado dos parâmetros e itens. Status continuam por `PATCH /api/propostas/{id}`. Nenhum novo chat é criado para o mesmo par.
- Os modelos podem usar catálogo relacional para categoria/tipo, definições tipadas de campo e preço por anúncio, e snapshot imutável por proposta. O schema final e a estratégia de armazenamento (tabelas/JSON validado) dependem da auditoria de consultas e do suporte MySQL/H2; o Angular nunca envia nem executa uma definição de fórmula.

Migrações Flyway futuras devem ser apenas aditivas (V25 em diante), com novos campos opcionais para anúncios existentes. Não renomear nem remover colunas usadas por propostas, acordos e mensagens. Mapear as três categorias antigas para o novo catálogo sem reclassificar silenciosamente os anúncios. Se uma oferta antiga ainda não tiver configuração, permitir que ela continue no fluxo de proposta simples até ser editada pelo dono.

## Entrega por incrementos

### 0. Reproduzir o redirecionamento e fechar decisões

- Identificar tela, URL antes/depois, conta autenticada e ação exata; conferir retorno de sessão e edição de anúncio.
- Confirmar se “categorias no perfil” significa especialidades do perfil ou categorias dos anúncios mostrados no perfil.
- Confirmar se “tela de análise” é a revisão antes de publicar o anúncio, a revisão da proposta antes de enviar ou os dois. Este plano inclui os dois pontos de revisão.
- Aprovar catálogo administrado ou permitir tipos personalizados por qualquer anunciante; recomendação atual: categoria/tipo do catálogo e título livre em “Outro serviço”.
- Definir se o preço calculado é um orçamento estimado do cliente ou uma proposta formal do profissional. Recomendação atual: o proponente (cliente) solicita com quantidades; o valor calculado é estimado e o profissional pode responder com outra proposta na mesma conversa.

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

- Trocar o formulário embutido por diálogo responsivo com seleção de anúncio, parâmetros dinâmicos, estimativa e resumo itemizado.
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
| Redirecionamento relatado não corresponder ao fluxo auditado | Confirmar URL e passos reais antes de alterar rotas ou sessão. |

## Pendências para iniciar a implementação

1. Qual tela você usa quando tenta mudar a categoria: editar o perfil (`/meu-perfil`), editar um anúncio (`/anuncios/{id}/editar`) ou filtrar a lista de profissionais? Se puder, informe o endereço que aparece depois do redirecionamento.
2. Quando diz “tela de análise”, você se refere à revisão do anúncio antes de publicar, à revisão da proposta no chat ou a ambas?
3. Para começar, a proposta deve ser um pedido do cliente com total estimado pelos parâmetros, e o profissional responde/corrige por uma nova proposta na mesma conversa? Essa é a recomendação deste plano.
