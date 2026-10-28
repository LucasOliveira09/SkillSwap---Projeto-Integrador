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

**Progresso:** retorno de login restrito a rotas internas, revisão de promessas da tela de detalhe, regras de avaliações/moderação e migrações versionadas concluídos. Flyway V1–V14 e Hibernate `validate` iniciaram em schema novo descartável MySQL 8.0.46/Java 21 e em MySQL 8.4.11 descartável na VPS. O backup do banco público foi restaurado em schema isolado, e a versão atual da preparação/baseline aplicou V1–V14 com Hibernate `validate`; os cinco enums foram preservados e uma comparação das sete tabelas legadas encontrou zero divergências. A validação negativa também confirmou que o guard não altera a prévia antiga. Banco e uploads têm cópias verificadas na VPS, mas falta cópia externa/retenção e restauração operacional. Nenhuma migração foi aplicada ao banco público. Os fluxos REST completos de perfil, upload/portfólio, descoberta, venda/troca, conversa, conclusão mútua e avaliação passaram também em MySQL 8.4.11 descartável; a foto sobreviveu à recriação da API com volume persistente. Em 27/10/2026, uma conta sintética na instância descartável 8.4 salvou perfil por `PATCH` e foi encontrada na busca pública por especialidade e cidade. Também em 27/10, Chromium percorreu cadastro, login, edição e busca pública de perfil na prévia Tailscale MySQL 8.0.46; o perfil de teste foi pausado após a validação. Ainda em 27/10, Chromium percorreu na stack descartável MySQL 8.4 o cadastro de cliente, envio/aceite de proposta, conversa, conclusão confirmada por ambas as partes e avaliação pela interface; a API confirmou acordo `CONCLUIDO`, uma mensagem, avaliação 5/5 e nota pública 5,0. O stack e seus volumes foram removidos após a conferência. O cabeçalho consulta a allowlist e oculta moderação para conta comum. Build, typecheck e 86 testes Angular passaram. Avaliações verificadas, denúncia, resposta, edição controlada e contestação foram implementadas; moderação e sua contestação passaram em MySQL 8.0.46 numa base descartável.

### Fase 1 — Fundação de persistência e segurança

- Adotar migrações versionadas com baseline documentado para esquemas MySQL 8.0/8.4; retirar dependência de `ddl-auto:update` para evolução normal depois da transição validada.
- Definir sessão/expiração/renovação sem colocar JWT em `localStorage` ou `sessionStorage`; tratar 401 e retorno interno permitido.
- Completar upload autenticado: conteúdo real e dimensões, lista MIME/extensão, limite coordenado no Spring e proxy, nomes opacos, armazenamento persistente, quotas, remoção segura e backup.
- Criar tipos Angular explícitos para propostas, perfil, publicações e avaliações; remover `any` introduzido nos novos fluxos.

**Aceite:** banco novo e prévia migram sem perda; uploads inválidos e excessivos são rejeitados; imagem continua acessível após recriação controlada da API; testes não usam arquivo falso como imagem válida.

**Progresso:** validação do conteúdo real (JPG/PNG/GIF), limites por arquivo/dimensão, quota por conta configurável (100 MiB padrão), consulta autenticada de uso, contabilização gradual de imagens legadas e diretório configurável implementados; Compose local e público declaram volumes de mídia. Limpeza diária de arquivos hash sem referência após 30 dias considera perfis, anúncios e publicações e remove os metadados associados. O script `scripts/backup-uploads.sh` cria backup somente leitura, valida o arquivo e registra SHA-256. Upload, persistência após reinício da API e sobrevivência da foto à recriação da API com volume nomeado passaram em MySQL 8.0.46 e 8.4.11 descartáveis. Em 28/10, o diretório público persistente foi verificado e estava vazio, sem referências; o tar foi validado por checksum e extraído temporariamente. O dump e o tar foram incluídos numa cópia cifrada AES-256, cuja leitura foi testada. A restauração do dump passou em MySQL 8.0.46 isolado. Retenção definida em 30 cópias diárias e uma de fim de mês por 12 meses, ainda manual; a cópia está fora da VPS, mas no mesmo local físico. Uma nova montagem pública depende de cópia remota fora da estação e de procedimento de backup automatizado. Em 28/10, `./mvnw.cmd test` passou com 58 testes e migrações V1–V14 em H2. Em 28/10/2026, a validação local foi concluída em MySQL 8.4.11 usando o schema novo `skillswap_validation` no mesmo servidor; o volume e schema antigos (três tabelas) ficaram intactos, sem baseline. Flyway aplicou V1–V14, Hibernate `validate` passou, o schema novo tem 17 tabelas e a carga trouxe 13 anúncios. O fluxo local REST completo com upload, perfil, descoberta, anúncio, publicação, proposta, acordo, mensagem, conclusão mútua e avaliação passou; Chromium conferiu perfil, portfólio e avaliação em 390/1440 px sem erros.

### Fase 2 — Perfil público editável

- Criar entidade/DTO/repositório/serviço/controller e migração; campos com limites e visibilidade.
- Criar tela de perfil do proprietário, edição com prévia e perfil público standalone responsivo.
- Ligar conta, anúncios e avaliações ao perfil; manter contas existentes com perfil mínimo sem inventar biografia, nota ou especialidade.

**Aceite:** usuário edita somente seu perfil; visitante consulta a versão pública; campos privados não saem na API pública; dados persistem em reinício e erros têm feedback claro.

**Progresso:** contrato REST de leitura pública, edição do próprio perfil, foto por upload validado, listagem paginada das ofertas ativas e busca de profissionais por especialidade/cidade implementados. Perfil, foto e descoberta por termo/cidade foram exercitados em MySQL 8.0.46; os dados permaneceram após reinício da API. Os fluxos REST também passaram em MySQL 8.4.11 descartável na VPS. Em 28/10/2026, a integração local com MySQL 8.4.11 e Angular foi validada. Chromium local percorreu cadastro, login, edição de perfil, publicação/pausa e criação de anúncio; em 390 px, o perfil público exibiu os dados persistidos e a oferta vinculada. Busca e perfil público com portfólio/avaliação também passaram no Chromium a 390 e 1440 px. A venda local percorreu proposta, aceite, conversa, conclusão mútua e avaliação; após a validação, os perfis sintéticos foram pausados e deixaram de aparecer na busca. A revisão de outras telas autenticadas em MySQL 8.4 e a acessibilidade manual ainda estão pendentes. Em 28/10, a revisão de privacidade encontrou e corrigiu a leitura de trechos de publicação e avaliação de perfil pausado via criação de denúncia: a API agora responde 404; os testes regressivos reproduziram as falhas e passaram após os ajustes, e o cenário isolado MySQL 8.4 confirmou o comportamento.

O perfil também aceita título profissional opcional, capa e links de LinkedIn, Instagram e site HTTPS. Título e links aparecem publicamente; o título também aparece nos resultados. A busca por título inclui profissionais sem especialidades preenchidas. O titular pode pausar e reativar a exposição pública sem apagar dados ou negociações; anúncios, portfólio, avaliações e referências de imagens também respeitam essa configuração. As migrações aditivas V11–V14 preservam os registros existentes. Capa pode ser enviada/removida pelo dono e segue as regras de acesso dos uploads. URLs externas são validadas no servidor e abertas com `noopener noreferrer`.

### Fase 3 — Portfólio com publicações e fotos

- Criar CRUD de publicação e relação com o autor; múltiplas imagens ordenadas com limites; publicação no perfil.
- Exibir feed compacto no perfil e grade responsiva com detalhes e acessibilidade.
- Adicionar denúncia e estados `RASCUNHO`, `PUBLICADA`, `OCULTA` ou conjunto equivalente aprovado.

**Progresso:** criação, edição de legenda, remoção e paginação de publicações autenticadas com até seis fotos JPG/PNG/GIF, além de vínculo opcional a oferta ativa do próprio autor. Upload, criação vinculada, leitura pública e persistência da imagem após reinício/recriação da API foram exercitados em MySQL 8.0.46 e 8.4.11, com volume persistente. Denúncia e moderação de publicações passaram pela API em MySQL 8.0.46; em 8.4.11, a interface também validou denúncias de publicações e contestação/moderação de avaliações. A moderação de denúncia de publicação de portfólio ainda não foi percorrida em 8.4.11.

Em 28/10/2026, Chromium a 390 px integrado a MySQL 8.4.11 descartável percorreu cadastro, login, edição dos dados do perfil e publicação pública do perfil pela interface. Após upload e criação de uma publicação de portfólio pela API, a interface autenticada editou a legenda; o perfil público exibiu a imagem e a legenda atualizadas. Após remover a publicação, a foto retornou 404 para visitante anônimo e 200 para a conta proprietária. Conta sintética, banco, uploads, containers e configuração foram removidos. Em 28/10, Chromium também percorreu a seleção e publicação de um JPG pelo controle de arquivos, carregando a foto persistida em MySQL 8.4.11 descartável; stack e volume foram removidos ao fim.

**Aceite:** criação, edição e remoção afetam somente conteúdo próprio; publicação aparece no perfil público; arquivo removido não deixa referência quebrada; falha de rede preserva o texto editável.

### Fase 4 — Anúncios vinculados e descoberta

- Permitir gestão completa das próprias ofertas, incluindo pausar/reativar e editar imagem/dados.
- Criar busca de profissionais e busca de ofertas com filtros úteis e valores derivados reais.
- Abrir detalhe do anúncio e navegar ao perfil associado; explicitar modalidade, local atendido e amostra de reputação.
- Rever o estilo da página de detalhe para harmonizar com o SkillSwap original, mantendo a hierarquia necessária para conversão.

**Aceite:** filtros combinados e paginação são consistentes; resultado identifica perfil e anúncio; estados vazios/erro/retry funcionam; sem notas ou contagens fictícias; navegação após login retorna ao destino interno original.

**Progresso:** busca pública de profissionais por nome, título/especialidade e cidade, paginação, cartões com reputação derivada e navegação para o perfil implementadas. Média e contagem da reputação são agregadas por página, sem uma leitura do histórico para cada cartão; a suíte Java confirma média real, estado sem avaliações e limite de consultas. Busca por termo/cidade, descoberta de oferta, venda e troca foram exercitadas na API sobre MySQL 8.0.46 e 8.4.11. O catálogo combina cidade, modalidade, faixa de preço e disponibilidade semanal declarada na interface e API; trocas sem preço ficam fora da faixa monetária. O proprietário lista, edita, pausa e reativa seus anúncios pela API. A migração V2 armazena até 21 combinações de dia/período; anunciante edita a grade, clientes filtram ofertas, e detalhe esclarece que os horários são autodeclarados. O perfil público apresenta as próprias ofertas ativas, total real e navegação paginada. Em 28/10, `scripts/validar-mysql84-isolado.sh` confirmou na API/MySQL 8.4.11 combinações de categoria, cidade, modalidade, faixa de preço, texto e disponibilidade, além de excluir troca de consultas com preço. Chromium com MySQL 8.4.11 descartável confirmou a busca filtrada, perfil público com avaliação real e portfólio, e detalhe do anúncio em 390 e 1440 px; as requisições e imagens carregadas não retornaram erros. O cenário usou dados sintéticos restritos ao banco descartável, removido após a revisão. A inspeção com leitor de tela e as telas autenticadas restantes continuam pendentes.

### Fase 5 — Negociação, acordo e mensagens

- Especificar e implementar transições de proposta/acordo com autorização por papel e concorrência segura.
- Criar painel “minhas propostas/acordos” com estado de carregamento, vazio, falha, novas tentativas e ações confirmadas.
- Implementar conversa persistida por acordo, paginada e acessível apenas aos participantes; distinguir mensagem inicial de proposta de chat contínuo.
- Criar notificação no próprio site para nova proposta/mensagem/alteração de estado; decidir canais externos posteriormente.

**Aceite:** comprador e anunciante completam um fluxo venda e um fluxo troca; usuários externos não leem nem alteram conversa; aceite concorrente não cria dois acordos; erro não aparece como lista vazia.

**Progresso:** Angular lista propostas/acordos, apresenta e envia conversa por acordo, registra a confirmação de conclusão e permite cancelar proposta pendente enviada. A conversa paginada abre nas mensagens recentes e permite percorrer páginas anteriores e seguintes. A API oferece recursos REST paginados e autorização de participantes. Criação, aceite, recusa e cancelamento bloqueiam a linha do anúncio; o aceite agora também lê e bloqueia as propostas sob `PESSIMISTIC_WRITE`, recusando qualquer concorrente após o primeiro aceite. O cenário foi reproduzido em MySQL 8.0.46 antes da correção (dois HTTP 200 e dois acordos) e repetido depois (HTTP 200/400, um acordo); a suíte Java passou com 54 testes naquele checkpoint. Em 28/10, `./mvnw.cmd test` passou localmente com JDK 21.0.12.1 portátil e H2, com 56 testes e zero falhas/erros; a integração local com MySQL 8.4.11 passou depois em schema isolado. Em 28/10, Chromium local percorreu uma venda completa em MySQL 8.4.11: duas contas, proposta, aceite, conversa, confirmação mútua, avaliação verificada 5/5 do profissional e 4/5 do serviço. MySQL confirmou acordo `CONCLUIDO`, uma mensagem e uma avaliação vinculada ao anúncio; os perfis sintéticos foram pausados ao final. O limite de login foi atingido durante uma execução anterior, retornou `429`/`Retry-After` e a interface mostrou o tempo de espera. Os fluxos completos de venda e troca, mensagens, confirmação mútua e avaliações também passaram pela interface Angular contra MySQL 8.0.46; os fluxos REST completos passaram em MySQL 8.4.11. Em 28/10, `scripts/validar-mysql84-isolado.sh` percorreu perfil público, descoberta por cidade/especialidade, exclusão do filtro de média com amostra insuficiente, proposta, notificação, aceite, conversa, conclusão mútua e avaliação; após reinício da API, a média profissional, a média do anúncio e a busca permaneceram corretas. No fluxo de venda 8.4 de 27/10, duas contas completaram pela interface cadastro, perfil, anúncio, proposta, aceite, mensagens, conclusão mútua e avaliação; consulta REST confirmou avaliação e média real. Em execução posterior descartável, Chromium percorreu pela interface a negociação de troca, e a API confirmou os registros após novo login. Perfil, portfólio, anúncios, avaliações e fotos permaneceram após reinício da API em validações isoladas; em 8.4, a foto também sobreviveu à recriação da API com volume persistente. Histórico e notificações foram exercitados em MySQL. Acessibilidade manual e leitor de tela permanecem pendentes. Em 28/10, Chromium também selecionou uma foto pelo controle de arquivos, publicou no portfólio e exibiu o JPG persistido em MySQL 8.4.11 descartável; stack e volume foram removidos ao fim.

**Acessibilidade da conversa (28/10/2026):** a lista histórica não usa mais uma região viva, evitando que a mudança de página anuncie novamente todas as mensagens. Após o servidor confirmar a mensagem e a atualização da lista, a tela anuncia “Mensagem enviada.” numa região `status`. O teste de componente cobriu a lista, o envio e a confirmação; a validação manual com leitor de tela continua pendente.

**Cancelamento do acordo (28/10/2026):** participantes podem cancelar um acordo em andamento após confirmação explícita; o estado e o evento são recarregados da API. A API passou a rejeitar novas mensagens depois de cancelamento ou conclusão, mantendo o contrato de envio alinhado ao formulário, que só aparece durante o acordo em andamento. Testes de regressão reproduziram os dois envios indevidos e passaram após a correção.

**Ações de propostas na conta (28/10/2026):** o cancelamento de proposta enviada compartilha o bloqueio de ações concorrentes com aceitar e recusar. Falhas são apresentadas como erro da ação, sem se misturar a falhas de carregamento, e a interface libera nova tentativa depois da resposta 503. O teste reproduziu cliques repetidos antes da alteração e passou com um único PATCH.

**Notificações (28/10/2026):** se a API não conseguir marcar uma notificação como lida, o painel preserva a linha não lida, exibe o erro e deixa a pessoa tentar novamente. A navegação ocorre após a confirmação do PATCH; o teste cobriu falha 503 seguida de sucesso.

### Fase 6 — Conclusão, avaliações e reputação

- Definir confirmação unilateral ou mútua de conclusão e janela de avaliação; resolver conflito de confirmação antes de liberar avaliação.
- Implementar avaliação profissional + oferta, vínculo obrigatório ao acordo, unicidade e estados de moderação.
- Calcular médias/contagens no servidor e documentar o tratamento de poucas avaliações, denúncia, resposta e edição.
- Exibir notas e contexto no perfil e detalhe sem personalização enganosa.

**Aceite:** apenas participante elegível de acordo concluído avalia uma vez; tentativa própria, duplicada ou sem acordo é rejeitada; nota e quantidade batem com registros; avaliação não altera texto original de terceiros.

**Progresso:** a tela coleta nota separada do profissional e da oferta após as duas partes confirmarem a conclusão; o backend impede avaliação antes disso, por não participante, autoavaliação e duplicidade. Médias e contagens reais alimentam perfil e detalhe do anúncio, e o perfil lista os depoimentos. Notas separadas e média do profissional foram exercitadas em MySQL 8.0.46 e 8.4.11 e persistiram após reinício da API; o conjunto 8.4 produziu duas avaliações reais de teste com média 4,0. Denúncia e moderação podem ocultar/reexibir avaliações sem apagar evidências e recalculam as métricas públicas excluindo conteúdo oculto. O profissional avaliado pode publicar uma resposta única e imutável no perfil, com autoria verificada no servidor, ou abrir contestação privada em até sete dias; moderadores da allowlist decidem com fundamentação. A contestação, a autorização da fila, a decisão de ocultar e a persistência após reinício da API passaram em MySQL 8.0.46 numa base descartável. O cliente autor pode editar uma vez em até 24 horas; a versão anterior fica em histórico privado de moderação, e resposta, denúncia ou contestação bloqueia edição. As migrações V9 e V10 registram contestação e edição. A política e os contratos estão em `docs/politica-avaliacoes.md` e `docs/backend.md`. Em 28/10, Chromium a 390 px com MySQL 8.4.11 percorreu a fila de denúncias de publicação, ocultação e reexibição; conta comum recebeu 403 e o post voltou à consulta pública após reexibição. Em 28/10, `scripts/validar-mysql84-isolado.sh` também validou no MySQL 8.4.11 a API REST de denúncias de publicação, acesso restrito, duplicidade, ocultação e reexibição, incluindo a privacidade pública da imagem. Em 28/10, Chromium a 390 px com MySQL 8.4.11 validou pela interface as decisões MANTER e OCULTAR numa contestação de avaliação; após reinício da API, o estado permaneceu, a contestação ficou privada ao profissional e a média pública foi recalculada de 1,5/2 para 2,0/1. A interface de outras telas e a auditoria manual de leitor de tela continuam pendentes. Perfil, fotos, busca e avaliações verificadas foram percorridos na interface com dados reais persistidos no schema descartável MySQL 8.0.46; a API REST dos fluxos de venda/troca e avaliações também passou em 8.4.11.

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
| Poucas avaliações no início | Ranking instável | Mostrar contagem/contexto, não ordenar por nota; filtro de média só inclui profissionais com pelo menos três avaliações visíveis |

## Decisões pendentes para fechar antes das fases correspondentes

1. Visibilidade de perfil: definida em [`politica-perfis.md`](../docs/politica-perfis.md). O perfil básico e conteúdos profissionais publicados são públicos; contato, conversa e negociação são privados. Controles por campo e exclusão da conta não existem nesta fase.
2. Avaliações: uma ação do cliente registra nota do profissional e do serviço; o profissional não avalia o cliente nesta fase. Regras em [`politica-avaliacoes.md`](../docs/politica-avaliacoes.md).
3. Moderação: somente e-mails na allowlist podem analisar, com justificativa obrigatória e trilha persistida; a allowlist operacional continua configurável por ambiente.
4. Imagens: até seis fotos por publicação, armazenamento em volume configurável e limpeza de arquivos órfãos após retenção. A URL pública é revogada quando some a última referência publicada, e a resposta usa `no-store`. Em 28/10, o dump atual do banco foi copiado cifrado para a estação Windows e restaurado em MySQL 8.0.46 isolado; o arquivo do diretório público, vazio e sem referências, foi extraído em diretório temporário. O fluxo de foto também sobreviveu à recriação da API em MySQL 8.4.11 com volume persistente. A cópia externa está no mesmo local físico, a rotina ainda é manual e a retenção de longo prazo não foi exercitada.
5. Na fase de pagamento, quais meios, taxas e política de estorno serão aceitos? Não escolher fornecedor antes de mapear esses requisitos.
6. Como migrar usuários e dados já existentes sem inventar perfis, propriedade ou avaliações?

A confirmação mútua para concluir acordos foi implementada e registrada na [ADR-005](../docs/decisions/005-confirmacao-mutua-acordo.md). Notificações transacionais em tela foram implementadas; a janela de contestação das avaliações está implementada em sete dias. Um processo de divergência sobre o resultado do serviço ainda precisa ser definido antes de apresentar a plataforma como árbitro de disputas.

## Referências do projeto

- [Escopo do MVP](../docs/escopo-mvp.md)
- [Contrato e operação do backend](../docs/backend.md)
- [Layout legado](../docs/layout-legado.md)
- [ADR-001: Angular standalone](../docs/decisions/001-angular-standalone.md)
- [ADR-004: backend adotado](../docs/decisions/004-integra-backend-github.md)
