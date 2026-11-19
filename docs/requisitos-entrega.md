# Conferência dos requisitos da entrega

Mapa de cada requisito dos dois enunciados da entrega final para o local onde foi atendido. Atualizado em 17/11/2026.

Legenda: ✅ atendido · ⏳ pendente.

## Enunciado 1 — Backend REST com Spring Boot (Padrões de Desenvolvimento Web)

| Requisito | Situação | Onde |
| --- | --- | --- |
| Partir do projeto de e-commerce e adaptar ao domínio do PI | ✅ | Domínio SkillSwap em `backend/src/main/java/com/skillswap/api/domain/` (ver [ADR-004](decisions/004-integra-backend-github.md)) |
| ≥ 5 entidades JPA relacionadas e coerentes com o domínio | ✅ | `Usuario`, `Anuncio`, `Conversa`, `Proposta`, `Acordo`, `Avaliacao`, `Mensagem`, `ComentarioAnuncio`, `PublicacaoPortfolio`, `ExperienciaProfissional`, `Denuncia` e outras |
| Persistência em MySQL | ✅ | `application.yml`, migrações Flyway V1–V24 em `backend/src/main/resources/db/migration` |
| Diagrama de classes ou ER | ⏳ | A incluir em `docs/` (fora do escopo desta etapa, a pedido do grupo) |
| CRUD completo de todas as entidades de domínio | ✅ | [documentacao-api.md](documentacao-api.md#endpoints-por-recurso). Exclusões respeitam o domínio: anúncio só sem negociações (senão é pausado); proposta por cancelamento; acordo por cancelamento; mensagens imutáveis; usuário só sem histórico |
| Métodos HTTP e códigos corretos | ✅ | 201 + `Location` na criação, 204 em exclusões, PATCH para mudança de estado |
| Arquitetura Entity → Repository → Service → Controller; controllers sem repositories | ✅ | Pacotes por domínio; controllers dependem apenas de services |
| ≥ 2 regras de negócio no Service | ✅ | [Regras de negócio](documentacao-api.md#regras-de-negócio-camada-service) (11 regras) |
| Bean Validation com `@Valid` | ✅ | DTOs `record` com `@NotBlank`, `@Size`, `@Email`, `@Min`, `@Max`, `@DecimalMin`, `@Digits`, `@PastOrPresent`, `@Pattern`, `@Positive` |
| Exceção personalizada + `@RestControllerAdvice` | ✅ | `infra/exception/` (`ConflitoException`, `NaoEncontradoException`, `NaoAutorizadoException`, `GlobalExceptionHandler`) |
| Demonstrar 400, 404 e 409 com mensagens claras | ✅ | Pastas 02, 03, 06, 07 e 09 da coleção Postman |
| ≥ 2 consultas personalizadas | ✅ | [Consultas personalizadas](documentacao-api.md#consultas-personalizadas-spring-data-jpa) (JPQL, método derivado e nativa) |
| Spring Security com HTTP Basic | ✅ | `infra/security/SecurityConfig.java` (`httpBasic`) |
| ≥ 2 usuários e ≥ 2 perfis | ✅ | `UsuariosDemonstracao` (admin, profissional, cliente) e `PerfisAcesso` (`USER`, `ADMIN`) |
| Operação restrita a um perfil | ✅ | `GET/DELETE /api/usuarios`, `/api/moderacao/**`, `/api/painel/plataforma` exigem `ADMIN` |
| Diferença entre 401 e 403 | ✅ | Pasta 01 da coleção; teste `SegurancaBasicEPainelTest` |
| Funcionalidade específica do PI | ✅ | Negociação: conversa → proposta → aceite (acordo) → conclusão mútua → avaliação verificada (pasta 06) |
| Coleção Postman com respostas salvas, exportada em JSON, com variáveis | ✅ | `postman/projeto-integrador.postman_collection.json` (83 requisições, 83 respostas, variáveis com valores) |
| Somente credenciais de teste | ✅ | Contas `@skillswap.dev` com senhas exclusivas de teste |
| README com nome, descrição, objetivo, tecnologias, execução e integrantes | ✅ | [README](../README.md) |
| Documentação dos endpoints | ✅ | [documentacao-api.md](documentacao-api.md) |

## Enunciado 2 — Entrega final do Projeto Integrador (Angular + publicação)

| Requisito | Situação | Onde |
| --- | --- | --- |
| Angular com `standalone: false` e NgModules | ✅ | `src/app/app.module.ts`, módulos em `src/app/features/*/*.module.ts`, schematic em `angular.json` ([ADR-009](decisions/009-ngmodules-e-http-basic-entrega-final.md)) |
| Roteamento habilitado; SSR/SSG desabilitado | ✅ | `AppRoutingModule` e rotas filhas por módulo; `angular.json` sem `ssr`/`prerender` |
| Angular Material aplicado | ✅ | `src/app/shared/material.module.ts`; tema em `src/styles.scss` |
| ≥ 4 entidades de negócio relacionadas | ✅ | Usuário, Anúncio, Proposta, Acordo, Avaliação, Conversa/Mensagem, Portfólio |
| CRUD das entidades principais pela interface | ✅ | Anúncios (criar, editar, pausar e excluir em *Meus anúncios*), perfil, experiências, certificações e portfólio (*Editar perfil*), propostas/acordos/avaliações (*Mensagens*) |
| Dashboard com ≥ 3 KPIs calculados dos dados | ✅ | `/painel`: 5 KPIs da conta (anúncios ativos, propostas pendentes, acordos em andamento, taxa de aceitação, nota média) e, para ADMIN, KPIs da plataforma |
| Fluxo principal de ponta a ponta pela interface | ✅ | Cadastro → anúncio → conversa → proposta → aceite → conclusão → avaliação (ver README) |
| Repositório Git versionado e atualizado | ✅ | Este repositório |
| Aplicação publicada em nuvem, com instruções de acesso | ⏳ | https://skillswap.vizihub.com.br — publicar esta versão e informar as contas de teste no AVA |
| Diagrama de classes | ⏳ | A incluir |
