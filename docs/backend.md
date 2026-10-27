# Backend e integração Angular

## Base adotada em 21/10/2026

O usuário escolheu integrar o backend de `origin/main` (`6085271`) ao layout e ao cliente HTTP locais. Há uma aplicação Spring Boot 4.0.8 com Java 21, Maven Wrapper 3.10.0, JPA/Lombok e JWT em `com.skillswap.api`. A [ADR-004](decisions/004-integra-backend-github.md) registra a escolha; a ADR-003 documenta a etapa anterior.

Os domínios de anúncios, usuários, propostas, acordos, mensagens e avaliações foram preservados. Catálogo, conta, perfil, portfólio, descoberta, propostas, acordos, conversa, reputação e moderação inicial têm integração Angular/API. A validação completa com MySQL e os fluxos operacionais ainda estão pendentes; a presença de código não comprova o MVP inteiro.

## Convenções REST

Usar recursos nas URLs, GET sem modificar dados, POST para criação, PUT para substituição completa, PATCH para alteração parcial e DELETE para remoção autorizada. Respeitar idempotência de PUT/DELETE, DTOs, validação no servidor, filtros por query string, paginação limitada e códigos HTTP coerentes. Criação devolve 201 e Location; falhas nunca simulam sucesso.

Os aliases de autenticação recebidos do GitHub são mantidos por compatibilidade; o Angular usa as rotas de recursos abaixo. Rotas antigas dos módulos ainda sem interface deverão ser revisadas ao implementar seus fluxos, sem quebrar consumidores silenciosamente.

## Contrato usado pelo Angular

Base da própria origem: `/api`. O proxy de desenvolvimento e o Nginx encaminham essa rota ao backend.

| Método | Rota | Comportamento |
| --- | --- | --- |
| GET | `/categorias` | Códigos e rótulos dos enums definidos no backend. |
| GET | `/vitrine` | Categorias visuais, destaques e serviços persistidos no banco. |
| GET | `/anuncios?busca=&categoria=&cidade=&modalidade=VENDA&precoMinimo=&precoMaximo=&diaSemana=&periodo=&pagina=0&tamanho=5` | `{itens,pagina,tamanho,totalItens,totalPaginas}`; filtros são opcionais, cidade até 80 caracteres, página até 1.000.000, tamanho 1–50 e busca até 100 caracteres. `modalidade` aceita `VENDA` ou `TROCA`; faixa de preço se aplica a vendas com preço, e nunca inclui trocas. `diaSemana` aceita `SEGUNDA` a `DOMINGO` e `periodo` aceita `MANHA`, `TARDE` ou `NOITE`; quando ambos são enviados, devem corresponder ao mesmo período cadastrado. Mínimo não pode exceder máximo. Ordem por data/ID decrescente. |
| GET | `/anuncios/{id}` | Anúncio ativo ou 404; inclui `mediaAvaliacoesAnuncio` e `quantidadeAvaliacoesAnuncio`, derivados de avaliações existentes de acordos concluídos. |
| GET | `/anuncios/meus?pagina=0&tamanho=20` | Exige JWT; lista somente anúncios da conta autenticada, inclusive pausados, sem exemplos demonstrativos; paginação até 50. |
| GET | `/anuncios/meus/{id}` | Exige JWT do proprietário; permite carregar para edição anúncio ativo ou pausado. |
| POST | `/usuarios` | Criar conta, 201 com Location e JWT; alias antigo `/auth/registrar`. |
| POST | `/sessoes` | Autenticar com e-mail/senha e receber JWT; alias antigo `/auth/login`. |
| POST | `/anuncios` | Exige Bearer JWT; 201 com Location após persistir. |
| PUT | `/anuncios/{id}` | Exige proprietário autenticado e entrada completa. |
| PATCH | `/anuncios/{id}` | Exige proprietário autenticado; recebe `{ "status": "ATIVO" }` ou `{ "status": "INATIVO" }` e devolve a representação atualizada. |
| DELETE | `/anuncios/{id}` | Exige proprietário; inativa e devolve 204. Exemplos não podem ser alterados. |
| GET | `/perfis/{id}` | Campos públicos do perfil; sem e-mail ou outros campos de autenticação. |
| GET | `/perfis/{id}/anuncios?pagina=0&tamanho=6` | Página limitada às ofertas ativas não demonstrativas do profissional. |
| GET | `/perfis/{id}/publicacoes?pagina=0&tamanho=12` | Publicações públicas de portfólio, com até 20 itens por página e fotos em ordem. |
| GET | `/profissionais?busca=&cidade=&notaMinima=&pagina=0&tamanho=12` | Perfis públicos com especialidades preenchidas; filtra nome/especialidades, cidade e média mínima, e limita a página a 20. O filtro de nota só considera perfis com pelo menos três avaliações visíveis; a ordenação permanece estável por criação. |
| GET | `/perfil` | Exige JWT e devolve o perfil da conta autenticada. |
| PATCH | `/perfil` | Exige JWT; altera nome público, biografia, cidade ou especialidades após validar o DTO. |
| PUT | `/perfil/foto` | Exige JWT e multipart `file`; valida conteúdo JPG/PNG/GIF, até 5 MB, 5000 px por lado e 20 MP; associa a nova imagem ao perfil autenticado. |
| DELETE | `/perfil/foto` | Exige JWT; remove a referência da foto atual do perfil e devolve 204. |
| POST | `/uploads` | Exige JWT; recebe imagem validada e devolve URL relativa para uso em anúncio. |
| GET | `/uploads/uso` | Exige JWT; informa bytes usados, quota da conta e limite por imagem. |
| GET | `/uploads/{nomeArquivo}` | Serve somente nomes aleatórios gerados pelo backend, como imagem pública com `nosniff`. |
| POST | `/publicacoes` | Exige JWT; cria publicação com legenda de até 500 caracteres e de 1 a 6 fotos existentes no armazenamento. Vínculo opcional é aceito somente para oferta ativa e própria. Responde 201 com Location. |
| PATCH | `/publicacoes/{id}` | Exige o autor; altera a legenda (até 500 caracteres) e devolve a publicação atualizada, preservando suas fotos e vínculo. |
| GET | `/publicacoes/{id}` | Consulta uma publicação pública de portfólio pelo identificador. |
| DELETE | `/publicacoes/{id}` | Exige JWT e autoria; remove a publicação e suas referências de foto, responde 204. Arquivos não são apagados. |

### Negociação, conversa e reputação

| Método | Rota | Comportamento |
| --- | --- | --- |
| POST | `/anuncios/{anuncioId}/propostas` | Exige JWT de outra pessoa; cria proposta pendente e responde 201 com Location. |
| GET | `/propostas?direcao=ENVIADAS&pagina=0&tamanho=20` | Exige JWT; lista propostas enviadas ou recebidas em página de até 50 itens. |
| GET | `/notificacoes?pagina=0&tamanho=20` | Exige JWT; lista somente notificações da conta autenticada, da mais recente à mais antiga. |
| PATCH | `/notificacoes/{id}` | Exige proprietário da notificação; recebe `{ "lida": true }` ou `{ "lida": false }` e marca ou reabre a notificação. |
| GET | `/propostas/{id}` | Exige JWT de uma das partes da proposta. |
| PATCH | `/propostas/{id}` | Aceita `{ "status": "ACEITA" }` ou `{ "status": "RECUSADA" }` do anunciante, e `{ "status": "CANCELADA" }` do proponente; somente enquanto pendente. Aceite, recusa, cancelamento e criação usam bloqueio transacional por anúncio. Aceitar cria um acordo e recusa propostas pendentes concorrentes. |
| GET | `/acordos?pagina=0&tamanho=20` | Exige JWT; lista acordos dos quais a pessoa participa, até 50 por página. |
| GET | `/acordos/{id}` | Exige JWT de um participante. |
| GET | `/acordos/{id}/eventos` | Exige participante; lista em ordem cronológica o início do acordo, confirmações, conclusão e cancelamento, com ator e data. |
| PATCH | `/acordos/{id}` | Exige participante; status pode ser `CONCLUIDO` ou `CANCELADO`, somente a partir de `EM_ANDAMENTO`. `CONCLUIDO` registra a confirmação de quem chamou e só muda o status depois que os dois participantes confirmarem; a resposta identifica a confirmação de cada lado. `CANCELADO` continua disponível enquanto estiver em andamento. |
| GET | `/acordos/{id}/mensagens?pagina=0&tamanho=50` | Exige participante; mensagens em ordem cronológica, até 100 por página. |
| POST | `/acordos/{id}/mensagens` | Exige participante; grava mensagem de até 2.000 caracteres, 201 com Location. |
| GET | `/acordos/{id}/mensagens/{mensagemId}` | Exige participante; 404 se a mensagem não pertencer ao acordo informado. |
| POST | `/acordos/{id}/avaliacoes` | Exige participante e acordo concluído; uma avaliação por participante, sem autoavaliação. Recebe `nota` (profissional, 1–5), `notaAnuncio` (oferta/serviço, 1–5) e depoimento opcional. `notaAnuncio` é opcional para compatibilidade com clientes antigos. Responde 201. |
| GET | `/acordos/{id}/avaliacoes` | Exige participante; lista avaliações vinculadas ao acordo. |
| PATCH | `/acordos/{id}/avaliacoes/{avaliacaoId}/resposta` | Exige JWT do profissional avaliado, participante do acordo concluído; aceita `{ "resposta": "..." }` até 1.000 caracteres, uma única vez. A resposta é pública junto da avaliação e não altera nota nem depoimento. |
| GET | `/acordos/{id}/avaliacoes/{avaliacaoId}` | Exige participante; 404 se a avaliação não pertencer ao acordo. |
| GET | `/usuarios/{id}/avaliacoes` | Lista avaliações recebidas pelo perfil público, incluindo as duas notas e depoimento, quando fornecidos. |
| POST | `/denuncias` | Exige JWT; cria denúncia de publicação ou avaliação visível, com motivo e detalhe opcional; a mesma conta só denuncia o mesmo conteúdo uma vez. Responde 201 com Location. |
| GET | `/denuncias/{id}` | Exige JWT; consulta apenas denúncia própria ou para a qual o usuário esteja na allowlist de moderação. |
| GET | `/moderacao/acesso` | Exige JWT; informa se o e-mail autenticado pertence à allowlist `MODERATOR_EMAILS`. |
| GET | `/moderacao/denuncias?status=ABERTA&pagina=0&tamanho=20` | Exige JWT de moderador; lista denúncias paginadas, da mais antiga, preservando trecho do conteúdo reportado. |
| PATCH | `/moderacao/denuncias/{id}` | Exige moderador; `{ "acao": "OCULTAR" }`, `{ "acao": "DESCARTAR" }` ou `{ "acao": "REEXIBIR" }`. Ocultação não apaga evidência e sai das páginas/médias públicas; reexibição só é permitida para denúncia ocultada. |

Os endpoints legados `/propostas/minhas`, `/propostas/recebidas`, `/propostas/{id}/aceitar`, `/propostas/{id}/recusar`, `/acordos/meus`, `/acordos/{id}/concluir` e `/acordos/{id}/cancelar` continuam disponíveis para clientes antigos. A rota legada de conclusão segue a mesma regra de confirmação mútua. O Angular usa coleções de recursos, `PATCH` no recurso e query parameters para filtrar e paginar.

O backend usa enums `MANUTENCAO`, `AULAS`, `TRANSPORTE` e `VENDA`/`TROCA`. O cliente HTTP converte os códigos para os valores internos do formulário e o envio para os enums do servidor. A média e a quantidade da oferta ignoram avaliações antigas sem `notaAnuncio`; a nota do profissional continua agregada no perfil.

Cadastro/edição: título 1–100, descrição 1–1500, cidade 1–80, categoria e modalidade válidas. Venda exige preço de 0,01 a 99.999.999,99 com até duas casas; troca exige habilidade de até 120 caracteres e não preserva preço. Campos obrigatórios rejeitam espaços. Proprietário, imagens ilustrativas, avaliações e indicação de exemplo não são controlados pelo formulário. A conta pode pausar e reativar o próprio anúncio com `PATCH`; o público vê somente anúncios ativos.

Disponibilidade: cada anúncio pode declarar até 21 combinações únicas entre os sete dias da semana e os períodos `MANHA`, `TARDE` e `NOITE`. O `PUT` substitui a lista completa e a omissão equivale a lista vazia. É uma declaração do anunciante, sem reserva ou confirmação de agenda. A migração Flyway V2 cria a tabela de associação.

Erro JSON: `{codigo,mensagem,campos}`. 400 para entrada inválida; 401 para credenciais/token ausentes ou inválidos; 403 para operação de outro usuário; 404 para recurso ausente/inativo; 405 para método indisponível; 500/503 com mensagem genérica. Não retornar SQL, senha, segredo JWT ou stack trace ao cliente.

Busca ignora acentos/caixa e trata %/_ literalmente. A pesquisa aceita categoria, cidade, modalidade e faixa de preço combinados. A faixa inclui somente vendas com valor monetário; ofertas de troca não possuem preço e não aparecem quando há filtro de preço. Um inicializador preenche o índice novo dos anúncios antigos sem mudar seus dados ou proprietários. No MySQL, outro inicializador amplia o enum antigo conhecido para incluir TRANSPORTE antes da carga; não altera enums desconhecidos. A vitrine é idempotente, identificada como ilustrativa e não substitui dados existentes.

### Dados demonstrativos

A carga de `vitrine-inicial.json` contém 13 anúncios ilustrativos: os cinco originais e oito exemplos adicionais de elétrica, encanamento, pintura, inglês, reforço escolar, informática, hortas e jardinagem. Os novos exemplos ficam em Florianópolis, São José, Palhoça e Biguaçu/SC; cinco são de venda com preço/unidade e três são de troca, com habilidade desejada e preço nulo. Seus perfis são identificados como ilustrativos e não possuem notas inventadas.

Os inserts ocorrem pela aplicação/JPA no MySQL, com chave estável `codigoLegado`: reiniciar não duplica nem sobrescreve anúncios existentes. Não é necessário executar SQL manual ou criar um proprietário fictício para anúncios reais. `SEED_ENABLED=false` impede novas inserções. As coleções visuais originais permanecem preservadas como referência do layout; não são estatísticas de popularidade nem comprovam disponibilidade de produtos.

## Contas no Angular

A tela `/conta` permite registrar, entrar e sair; `/anuncios/novo` solicita login antes de salvar. O JWT fica somente em memória e é enviado apenas à API relativa da própria origem. Não há armazenamento em localStorage/sessionStorage. Recarregar encerra a sessão, mas não apaga anúncios; um 401 encerra a sessão local e a tela trata a falha.

Sair remove a credencial da memória. O token no servidor expira em uma hora; esta etapa não implementa revogação individual. A edição básica do perfil e a troca da foto usam o JWT em memória. O backend confere o conteúdo decodificado e não confia no MIME/na extensão do arquivo; SVG e WebP não são aceitos. A aplicação usa `UPLOAD_DIR` (padrão `uploads`) e os Compose local/público montam volume de arquivos. Cada conta tem quota padrão de 100 MiB (`UPLOAD_USER_QUOTA_BYTES`) e cada imagem aceita até 5 MiB; `GET /api/uploads/uso` informa o consumo, incluindo arquivos enviados ainda sem referência. A migração V6 registra fotos já ligadas a perfil, anúncio e portfólio, mede tamanhos existentes no primeiro acesso e impede contagem duplicada quando a mesma conta reenvia o mesmo conteúdo. Uma rotina diária às 03:30 UTC remove somente arquivos com nome hash gerado pela API, sem referência em perfil, anúncio ou publicação e com mais de 30 dias, liberando também o registro de quota. Configure `UPLOAD_ORPHAN_RETENTION_DAYS` (mínimo 1) e `UPLOAD_CLEANUP_CRON` se necessário. Backup do volume e migração dos arquivos públicos existentes continuam pendentes. Pagamentos integrados não estão disponíveis nesta fase.

## Execução local

Pré-requisitos: JDK 21, PowerShell 7, Docker Desktop ativo e Node compatível com Angular 20. Na raiz:

```powershell
./scripts/iniciar-banco.ps1
./scripts/iniciar-backend.ps1
```

Em outro terminal: `npm ci` e `npm start`. Angular: localhost:4200; API: 127.0.0.1:8080; MySQL local: 127.0.0.1:3307. O script do banco cria senhas locais e JWT_SECRET em `.env` ignorado pelo Git. O script Java aceita `-JavaHome` quando necessário e só lê variáveis conhecidas do arquivo.

| Variável | Uso |
| --- | --- |
| DB_PASSWORD | Obrigatória, senha do banco. |
| MYSQL_ROOT_PASSWORD | Inicialização do MySQL pelo Compose. |
| JWT_SECRET | Obrigatória, chave aleatória de pelo menos 32 bytes; gerada pelo script. |
| DB_USER | Padrão skillswap. |
| DB_URL | Padrão MySQL em 127.0.0.1:3307/skillswap. |
| PORT / SERVER_ADDRESS | Padrões 8080 / 127.0.0.1; a imagem Docker usa 0.0.0.0 internamente. |
| SEED_ENABLED | Padrão true; false não insere novos exemplos e não apaga dados existentes. |
| UPLOAD_DIR | Diretório de arquivos enviados; padrão `uploads`. Deve estar em armazenamento persistente. |
| UPLOAD_USER_QUOTA_BYTES | Quota de armazenamento por conta em bytes; padrão 104857600 (100 MiB), no mínimo 5 MiB. |
| MODERATOR_EMAILS | E-mails de contas existentes, separados por vírgula, autorizados a consultar a fila e decidir denúncias; vazio por padrão, sem conta administrativa fixa. |
| UPLOAD_ORPHAN_RETENTION_DAYS | Retenção mínima antes da remoção automática de uploads sem referência; padrão 30 dias. |
| UPLOAD_CLEANUP_CRON | Expressão cron Spring, interpretada em UTC; padrão diário às 03:30. |

## Migrações do banco

O schema versionado fica em `backend/src/main/resources/db/migration`. A migração `V1__baseline_skillswap.sql` cria as tabelas atuais em um banco vazio; o Hibernate usa `ddl-auto: validate` e não altera schema automaticamente. O Flyway também valida checksums e executa novas migrações versionadas uma vez, em ordem.

A migração V8 acrescenta resposta profissional e instante da resposta às avaliações existentes. Apenas o profissional avaliado pode responder uma vez, depois de concluído o acordo. A resposta não permite editar nota ou depoimento e é apresentada junto da avaliação pública. O fluxo foi coberto por integração com H2; MySQL 8.0/8.4 real segue pendente.

Testes usam H2 em modo MySQL, aplicam a mesma V1 e validam as entidades contra o resultado. Isso cobre o contrato ORM/schema, mas não substitui executar a migração em MySQL 8.0 e 8.4.

Um banco existente sem `flyway_schema_history` falha fechado com a configuração padrão. Antes de adotar o Flyway nele, faça backup, confira a estrutura com `mysqldump --no-data` ou `information_schema` e compare tabelas, colunas, tipos, índices e chaves com a V1. Somente após confirmar que o schema corresponde à versão 1, defina `FLYWAY_BASELINE_ON_MIGRATE=true` no ambiente da API para uma inicialização; o Flyway registra a versão 1 e não executa a V1 nesse banco. Remova a variável após a inicialização. Não habilite esse modo por padrão nem use em um banco público sem solicitação, backup verificado e conferência da estrutura: o Flyway pode marcar qualquer schema não vazio como baseline.

Para um schema vazio, não habilite o baseline automático: a própria V1 cria as tabelas. Se a API existente iniciar com erro por falta de histórico, não volte `ddl-auto` para `update`; confira primeiro a configuração de conexão e o estado real do banco.
| WEB_BIND / WEB_PORT | Prévia Docker: padrões 127.0.0.1 / 8088. |

Para o conjunto completo local: `docker compose --profile app up -d --build --wait`. A imagem Angular é compilada em estágio separado; não depende de frontend-dist no host. O Nginx preserva as rotas da SPA e encaminha /api à imagem Java. O arquivo canônico é compose.yaml; o docker-compose.yml concorrente foi removido.

MySQL local permanece em 8.4 com seu volume. Nunca remover o volume para reinicializar. Alterar .env não troca a senha de um volume já inicializado. Hibernate update preserva as tabelas; migrações versionadas continuam necessárias antes de produção.

As tabelas antigas singulares da primeira aplicação não são apagadas. As entidades adotadas usam tabelas plurais. Anúncios anônimos reais do esquema anterior precisam de atribuição explícita de proprietário antes de importação; não inventar um dono.

## Testes

Na pasta backend, com JAVA_HOME apontando para JDK 21: `./mvnw.cmd test` ou `./mvnw.cmd package`. O jar é `target/skillswap-api-0.0.1-SNAPSHOT.jar`. H2 é exclusivo dos testes e não acessa o MySQL de desenvolvimento. Ao trocar a base de classes/configuração, usar clean para remover saídas antigas.

Verificação da integração em 21/10/2026: 28 testes Angular, build e typecheck aprovados; 13 testes do backend com Spring/MockMvc/JPA para contrato, autenticação, propriedade, validação, paginação, busca, carga inicial e índice dos registros antigos.

Na restauração visual e ampliação da carga, o backend passou a 14 testes aprovados, incluindo a coerência das modalidades e localidades dos novos exemplos. Os 28 testes Angular, build e tipos continuaram aprovados.

Na fatia inicial de perfil, a API pública e a edição autenticada foram cobertas por testes MockMvc/JPA; `GET /perfis/{id}` não expõe e-mail. A listagem pública limita-se a ofertas ativas e não demonstrativas. A suíte então passou com 43 testes Angular e 19 Java. Na fatia de fotos, testes cobrem upload, leitura, atualização/remoção da referência e rejeição de conteúdo falso; o total passou a 46 testes Angular e 21 Java, com build e typecheck aprovados. Chrome/Playwright conferiu perfil público e editor em 1440px e 360px sem overflow horizontal nem erros JavaScript. A inspeção de MySQL local não foi possível porque o Docker Desktop não estava ativo e a porta 3307 não respondeu; reinício/persistência real continuam pendentes. A configuração do volume público foi preparada, mas ainda exige migração controlada de arquivos já existentes antes de qualquer implantação.

A descoberta pública também oferece `GET /profissionais`, com busca em nome/especialidades, filtro por cidade, média mínima opcional (1 a 5), paginação e ordenação estável por criação. O filtro de média só inclui perfis com pelo menos três avaliações visíveis, evitando tratar poucas avaliações como reputação consolidada. Só entram perfis que declararam especialidades; resultados mostram avaliações apenas quando há registros reais e não retornam e-mail. Testes MockMvc confirmam os filtros, a amostra mínima e a exclusão de perfis vazios. O endpoint ainda precisa ser verificado com MySQL real.

O portfólio agora persiste publicações e até seis referências de fotos por post; criação exige autenticação, valida que as imagens já foram enviadas ao armazenamento e restringe uma oferta vinculada ao próprio autor. A página pública exibe as fotos; o proprietário publica, edita legenda, remove conteúdo e percorre páginas no editor do perfil. A rotina diária de limpeza remove arquivos hash sem referência em perfis, anúncios e publicações após a retenção configurada de 30 dias. Ainda faltam denúncia/moderação, quota por conta, backup do volume e validação real no MySQL. MockMvc/JPA valida criação, leitura, vínculo/armazenamento de imagens e bloqueio de alteração por outro usuário; o teste unitário cobre retenção, referências existentes e nomes estranhos.

A negociação agora tem recursos REST para propostas e acordos, filtros/paginação e alterações por PATCH. A tela `/acordos/{id}` apresenta a conversa paginada começando pelas mensagens recentes, permite navegar pelo histórico, enviar mensagens e confirmar conclusão. Após ambas as partes confirmarem, somente o cliente que enviou a proposta pode avaliar o profissional e o serviço; o profissional pode publicar uma resposta única e imutável no perfil. A primeira confirmação deixa o acordo em andamento e bloqueia avaliações; a segunda conclui e registra a data. A conta lista as propostas e permite cancelar uma enviada enquanto pendente. A API confere participação nas leituras e escritas; bloqueio pessimista por anúncio serializa propostas e bloqueio por acordo serializa confirmações/cancelamentos. A migração Flyway V3 adiciona os dois marcadores; registros anteriores ficam nulos quando não há evidência das confirmações, e novos acordos começam com ambos falsos. Testes MockMvc/JPA cobrem autorização, transição em duas etapas, corrida simultânea e avaliações somente após conclusão mútua e pelo cliente. Edição controlada, contestação formal, validação em MySQL real e persistência após reinício ainda estão pendentes.

Em 26/10/2026, a disponibilidade semanal declarada foi adicionada com migração Flyway V2 e filtros REST por dia/período. O formulário permite selecionar até 21 combinações; catálogo, API e detalhe apresentam o dado como declaração sem reserva. O perfil público também informa o total real de ofertas e permite percorrer suas páginas. Build, typecheck, 66 testes Angular e 35 testes Java passaram; Chrome conferiu catálogo, cadastro e perfil em 360 px, além da confirmação do acordo em 360 px e 1440 px, sem overflow ou erros JavaScript. MySQL local não respondeu porque o mecanismo Docker Desktop estava indisponível, portanto as migrações ainda não foram verificadas em MySQL real nem aplicadas a banco público.

Em 27/10/2026, a migração Flyway V4 adicionou notificações persistidas para nova proposta, resposta de proposta, nova mensagem e atualização de acordo. `GET /notificacoes` lista os dez eventos mais recentes no painel da conta e `PATCH /notificacoes/{id}` marca ou desmarca como lida apenas para o destinatário. Os textos não incluem o conteúdo privado das mensagens; cada notificação aponta para a tela interna relacionada. A validação MySQL real permanece pendente.

O histórico por acordo usa a migração Flyway V5 e registra início, confirmações individuais, conclusão e cancelamento com autor e instante do servidor. `GET /acordos/{id}/eventos` exige participação; a tela de detalhe apresenta os eventos em ordem cronológica. A migração não inventa eventos para acordos anteriores. Repetir uma confirmação já registrada não cria outro evento nem outra notificação. Testes cobrem sequência e acesso externo; Chrome conferiu a linha do tempo em 360 px e 1440 px, sem overflow ou erros JavaScript. MySQL real continua pendente.

Na prévia MySQL, foram confirmados os oito inserts (cinco vendas e três trocas), totalizando 13 exemplos e um anúncio real já existente. A verificação comparou o hash dos registros reais antes/depois e confirmou sua preservação. O navegador validou busca sem acentos, paginação, estado vazio, preço ausente nas trocas e menu com Escape, sem erros JavaScript/rede. As três telas passaram novamente nas quatro larguras e no axe. Antes da alteração foi criado backup restrito em `/home/ubuntu/backups/skillswap-layout-20261022T014042Z`.

Na VPS, Chrome/Playwright validou criação de conta (201/Location), login, venda e troca (201/Location), busca e persistência após recarga e reinício da API com MySQL 8.0. Um POST sem JWT retornou 401. A sessão foi encerrada após recarga, como previsto. Catálogo, conta e formulário passaram nas larguras 320, 768, 1024 e 1440 sem transbordamento; axe não encontrou violações WCAG A/AA nessas telas. Não houve erros JavaScript, respostas HTTP de erro ou imagens quebradas no carregamento normal do catálogo. A conta e os dois anúncios criados para a verificação foram removidos da base isolada ao concluir. Isso não substitui avaliação manual completa de acessibilidade nem testes dos demais domínios.

## VPS e preservação

O repositório anterior permanece em /home/ubuntu/skillswap-integrador. A integração usa um worktree separado em /home/ubuntu/skillswap-integracao na branch codex/sincroniza-github-vps, para preservar o serviço que ficou ativo durante o trabalho.

Histórico e arquivos foram preservados em /home/ubuntu/backups/skillswap-sync-20261022T005155Z, com backup SQL transacional de permissões restritas. A stack skillswap-integracao usa um volume separado restaurado desse backup. A prévia não usa o volume skillswap-integrador_skillswap-mysql-data, que continua sendo o banco ativo do site público. A cópia mantém MySQL 8.0, sem upgrade do diretório original.

A configuração é `docker compose -p skillswap-integracao -f compose.yaml -f compose.vps.yaml --profile app`. MYSQL_VPS_VOLUME aponta para o volume restaurado e WEB_BIND para o IP Tailscale. Não há porta de banco publicada na VPS nem alteração do proxy público dos demais projetos.

Prévia: [http://100.97.254.9:8088](http://100.97.254.9:8088), acessível com a rede Tailscale conectada. Web, API e MySQL passaram nos healthchecks. O volume da prévia é skillswap-integracao_mysql-20261021; os segredos ficam apenas no .env restrito do worktree.

A prévia é uma fotografia da base na data do backup. Nunca substituir o banco público por essa fotografia. A publicação descrita abaixo atualizou as imagens da aplicação diretamente sobre o banco ativo, preservando os dados atuais.

## Site público

Em 21/10/2026, o usuário esclareceu que o destino do retorno ao layout original era [skillswap.vizihub.com.br](https://skillswap.vizihub.com.br). O domínio ainda servia o primeiro frontend demonstrativo em memória. Foram publicados o Angular com o layout antigo e a API compatível, validados antes na prévia. O Caddy, o container MySQL, seu volume e suas credenciais permaneceram preservados.

O Compose ativo de API/web fica em `/home/ubuntu/skillswap-publicacao/compose.yaml`, baseado em `compose.publico.yaml`. O `.env` restrito reutiliza a conexão real do banco e configura um JWT_SECRET aleatório fora do Git. As imagens são `skillswap-api:3a01b4c` e `skillswap-web:3a01b4c`, compiladas para ARM na própria VPS. Não há portas adicionais publicadas. O Nginx usa o nome explícito skillswap-api para evitar colisão com serviços de outros projetos na rede do proxy.

```bash
docker compose -p skillswap-integrador -f /home/ubuntu/skillswap-publicacao/compose.yaml up -d --no-deps --wait api web
```

Essa configuração administra somente API e web; o MySQL existente aparece como órfão para o Compose e deve permanecer ativo. Não usar `--remove-orphans` nem executar `down` no conjunto original. O repositório `/home/ubuntu/skillswap-integrador` e suas configurações anteriores foram preservados para reversão; executá-los novamente repõe a versão antiga da aplicação.

Backup transacional da base ativa e configurações anteriores: `/home/ubuntu/backups/skillswap-publico-20261022T014719Z`, com permissões restritas. O script `reverter.sh` nesse diretório repõe as imagens/configurações anteriores de API/web, sem apagar ou restaurar o banco. A comparação dos campos preexistentes confirmou a preservação dos dois usuários, um anúncio, uma proposta, um acordo, uma mensagem e uma avaliação existentes. A carga acrescentou 13 exemplos identificados, sem substituir esses registros.

Validação pelo domínio público em Chrome: catálogo, busca, paginação, menu, estado vazio, conta, login, venda, troca, recarga e 401 sem JWT. Criações retornaram 201 com Location relativa válida na origem HTTPS. As três telas passaram nas quatro larguras e no axe, sem erros de JavaScript. `npm audit --omit=dev` não apontou vulnerabilidades no lockfile validado. Contas e anúncios criados exclusivamente para os testes foram removidos; todos os registros anteriores foram novamente conferidos e preservados.

## Fontes

- [Interceptores no Angular](https://angular.dev/guide/http/interceptors).
- [Sessões e autenticação no Spring Security](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).
- [Docker em estágios](https://docs.docker.com/build/building/multi-stage/).
