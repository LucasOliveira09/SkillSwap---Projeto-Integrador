# ADR-009: NgModules e HTTP Basic para a entrega final

Status: aceita em 17/11/2026, a pedido do usuário, para atender aos enunciados da entrega final do Projeto Integrador. Substitui a ADR-001 na organização do Angular.

## Contexto

Os dois enunciados da entrega final pedem:

- Angular com `standalone: false`, organização por NgModules, roteamento habilitado, SSR/SSG desabilitado e Angular Material;
- backend com Spring Security usando **HTTP Basic**, pelo menos dois usuários e dois perfis de acesso, com diferença demonstrável entre 401 e 403;
- CRUD completo das entidades de domínio, regras de negócio na camada Service, 400/404/409 e dashboard com pelo menos três KPIs.

A base anterior usava componentes standalone (ADR-001) e autenticação apenas por JWT.

## Decisão

### Angular

- Todos os componentes declaram `standalone: false` e pertencem a um único NgModule; o schematic do `angular.json` gera novos componentes já não standalone.
- `AppModule` + `AppRoutingModule` (`RouterModule.forRoot`) e bootstrap por `platformBrowser().bootstrapModule(AppModule)`.
- Módulos de funcionalidade carregados sob demanda: `AnunciosModule` (com `AnunciosRoutingModule`), `ContaModule`, `PerfilModule`, `NegociacaoModule`, `ModeracaoModule` e `PainelModule`.
- Módulos com mais de um endereço de topo (`/mensagens` e `/acordos/:id`; `/meu-perfil` e `/profissionais`) são montados por um `UrlMatcher` de prefixo que não consome segmentos, para manter as URLs públicas existentes.
- `SharedModule` reúne CommonModule, FormsModule, RouterModule, `MaterialModule`, `IconesModule`, o diálogo informativo e o formulário de denúncia. O `AppModule` importa só o necessário ao layout (botões, menu lateral, ícones) para manter o bundle inicial abaixo do orçamento de 500 kB.
- O `app.config.ts` passa a exportar `appProviders` (locale, HttpClient com interceptor, erros globais) usados pelo `AppModule`; `appConfig` continua disponível para os testes.

### Backend

- `httpBasic()` foi habilitado na mesma cadeia de segurança do JWT. O Angular continua usando Bearer JWT; o Postman usa HTTP Basic. Os dois autenticam as mesmas contas do banco.
- O `DaoAuthenticationProvider` usa `forcePrincipalAsString`, de modo que o principal é sempre o e-mail, como no filtro JWT.
- Perfis: `USER` para toda conta e `ADMIN` para os e-mails de `MODERATOR_EMAILS` (e para o administrador de teste quando `DEMO_USERS_ENABLED=true`). As rotas `/api/moderacao/**` (exceto `/acesso`), `GET /api/usuarios`, `DELETE /api/usuarios/{id}` e `GET /api/painel/plataforma` exigem `ADMIN`.
- As contas de teste não ficam em memória: são gravadas no MySQL por um `CommandLineRunner` idempotente, porque anúncios, propostas e acordos precisam de um `Usuario` persistido. O enunciado permite usuários em memória, mas não exige.

## Consequências

- A ADR-001 deixa de valer para a estrutura do Angular; as decisões de versões, Material, testes e organização por domínio continuam.
- As senhas padrão das contas de teste (`application.yml`) servem apenas à execução local e ao Postman. No site publicado, definir `DEMO_ADMIN_PASSWORD` e `DEMO_USER_PASSWORD` no ambiente e informar o acesso na entrega do AVA.
- Erros de estado (proposta já respondida, avaliação antes da conclusão, avaliação duplicada, acordo encerrado) passaram de 400 para 409.
