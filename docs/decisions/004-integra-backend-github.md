# ADR-004: Integrar o backend novo do GitHub

Status: aceita pelo usuário em 21/10/2026, escolhendo explicitamente o backend de `origin/main` (`6085271`) e preservando o layout Angular. Substitui a organização inicial da [ADR-003](003-backend-padroes-eventos.md), cujo histórico permanece válido.

## Decisão

- Uma aplicação Java 21/Spring Boot 4.0.8, pacote `com.skillswap.api`, organizada em `domain` e `infra`, conforme o backend adotado. Maven Wrapper 3.10.0. A aplicação anterior foi removida do código ativo; permanece no histórico e na branch anterior.
- Manter catálogo, cards, fontes, imagens e vitrine Angular. Adaptar enums de categoria/modalidade, paginação e dados visuais ao contrato da API.
- Cadastro de anúncios exige sessão JWT e proprietário real. Acrescentar tela standalone de conta, criação de usuário, login e logout local. Token apenas em memória, enviado pelo interceptor somente à API da própria origem. Recarregar encerra a sessão, sem apagar os dados.
- `/api/usuarios` e `/api/sessoes` são as rotas usadas pelo Angular. As rotas `/api/auth/registrar` e `/api/auth/login` do backend recebido ficam como aliases de compatibilidade. Novas rotas devem seguir as convenções REST.
- Preservar os módulos novos de propostas, acordos, mensagens e avaliações, sem anunciar telas ou validação integral que ainda não existem.
- Manter carga inicial identificada e idempotente; os exemplos usam uma conta de demonstração com senha aleatória indisponível para login normal. Não atribuir anúncios antigos anônimos a usuários inventados.

## Banco e VPS

O MySQL local permanece 8.4 no volume existente. As tabelas antigas singulares não são apagadas; as entidades novas usam tabelas plurais. Anúncios anônimos reais do esquema antigo precisam de atribuição explícita de proprietário antes de uma importação.

A VPS possuía um volume MySQL 8.0. A integração preserva o original e trabalha em uma cópia, mantendo MySQL 8.0 para evitar upgrade do diretório de dados. Segredos são configurados fora do Git e as senhas da cópia são renovadas. O acesso de prévia fica restrito ao Tailscale; não altera o proxy público de outros projetos.

## Fontes

- [Interceptores funcionais no Angular](https://angular.dev/guide/http/interceptors).
- [Sessões e autenticação stateless no Spring Security](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).
- [Builds Docker em estágios](https://docs.docker.com/build/building/multi-stage/).
