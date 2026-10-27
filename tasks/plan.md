# Plano de produto e implementação do SkillSwap

**Estado:** aprovado pelo usuário em 26/10/2026; implementação em andamento. Pagamentos ficam para fase posterior.

## Visão

Transformar o SkillSwap em uma plataforma local onde cada pessoa pode apresentar seu trabalho, ser descoberta como profissional, publicar serviços/aulas, negociar com clientes e construir reputação com base em trabalhos concluídos. A experiência combina a apresentação e o portfólio de uma rede profissional com a descoberta, os anúncios e a negociação de um marketplace.

O produto deve ajudar os dois lados:

- **Profissional/anunciante:** mostrar qualidade com perfil e portfólio, divulgar ofertas, receber contatos qualificados, organizar negociações e construir histórico verificável.
- **Cliente/contratante:** encontrar profissionais adequados à necessidade e região, comparar ofertas e avaliações confiáveis, tirar dúvidas e acompanhar o acordo.

Uma conta pode atuar como profissional e cliente. O sistema não deve confundir cadastro, fotos ou quantidade de seguidores com prova de qualidade. Avaliações, histórico, selos e indicadores precisam explicar de onde vieram e quantas operações os sustentam.

## Resultado esperado da primeira entrega completa

Uma pessoa cria e apresenta seu perfil, publica ofertas e fotos de portfólio; outra encontra um profissional por necessidade e localidade, consulta o perfil e as avaliações, envia uma proposta, conversa, registra o acordo, marca a conclusão e avalia o profissional e a oferta. Cada mudança persiste no MySQL e pode ser consultada pelas pessoas autorizadas. Pagamentos integrados ficam para uma fase posterior, conforme decisão do usuário.

## Premissas e limites

- Aplicação web responsiva, preservando a identidade visual azul/verde do primeiro SkillSwap.
- Região inicial: Florianópolis e cidades próximas; busca por cidade/bairro informado, sem exigir localização precisa do navegador.
- Stack mantida: Angular 20 standalone, Spring Boot 4 / Java 21 e MySQL, conforme ADR-001/004.
- Venda de serviço/aula e troca de habilidades continuam no escopo.
- Usuário, profissional e cliente são papéis de uso, não contas separadas.
- Publicações são portfólio de trabalho e divulgação profissional; não se pretende criar uma rede social genérica com feed sem relação com serviços.
- Nesta primeira entrega não há cobrança, retenção de valores, promessa de pagamento protegido, identidade verificada ou selo de qualidade sem mecanismo real.
- Upload de foto já existe parcialmente no branch, mas ainda requer validação de conteúdo, armazenamento durável, limites operacionais, associação e testes antes de ser considerado pronto.
- A tela de detalhe e o envio de proposta também existem parcialmente; não equivalem a chat completo, acordos validados, perfil público ou reputação implementada.

## Princípios de produto e confiança

1. **Descoberta por necessidade:** começar pela categoria/serviço e região; filtros e resultados precisam refletir dados reais.
2. **Qualidade explicável:** apresentar notas junto de quantidade, data/recência e indicação de acordo concluído. Não fabricar notas, selos, popularidade ou estatísticas.
3. **Reputação resistente a abuso:** só permitir avaliação após acordo concluído e por participante elegível; bloquear autoavaliação, duplicidade e avaliações de negociação inexistente.
4. **Imagem profissional sob controle do autor:** o profissional escolhe foto, biografia, especialidades e trabalhos que publica; conteúdo pode ser denunciado e moderado.
5. **Negociação rastreável:** proposta, aceite, cancelamento, conclusão e divergência têm estados e histórico explícitos.
6. **Segurança por padrão:** autorização no servidor, validação de arquivos e DTOs, controle de acesso às conversas, proteção contra spam e dados pessoais mínimos.
7. **Promessas correspondem ao produto:** só usar “verificado”, “seguro”, “protegido” ou “pago” quando houver validação operacional e técnica que sustente o termo.

## Escopo funcional

### 1. Conta e perfil profissional público

- Criar/editar nome público, foto de perfil, imagem de capa opcional, título profissional, biografia, categorias, habilidades, cidade/região atendida, modalidades (venda/troca), disponibilidade declarada e links opcionais aprovados.
- Exibir perfil público estável com URL compartilhável, ofertas ativas, portfólio, avaliações e indicadores derivados de dados reais.
- Permitir prévia do perfil, controle de publicação/pausa e remoção de conteúdo próprio dentro das regras de retenção.
- Manter visível o que é informação autodeclarada e o que foi verificado por uma operação concluída; não inferir identidade/documentos verificados.

### 2. Portfólio e publicações com fotos

- Criar publicação com legenda, uma ou mais imagens, categoria, data opcional e vínculo opcional a uma oferta ou serviço realizado.
- Organizar publicações no perfil; oferecer edição, remoção, denúncia e estado de moderação.
- Preparar imagens para formatos e dimensões seguros; armazenar em serviço/volume durável com backup e referências estáveis no banco.
- Informar limites de tipo, tamanho, quantidade e uso; impedir arquivo falso, path traversal, substituição de arquivos e publicação sem autorização.
- Não usar imagens do portfólio como evidência de avaliação ou serviço concluído sem vínculo validado.

### 3. Ofertas e catálogo

- Criar, editar, pausar e encerrar anúncios próprios de serviço/aula; venda exige preço/unidade e troca exige habilidade desejada.
- Conectar cada anúncio ao perfil responsável e permitir que uma publicação destaque/divulgue uma oferta.
- Pesquisar por termos, categoria, cidade/região, modalidade, faixa de preço, disponibilidade declarada e nota mínima quando houver amostra suficiente.
- Mostrar estados de carregamento, erro, resultado vazio, paginação e ordenação previsível.
- Exibir informação útil antes do contato: descrição, preço/modalidade, local de atendimento, portfólio, autor, avaliações e última atualização.

### 4. Descoberta e seleção de profissionais

- Criar resultados próprios de profissionais e de ofertas, sem depender somente do catálogo atual de anúncios.
- Permitir comparação por adequação ao termo/categoria/região, modalidade, preço informado e reputação sustentada.
- Explicar filtros e sinais apresentados. Qualquer ordenação patrocinada futura deve ser identificada como anúncio pago e não misturada a avaliações orgânicas.
- Registrar eventos de uso agregados e mínimos para medir busca sem expor mensagens privadas ou dados sensíveis.

### 5. Propostas, acordos e conversa

- Cliente envia proposta para um anúncio com mensagem inicial e, quando aplicável, contexto de prazo/serviço.
- Anunciante aceita ou recusa; o aceite cria acordo com participantes, escopo e estado inicial consistentes.
- Chat persistido por acordo, acessível apenas aos participantes; mensagens ordenadas, paginadas e com limite de conteúdo.
- Estados propostos: `PENDENTE`, `ACEITA`, `RECUSADA`, `CANCELADA`, `EM_ANDAMENTO`, `CONCLUIDA`, `EM_DISPUTA` quando aplicável. Definir transições permitidas e quem pode executá-las antes de implementá-las.
- Registrar aceite, mudanças de estado e confirmação de conclusão com data e ator. Impedir auto-proposta, duplicidade ativa, aceite concorrente e acesso por terceiros.
- Nesta fase, não afirmar que o sistema processou ou protegeu o pagamento; registrar apenas condições combinadas se o usuário confirmar esse escopo.

### 6. Avaliações e reputação

- Após acordo concluído, o cliente elegível pode enviar uma avaliação única, de 1 a 5 estrelas, comentário opcional e dimensões separadas para **profissional** e **oferta/serviço**.
- A nota do profissional agrega avaliações de acordos concluídos daquele profissional; a nota da oferta agrega avaliações vinculadas àquela oferta.
- Mostrar média, quantidade e recência; ocultar média ou marcar amostra pequena conforme regra de produto definida antes da implementação. Não arredondar de modo a sugerir precisão inexistente.
- Permitir resposta pública do profissional e denúncia; definir moderação e política de edição/contestação antes de publicar comentários.
- Trocas também podem gerar avaliação após confirmação mútua da conclusão; não depender de pagamento para ser elegível.

### 7. Pagamentos integrados — fase posterior

- Avaliar provedores que suportem a operação pretendida, métodos locais, split/repasse, reembolso e notificações confiáveis antes de escolher tecnologia.
- Integrar por referência de pagamento/tokenização hospedada pelo provedor; não receber nem armazenar número completo de cartão ou credenciais financeiras.
- Tratar webhooks autenticados, idempotência, conciliação, falha, cancelamento, reembolso e disputa. A interface não declara sucesso baseada somente no retorno do navegador.
- Definir tarifas, prazos, cancelamento e responsabilidades operacionais com o grupo antes da ativação real. Retenção/escrow só entra se o provedor oferecer suporte e houver aprovação da operação.
- Até essa fase, acordos podem ser concluídos sem confirmação financeira automatizada; a interface deixa essa limitação clara.

## Modelo conceitual proposto

```text
Usuario 1──1 PerfilProfissional
PerfilProfissional 1──N Publicacao 1──N ImagemPublicacao
PerfilProfissional 1──N Anuncio
Anuncio 1──N Proposta N──1 Usuario(proponente)
Proposta aceita 1──0..1 Acordo
Acordo 1──N Mensagem
Acordo 1──N EventoAcordo
Acordo concluido 1──N Avaliacao (limitada por autor e regra)
Avaliação ── referencia profissional e, opcionalmente, anúncio
Acordo 1──0..N Pagamento (somente fase posterior)
```

É um modelo de domínio para discussão, não uma migração pronta. Reutilizar e adaptar as entidades existentes de anúncio, proposta, acordo, mensagem e avaliação. Antes de criar tabelas, mapear o esquema atual e escrever migrações versionadas e não destrutivas; o banco público contém dados e não pode ser recriado para facilitar desenvolvimento.

## Contratos REST esperados

Manter os domínios em `com.skillswap.api.domain` e a convenção REST existente. As rotas abaixo são candidatas para discussão, não contrato aprovado:

| Recurso | Rotas candidatas | Regra principal |
| --- | --- | --- |
| Perfil | `GET/PATCH /api/perfil`; `GET /api/perfis/{slug}` | edição exige proprietário; público expõe apenas campos publicados |
| Publicações | `GET/POST /api/publicacoes`; `GET/PUT/DELETE /api/publicacoes/{id}` | leitura pública das publicadas; escrita autenticada e autorizada |
| Imagens | `POST /api/uploads`; `GET /api/uploads/{id}` | upload autenticado, arquivo validado e armazenamento durável |
| Profissionais | `GET /api/profissionais?...` | filtros, ordenação e paginação consistentes |
| Anúncios | rotas atuais `/api/anuncios` | associar perfil; manter autorização do dono e regras de venda/troca |
| Propostas | rotas atuais `/api/anuncios/{id}/propostas`, `/api/propostas/{id}/...` | validar ator e estado; retorno/status REST coerentes |
| Conversas | `GET/POST /api/acordos/{id}/mensagens` | somente participantes, paginação e limites |
| Avaliações | `POST /api/acordos/{id}/avaliacoes`; `GET /api/perfis/{slug}/avaliacoes` | exigir acordo concluído e impedir duplicidade |
| Pagamentos | `POST /api/acordos/{id}/pagamentos`; webhook do provedor | somente na fase posterior; validar assinatura e idempotência |

Todos os endpoints mutáveis validam DTOs no servidor, autorização por recurso, status/cabeçalhos apropriados, limites de paginação e erros `{codigo,mensagem,campos}`. Nunca confiar em `usuarioId`, reputação, estado de acordo ou papel enviados pelo Angular.

## Sequência de implementação e critérios de aceite

### Fase 0 — Revisar o que já existe e fechar regras

- Auditar os commits atuais de upload, detalhe e proposta; separar código utilizável de código incompleto. Corrigir o retorno de login para voltar com segurança ao destino interno solicitado.
- Registrar estados/transições de proposta e acordo, campos do perfil, dimensões das avaliações, privacidade e moderação.
- Planejar migrações versionadas, linha de base do MySQL atual, rollback e estratégia separada de prévia/publicação.
- Especificar teste do fluxo completo e ameaças de upload, abuso de proposta, exposição de conversa e manipulação de avaliação.

**Aceite:** regras de negócio e contratos REST revisados; nenhum dado do banco ativo é usado como sandbox; nenhuma promessa de pagamento/identidade está em texto sem recurso real.

**Progresso:** retorno de login restrito a rotas internas, revisão de promessas da tela de detalhe e início da revisão de dados pessoais concluídos. Estados de negociação, avaliações, moderação e migrações continuam pendentes.

### Fase 1 — Fundação de persistência e segurança

- Adotar migrações versionadas com baseline documentado para esquemas MySQL 8.0/8.4; retirar dependência de `ddl-auto:update` para evolução normal depois da transição validada.
- Definir sessão/expiração/renovação sem colocar JWT em `localStorage` ou `sessionStorage`; tratar 401 e retorno interno permitido.
- Completar upload autenticado: conteúdo real e dimensões, lista MIME/extensão, limite coordenado no Spring e proxy, nomes opacos, armazenamento persistente, quotas, remoção segura e backup.
- Criar tipos Angular explícitos para propostas, perfil, publicações e avaliações; remover `any` introduzido nos novos fluxos.

**Aceite:** banco novo e prévia migram sem perda; uploads inválidos e excessivos são rejeitados; imagem continua acessível após recriação controlada da API; testes não usam arquivo falso como imagem válida.

**Progresso:** validação do conteúdo real (JPG/PNG/GIF), limites por arquivo/dimensão e diretório configurável implementados; Compose local e público declaram volumes de mídia. Limpeza diária de arquivos hash sem referência após 30 dias considera perfis, anúncios e publicações. Ainda faltam quota por conta, rotina de backup, migração dos arquivos do container público atual e validação de reinício com MySQL. Não implantar a nova montagem pública sem essa migração.

### Fase 2 — Perfil público editável

- Criar entidade/DTO/repositório/serviço/controller e migração; campos com limites e visibilidade.
- Criar tela de perfil do proprietário, edição com prévia e perfil público standalone responsivo.
- Ligar conta, anúncios e avaliações ao perfil; manter contas existentes com perfil mínimo sem inventar biografia, nota ou especialidade.

**Aceite:** usuário edita somente seu perfil; visitante consulta a versão pública; campos privados não saem na API pública; dados persistem em reinício e erros têm feedback claro.

**Progresso:** contrato REST de leitura pública, edição do próprio perfil, foto por upload validado, listagem paginada das ofertas ativas e busca de profissionais por especialidade/cidade implementados. Integração real com MySQL e persistência após reinício ainda precisam ser concluídas e validadas.

### Fase 3 — Portfólio com publicações e fotos

- Criar CRUD de publicação e relação com o autor; múltiplas imagens ordenadas com limites; publicação no perfil.
- Exibir feed compacto no perfil e grade responsiva com detalhes e acessibilidade.
- Adicionar denúncia e estados `RASCUNHO`, `PUBLICADA`, `OCULTA` ou conjunto equivalente aprovado.

**Progresso:** criação, edição de legenda, remoção e paginação de publicações autenticadas com até seis fotos JPG/PNG/GIF, além de vínculo opcional a oferta ativa do próprio autor. O perfil público pagina as publicações a partir da API. Ainda faltam denúncia/moderação e validação real no MySQL.

**Aceite:** criação, edição e remoção afetam somente conteúdo próprio; publicação aparece no perfil público; arquivo removido não deixa referência quebrada; falha de rede preserva o texto editável.

### Fase 4 — Anúncios vinculados e descoberta

- Permitir gestão completa das próprias ofertas, incluindo pausar/reativar e editar imagem/dados.
- Criar busca de profissionais e busca de ofertas com filtros úteis e valores derivados reais.
- Abrir detalhe do anúncio e navegar ao perfil associado; explicitar modalidade, local atendido e amostra de reputação.
- Rever o estilo da página de detalhe para harmonizar com o SkillSwap original, mantendo a hierarquia necessária para conversão.

**Aceite:** filtros combinados e paginação são consistentes; resultado identifica perfil e anúncio; estados vazios/erro/retry funcionam; sem notas ou contagens fictícias; navegação após login retorna ao destino interno original.

**Progresso:** busca pública de profissionais por especialidade/nome e cidade, paginação, cartões com reputação derivada e navegação para o perfil implementadas. O catálogo combina cidade, modalidade, faixa de preço e disponibilidade semanal declarada na interface e API; trocas sem preço ficam fora da faixa monetária. O proprietário lista, edita, pausa e reativa seus anúncios pela API. A migração V2 armazena até 21 combinações de dia/período; anunciante edita a grade, clientes filtram ofertas, e detalhe esclarece que os horários são autodeclarados. O perfil público apresenta as próprias ofertas ativas, total real e navegação paginada. Validação com MySQL ainda falta.

### Fase 5 — Negociação, acordo e mensagens

- Especificar e implementar transições de proposta/acordo com autorização por papel e concorrência segura.
- Criar painel “minhas propostas/acordos” com estado de carregamento, vazio, falha, novas tentativas e ações confirmadas.
- Implementar conversa persistida por acordo, paginada e acessível apenas aos participantes; distinguir mensagem inicial de proposta de chat contínuo.
- Criar notificação no próprio site para nova proposta/mensagem/alteração de estado; decidir canais externos posteriormente.

**Aceite:** comprador e anunciante completam um fluxo venda e um fluxo troca; usuários externos não leem nem alteram conversa; aceite concorrente não cria dois acordos; erro não aparece como lista vazia.

**Progresso:** Angular lista propostas/acordos, apresenta e envia conversa por acordo, registra a confirmação de conclusão e permite cancelar proposta pendente enviada. A conversa paginada abre nas mensagens recentes e permite percorrer páginas anteriores e seguintes. A API oferece recursos REST paginados e autorização de participantes. Criação, aceite, recusa e cancelamento de propostas bloqueiam a linha do anúncio; ao aceitar, propostas concorrentes são recusadas. Conclusão agora exige confirmação de cada participante, com bloqueio da linha do acordo para serializar confirmações/cancelamento; avaliação só fica disponível após a segunda confirmação. Ainda faltam validar concorrência e persistência no MySQL real, histórico de eventos, notificações e cenários de venda e troca no MySQL.

### Fase 6 — Conclusão, avaliações e reputação

- Definir confirmação unilateral ou mútua de conclusão e janela de avaliação; resolver conflito de confirmação antes de liberar avaliação.
- Implementar avaliação profissional + oferta, vínculo obrigatório ao acordo, unicidade e estados de moderação.
- Calcular médias/contagens no servidor e documentar o tratamento de poucas avaliações, denúncia, resposta e edição.
- Exibir notas e contexto no perfil e detalhe sem personalização enganosa.

**Aceite:** apenas participante elegível de acordo concluído avalia uma vez; tentativa própria, duplicada ou sem acordo é rejeitada; nota e quantidade batem com registros; avaliação não altera texto original de terceiros.

**Progresso:** a tela coleta nota separada do profissional e da oferta após as duas partes confirmarem a conclusão; o backend impede avaliação antes disso, por não participante, autoavaliação e duplicidade. Médias e contagens reais alimentam perfil e detalhe do anúncio, e o perfil lista os depoimentos. Ainda faltam janela de contestação, moderação, denúncia, resposta/edição e validação dos indicadores no MySQL.

### Checkpoint — Primeira entrega completa sem pagamentos integrados

- Um profissional publica perfil, oferta e portfólio; cliente o encontra, consulta evidências, propõe, conversa, aceita/acorda, conclui e avalia.
- Venda e troca passam por caminhos completos; ações persistem após logout/login, atualização de página e reinício controlado.
- Auditoria de autorização, dados pessoais, upload, abuso e acessibilidade; validação de navegador e mobile; cópia de segurança e reversão praticadas em prévia isolada.
- Não publicar na VPS/site público até aprovação explícita do usuário, cópia verificada, imagem/tag testada e plano de reversão preparado.

### Fase 7 — Pagamentos integrados

- Selecionar provedor e definir taxas, split, cancelamento, reembolso, disputa e suporte operacional.
- Implementar contratos e máquina de estados de pagamento, webhooks autenticados e idempotentes, conciliação e painel de suporte.
- Realizar testes sandbox de sucesso, falha, duplicidade, cancelamento, reembolso e eventos atrasados antes de qualquer uso real.

**Aceite:** nenhum cartão ou segredo financeiro é armazenado; a confirmação do provedor, não a tela do navegador, é a fonte de estado; conciliação reconstrói todas as transações de teste; termos e limitações são apresentados antes da confirmação.

### Fase 8 — Piloto e crescimento controlado

- Pilotar com profissionais reais da região, com consentimento e conteúdo identificados corretamente; remover os dados ilustrativos do ambiente público quando houver conteúdo real suficiente.
- Medir busca sem resultados, visualização de perfil para contato, tempo de resposta, acordos concluídos, avaliações elegíveis e denúncias/resolução.
- Definir metas após uma linha de base observada, não inventar percentuais; priorizar as causas de desistência reportadas por clientes e profissionais.
- Somente depois do piloto decidir destaque patrocinado, verificação documental, geolocalização precisa, PWA/notificações push e outros recursos do PDF.

## Estratégia de qualidade e operação

- **Angular:** `npm run build`, `npm run typecheck`, `npm run test:ci`; browser real para fluxos e responsividade; teclado e verificação automática de acessibilidade com revisão manual.
- **Backend:** `./mvnw.cmd test` com JDK 21; unitários de regras, integração MockMvc/JPA, migração em MySQL 8.0 e 8.4, persistência após reinício e autorização negativa.
- **Ponta a ponta:** conta A cria perfil/anúncio; conta B encontra, propõe e negocia; A aceita; ambos completam; B avalia; perfis exibem os dados corretos.
- **Dados:** migrações aditivas e backup verificado; H2 exclusivo de testes; não remover volumes; separar completamente banco de prévia e público.
- **Operação:** logs sem JWT/segredos/conteúdo privado, monitorar erro de upload/API, limitar abuso e ter instrução de reversão. Segredos exclusivamente por ambiente.
- **Entrega:** tarefas pequenas por fluxo vertical; revisão de código, commits por objetivo e atualização desta lista ao concluir cada fatia.

## Riscos e mitigação

| Risco | Consequência | Mitigação planejada |
| --- | --- | --- |
| Perfil e avaliações sem transações reais | Confiança falsa | Notas só de acordo concluído; identificação da amostra e regras públicas |
| Upload em disco efêmero ou conteúdo forjado | Fotos perdidas ou abuso/XSS | Repositório durável, validação de bytes, reprocessamento, quotas e testes de recriação |
| Negociação sem estados definidos | Acordos duplicados e disputas confusas | Máquina de estados, controle transacional, ator e histórico por transição |
| Mensagens expostas a terceiros | Vazamento de conversa | Autorização por participante em todas as leituras/escritas e testes negativos |
| Crescimento do escopo social | Atraso e produto genérico | Limitar publicações a portfólio profissional ligado a oferta/trabalho |
| Pagamento integrado cedo demais | Risco operacional e financeiro | Fase posterior, provedor apropriado, sandbox, conciliação e aprovação antes do real |
| Evolução de schema sobre banco ativo | Perda de dados | Baseline, migração aditiva, cópia, comparação, rollback e prévia separada |
| Poucas avaliações no início | Ranking instável | Mostrar contagem/contexto, não ordenar por nota até ter amostra suficiente |

## Decisões pendentes para fechar antes das fases correspondentes

1. Quais dados do perfil são públicos por padrão e quais podem ser ocultados?
2. Avaliação: o cliente dá uma nota ao profissional e outra ao anúncio na mesma ação? O profissional também avalia o cliente?
3. Moderadores: quem poderá ocultar conteúdo, responder denúncias e tratar contestação de avaliação?
4. Imagens: limite por publicação, retenção após excluir anúncio e provedor/volume durável.
5. Na fase de pagamento, quais meios, taxas e política de estorno serão aceitos? Não escolher fornecedor antes de mapear esses requisitos.
6. Como migrar usuários e dados já existentes sem inventar perfis, propriedade ou avaliações?

A confirmação mútua para concluir acordos foi implementada e registrada na [ADR-005](../docs/decisions/005-confirmacao-mutua-acordo.md). Ainda faltam janela de contestação, notificações e processo de divergência.

## Referências do projeto

- [Escopo do MVP](../docs/escopo-mvp.md)
- [Contrato e operação do backend](../docs/backend.md)
- [Layout legado](../docs/layout-legado.md)
- [ADR-001: Angular standalone](../docs/decisions/001-angular-standalone.md)
- [ADR-004: backend adotado](../docs/decisions/004-integra-backend-github.md)
