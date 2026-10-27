# Backend e integração Angular

## Base adotada em 21/10/2026

O usuário escolheu integrar o backend de `origin/main` (`6085271`) ao layout e ao cliente HTTP locais. Há uma aplicação Spring Boot 4.0.8 com Java 21, Maven Wrapper 3.10.0, JPA/Lombok e JWT em `com.skillswap.api`. A [ADR-004](decisions/004-integra-backend-github.md) registra a escolha; a ADR-003 documenta a etapa anterior.

Os domínios de anúncios, usuários, propostas, acordos, mensagens e avaliações foram preservados. Catálogo, conta, perfil, portfólio, descoberta, propostas, acordos, conversa e reputação têm integração Angular/API. A validação completa com MySQL, regras de moderação e fluxos operacionais ainda está pendente; a presença de código não comprova o MVP inteiro.

## Convenções REST

Usar recursos nas URLs, GET sem modificar dados, POST para criação, PUT para substituição completa, PATCH para alteração parcial e DELETE para remoção autorizada. Respeitar idempotência de PUT/DELETE, DTOs, validação no servidor, filtros por query string, paginação limitada e códigos HTTP coerentes. Criação devolve 201 e Location; falhas nunca simulam sucesso.

Os aliases de autenticação recebidos do GitHub são mantidos por compatibilidade; o Angular usa as rotas de recursos abaixo. Rotas antigas dos módulos ainda sem interface deverão ser revisadas ao implementar seus fluxos, sem quebrar consumidores silenciosamente.

## Contrato usado pelo Angular

Base da própria origem: `/api`. O proxy de desenvolvimento e o Nginx encaminham essa rota ao backend.

| Método | Rota | Comportamento |
| --- | --- | --- |
| GET | `/categorias` | Códigos e rótulos dos enums definidos no backend. |
| GET | `/vitrine` | Categorias visuais, destaques e serviços persistidos no banco. |
| GET | `/anuncios?busca=&categoria=&pagina=0&tamanho=5` | `{itens,pagina,tamanho,totalItens,totalPaginas}`; página até 1.000.000, tamanho 1–50 e busca até 100 caracteres. Ordem por data/ID decrescente. |
| GET | `/anuncios/{id}` | Anúncio ativo ou 404; inclui `mediaAvaliacoesAnuncio` e `quantidadeAvaliacoesAnuncio`, derivados de avaliações existentes de acordos concluídos. |
| POST | `/usuarios` | Criar conta, 201 com Location e JWT; alias antigo `/auth/registrar`. |
| POST | `/sessoes` | Autenticar com e-mail/senha e receber JWT; alias antigo `/auth/login`. |
| POST | `/anuncios` | Exige Bearer JWT; 201 com Location após persistir. |
| PUT | `/anuncios/{id}` | Exige proprietário autenticado e entrada completa. |
| DELETE | `/anuncios/{id}` | Exige proprietário; inativa e devolve 204. Exemplos não podem ser alterados. |
| GET | `/perfis/{id}` | Campos públicos do perfil; sem e-mail ou outros campos de autenticação. |
| GET | `/perfis/{id}/anuncios?pagina=0&tamanho=6` | Página limitada às ofertas ativas não demonstrativas do profissional. |
| GET | `/perfis/{id}/publicacoes?pagina=0&tamanho=12` | Publicações públicas de portfólio, com até 20 itens por página e fotos em ordem. |
| GET | `/profissionais?busca=&cidade=&pagina=0&tamanho=12` | Perfis públicos com especialidades preenchidas; filtra nome/especialidades e cidade e limita a página a 20. |
| GET | `/perfil` | Exige JWT e devolve o perfil da conta autenticada. |
| PATCH | `/perfil` | Exige JWT; altera nome público, biografia, cidade ou especialidades após validar o DTO. |
| PUT | `/perfil/foto` | Exige JWT e multipart `file`; valida conteúdo JPG/PNG/GIF, até 5 MB, 5000 px por lado e 20 MP; associa a nova imagem ao perfil autenticado. |
| DELETE | `/perfil/foto` | Exige JWT; remove a referência da foto atual do perfil e devolve 204. |
| POST | `/uploads` | Exige JWT; recebe imagem validada e devolve URL relativa para uso em anúncio. |
| GET | `/uploads/{nomeArquivo}` | Serve somente nomes aleatórios gerados pelo backend, como imagem pública com `nosniff`. |
| POST | `/publicacoes` | Exige JWT; cria publicação com legenda de até 500 caracteres e de 1 a 6 fotos existentes no armazenamento. Vínculo opcional é aceito somente para oferta ativa e própria. Responde 201 com Location. |
| GET | `/publicacoes/{id}` | Consulta uma publicação pública de portfólio pelo identificador. |
| DELETE | `/publicacoes/{id}` | Exige JWT e autoria; remove a publicação e suas referências de foto, responde 204. Arquivos não são apagados. |

### Negociação, conversa e reputação

| Método | Rota | Comportamento |
| --- | --- | --- |
| POST | `/anuncios/{anuncioId}/propostas` | Exige JWT de outra pessoa; cria proposta pendente e responde 201 com Location. |
| GET | `/propostas?direcao=ENVIADAS&pagina=0&tamanho=20` | Exige JWT; lista propostas enviadas ou recebidas em página de até 50 itens. |
| GET | `/propostas/{id}` | Exige JWT de uma das partes da proposta. |
| PATCH | `/propostas/{id}` | Aceita `{ "status": "ACEITA" }` ou `{ "status": "RECUSADA" }` do anunciante, e `{ "status": "CANCELADA" }` do proponente; somente enquanto pendente. Aceite, recusa, cancelamento e criação usam bloqueio transacional por anúncio. Aceitar cria um acordo e recusa propostas pendentes concorrentes. |
| GET | `/acordos?pagina=0&tamanho=20` | Exige JWT; lista acordos dos quais a pessoa participa, até 50 por página. |
| GET | `/acordos/{id}` | Exige JWT de um participante. |
| PATCH | `/acordos/{id}` | Exige participante; status pode ser `CONCLUIDO` ou `CANCELADO`, somente a partir de `EM_ANDAMENTO`. |
| GET | `/acordos/{id}/mensagens?pagina=0&tamanho=50` | Exige participante; mensagens em ordem cronológica, até 100 por página. |
| POST | `/acordos/{id}/mensagens` | Exige participante; grava mensagem de até 2.000 caracteres, 201 com Location. |
| GET | `/acordos/{id}/mensagens/{mensagemId}` | Exige participante; 404 se a mensagem não pertencer ao acordo informado. |
| POST | `/acordos/{id}/avaliacoes` | Exige participante e acordo concluído; uma avaliação por participante, sem autoavaliação. Recebe `nota` (profissional, 1–5), `notaAnuncio` (oferta/serviço, 1–5) e depoimento opcional. `notaAnuncio` é opcional para compatibilidade com clientes antigos. Responde 201. |
| GET | `/acordos/{id}/avaliacoes` | Exige participante; lista avaliações vinculadas ao acordo. |
| GET | `/acordos/{id}/avaliacoes/{avaliacaoId}` | Exige participante; 404 se a avaliação não pertencer ao acordo. |
| GET | `/usuarios/{id}/avaliacoes` | Lista avaliações recebidas pelo perfil público, incluindo as duas notas e depoimento, quando fornecidos. |

Os endpoints legados `/propostas/minhas`, `/propostas/recebidas`, `/propostas/{id}/aceitar`, `/propostas/{id}/recusar`, `/acordos/meus`, `/acordos/{id}/concluir` e `/acordos/{id}/cancelar` continuam disponíveis para clientes antigos. O Angular usa coleções de recursos, `PATCH` no recurso e query parameters para filtrar e paginar.

O backend usa enums `MANUTENCAO`, `AULAS`, `TRANSPORTE` e `VENDA`/`TROCA`. O cliente HTTP converte os códigos para os valores internos do formulário e o envio para os enums do servidor. A média e a quantidade da oferta ignoram avaliações antigas sem `notaAnuncio`; a nota do profissional continua agregada no perfil.

Cadastro: título 1–100, descrição 1–1500, cidade 1–80, categoria e modalidade válidas. Venda exige preço de 0,01 a 99.999.999,99 com até duas casas; troca exige habilidade de até 120 caracteres e não preserva preço. Campos obrigatórios rejeitam espaços. Proprietário, imagens ilustrativas, avaliações e indicação de exemplo não são controlados pelo formulário.

Erro JSON: `{codigo,mensagem,campos}`. 400 para entrada inválida; 401 para credenciais/token ausentes ou inválidos; 403 para operação de outro usuário; 404 para recurso ausente/inativo; 405 para método indisponível; 500/503 com mensagem genérica. Não retornar SQL, senha, segredo JWT ou stack trace ao cliente.

Busca ignora acentos/caixa e trata %/_ literalmente. Um inicializador preenche o índice novo dos anúncios antigos sem mudar seus dados ou proprietários. No MySQL, outro inicializador amplia o enum antigo conhecido para incluir TRANSPORTE antes da carga; não altera enums desconhecidos. A vitrine é idempotente, identificada como ilustrativa e não substitui dados existentes.

### Dados demonstrativos

A carga de `vitrine-inicial.json` contém 13 anúncios ilustrativos: os cinco originais e oito exemplos adicionais de elétrica, encanamento, pintura, inglês, reforço escolar, informática, hortas e jardinagem. Os novos exemplos ficam em Florianópolis, São José, Palhoça e Biguaçu/SC; cinco são de venda com preço/unidade e três são de troca, com habilidade desejada e preço nulo. Seus perfis são identificados como ilustrativos e não possuem notas inventadas.

Os inserts ocorrem pela aplicação/JPA no MySQL, com chave estável `codigoLegado`: reiniciar não duplica nem sobrescreve anúncios existentes. Não é necessário executar SQL manual ou criar um proprietário fictício para anúncios reais. `SEED_ENABLED=false` impede novas inserções. As coleções visuais originais permanecem preservadas como referência do layout; não são estatísticas de popularidade nem comprovam disponibilidade de produtos.

## Contas no Angular

A tela `/conta` permite registrar, entrar e sair; `/anuncios/novo` solicita login antes de salvar. O JWT fica somente em memória e é enviado apenas à API relativa da própria origem. Não há armazenamento em localStorage/sessionStorage. Recarregar encerra a sessão, mas não apaga anúncios; um 401 encerra a sessão local e a tela trata a falha.

Sair remove a credencial da memória. O token no servidor expira em uma hora; esta etapa não implementa revogação individual. A edição básica do perfil e a troca da foto usam o JWT em memória. O backend confere o conteúdo decodificado e não confia no MIME/na extensão do arquivo; SVG e WebP não são aceitos. A aplicação usa `UPLOAD_DIR` (padrão `uploads`) e os Compose local/público montam volume de arquivos. A remoção da foto desvincula a referência do perfil, mas não apaga o arquivo; política de quota, limpeza e backup ainda está pendente. Pagamentos integrados não estão disponíveis nesta fase.

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
| WEB_BIND / WEB_PORT | Prévia Docker: padrões 127.0.0.1 / 8088. |

Para o conjunto completo local: `docker compose --profile app up -d --build --wait`. A imagem Angular é compilada em estágio separado; não depende de frontend-dist no host. O Nginx preserva as rotas da SPA e encaminha /api à imagem Java. O arquivo canônico é compose.yaml; o docker-compose.yml concorrente foi removido.

MySQL local permanece em 8.4 com seu volume. Nunca remover o volume para reinicializar. Alterar .env não troca a senha de um volume já inicializado. Hibernate update preserva as tabelas; migrações versionadas continuam necessárias antes de produção.

As tabelas antigas singulares da primeira aplicação não são apagadas. As entidades adotadas usam tabelas plurais. Anúncios anônimos reais do esquema anterior precisam de atribuição explícita de proprietário antes de importação; não inventar um dono.

## Testes

Na pasta backend, com JAVA_HOME apontando para JDK 21: `./mvnw.cmd test` ou `./mvnw.cmd package`. O jar é `target/skillswap-api-0.0.1-SNAPSHOT.jar`. H2 é exclusivo dos testes e não acessa o MySQL de desenvolvimento. Ao trocar a base de classes/configuração, usar clean para remover saídas antigas.

Verificação da integração em 21/10/2026: 28 testes Angular, build e typecheck aprovados; 13 testes do backend com Spring/MockMvc/JPA para contrato, autenticação, propriedade, validação, paginação, busca, carga inicial e índice dos registros antigos.

Na restauração visual e ampliação da carga, o backend passou a 14 testes aprovados, incluindo a coerência das modalidades e localidades dos novos exemplos. Os 28 testes Angular, build e tipos continuaram aprovados.

Na fatia inicial de perfil, a API pública e a edição autenticada foram cobertas por testes MockMvc/JPA; `GET /perfis/{id}` não expõe e-mail. A listagem pública limita-se a ofertas ativas e não demonstrativas. A suíte então passou com 43 testes Angular e 19 Java. Na fatia de fotos, testes cobrem upload, leitura, atualização/remoção da referência e rejeição de conteúdo falso; o total passou a 46 testes Angular e 21 Java, com build e typecheck aprovados. Chrome/Playwright conferiu perfil público e editor em 1440px e 360px sem overflow horizontal nem erros JavaScript. A inspeção de MySQL local não foi possível porque o Docker Desktop não estava ativo e a porta 3307 não respondeu; reinício/persistência real continuam pendentes. A configuração do volume público foi preparada, mas ainda exige migração controlada de arquivos já existentes antes de qualquer implantação.

A descoberta pública também oferece `GET /profissionais`, com busca em nome/especialidades, filtro por cidade, paginação e ordenação estável por criação. Só entram perfis que declararam especialidades; resultados mostram avaliações apenas quando há registros reais e não retornam e-mail. Testes MockMvc confirmam os filtros e a exclusão de perfis vazios. O endpoint ainda precisa ser verificado com MySQL real.

O portfólio agora persiste publicações e até seis referências de fotos por post; criação exige autenticação, valida que as imagens já foram enviadas ao armazenamento e restringe uma oferta vinculada ao próprio autor. A página pública exibe as fotos; o proprietário publica e remove conteúdo no editor do perfil. A remoção exclui as referências do banco, sem apagar arquivos. Denúncia/moderação, paginação visual além da primeira página, quota e limpeza de uploads continuam pendentes. MockMvc/JPA valida criação, leitura, vínculo/armazenamento de imagens e bloqueio de remoção por outro usuário.

A negociação agora tem recursos REST para propostas e acordos, filtros/paginação e alterações por PATCH. A tela `/acordos/{id}` apresenta a conversa paginada começando pelas mensagens recentes, permite navegar pelo histórico, enviar mensagens, confirmar conclusão e avaliar o participante e a oferta após concluir. A conta lista as propostas e permite cancelar uma enviada enquanto pendente. A API confere participação nas leituras e escritas; bloqueio pessimista por anúncio serializa criação, aceite, recusa e cancelamento de propostas. MockMvc/JPA cobre autorização e cancelamento; um teste concorrente em H2 confirma que dois aceites simultâneos resultam em um acordo. Moderação e resposta/edição de avaliações, validação do bloqueio no MySQL real e persistência após reinício ainda estão pendentes.

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
