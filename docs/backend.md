# Backend e integração Angular

## Base adotada em 21/10/2026

O usuário escolheu integrar o backend de `origin/main` (`6085271`) ao layout e ao cliente HTTP locais. Há uma aplicação Spring Boot 4.0.8 com Java 21, Maven Wrapper 3.10.0, JPA/Lombok e JWT em `com.skillswap.api`. A [ADR-004](decisions/004-integra-backend-github.md) registra a escolha; a ADR-003 documenta a etapa anterior.

Os domínios de anúncios, usuários, propostas, acordos, mensagens e avaliações foram preservados. Esta integração valida catálogo, conta, cadastro e autorização de anúncios. Os demais domínios ainda precisam de telas e testes próprios; a presença de código não comprova o MVP inteiro.

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
| GET | `/anuncios/{id}` | Anúncio ativo ou 404. |
| POST | `/usuarios` | Criar conta, 201 com Location e JWT; alias antigo `/auth/registrar`. |
| POST | `/sessoes` | Autenticar com e-mail/senha e receber JWT; alias antigo `/auth/login`. |
| POST | `/anuncios` | Exige Bearer JWT; 201 com Location após persistir. |
| PUT | `/anuncios/{id}` | Exige proprietário autenticado e entrada completa. |
| DELETE | `/anuncios/{id}` | Exige proprietário; inativa e devolve 204. Exemplos não podem ser alterados. |

O backend usa enums `MANUTENCAO`, `AULAS`, `TRANSPORTE` e `VENDA`/`TROCA`. O cliente HTTP converte os códigos para os valores internos do formulário e o envio para os enums do servidor.

Cadastro: título 1–100, descrição 1–1500, cidade 1–80, categoria e modalidade válidas. Venda exige preço de 0,01 a 99.999.999,99 com até duas casas; troca exige habilidade de até 120 caracteres e não preserva preço. Campos obrigatórios rejeitam espaços. Proprietário, imagens ilustrativas, avaliações e indicação de exemplo não são controlados pelo formulário.

Erro JSON: `{codigo,mensagem,campos}`. 400 para entrada inválida; 401 para credenciais/token ausentes ou inválidos; 403 para operação de outro usuário; 404 para recurso ausente/inativo; 405 para método indisponível; 500/503 com mensagem genérica. Não retornar SQL, senha, segredo JWT ou stack trace ao cliente.

Busca ignora acentos/caixa e trata %/_ literalmente. Um inicializador preenche o índice novo dos anúncios antigos sem mudar seus dados ou proprietários. No MySQL, outro inicializador amplia o enum antigo conhecido para incluir TRANSPORTE antes da carga; não altera enums desconhecidos. A vitrine é idempotente, identificada como ilustrativa e não substitui dados existentes.

## Contas no Angular

A tela `/conta` permite registrar, entrar e sair; `/anuncios/novo` solicita login antes de salvar. O JWT fica somente em memória e é enviado apenas à API relativa da própria origem. Não há armazenamento em localStorage/sessionStorage. Recarregar encerra a sessão, mas não apaga anúncios; um 401 encerra a sessão local e a tela trata a falha.

Sair remove a credencial da memória. O token no servidor expira em uma hora; esta etapa não implementa revogação individual. Upload e pagamentos não estão disponíveis.

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

Na VPS, Chrome/Playwright validou criação de conta (201/Location), login, venda e troca (201/Location), busca e persistência após recarga e reinício da API com MySQL 8.0. Um POST sem JWT retornou 401. A sessão foi encerrada após recarga, como previsto. Catálogo, conta e formulário passaram nas larguras 320, 768, 1024 e 1440 sem transbordamento; axe não encontrou violações WCAG A/AA nessas telas. Não houve erros JavaScript, respostas HTTP de erro ou imagens quebradas no carregamento normal do catálogo. A conta e os dois anúncios criados para a verificação foram removidos da base isolada ao concluir. Isso não substitui avaliação manual completa de acessibilidade nem testes dos demais domínios.

## VPS e preservação

O repositório anterior permanece em /home/ubuntu/skillswap-integrador. A integração usa um worktree separado em /home/ubuntu/skillswap-integracao na branch codex/sincroniza-github-vps, para preservar o serviço que ficou ativo durante o trabalho.

Histórico e arquivos foram preservados em /home/ubuntu/backups/skillswap-sync-20261022T005155Z, com backup SQL transacional de permissões restritas. A stack skillswap-integracao usa um volume separado restaurado desse backup. O original skillswap-integrador_skillswap-mysql-data não é alterado. A cópia mantém MySQL 8.0, sem upgrade do diretório original.

A configuração é `docker compose -p skillswap-integracao -f compose.yaml -f compose.vps.yaml --profile app`. MYSQL_VPS_VOLUME aponta para o volume restaurado e WEB_BIND para o IP Tailscale. Não há porta de banco publicada na VPS nem alteração do proxy público dos demais projetos.

Prévia: [http://100.97.254.9:8088](http://100.97.254.9:8088), acessível com a rede Tailscale conectada. Web, API e MySQL passaram nos healthchecks. O volume da prévia é skillswap-integracao_mysql-20261021; os segredos ficam apenas no .env restrito do worktree.

A prévia é uma fotografia da base na data do backup. Promover para o endereço público exige nova sincronização dos dados e validação do serviço atual; não substituir o banco original por uma fotografia antiga.

## Fontes

- [Interceptores no Angular](https://angular.dev/guide/http/interceptors).
- [Sessões e autenticação no Spring Security](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).
- [Docker em estágios](https://docs.docker.com/build/building/multi-stage/).
