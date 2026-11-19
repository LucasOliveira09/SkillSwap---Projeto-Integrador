# SkillSwap — Projeto Integrador

**Plataforma de serviços e aulas locais com venda direta ou troca de habilidades.**
Projeto Integrador do grupo **Saptura** — Engenharia de Software, 4º termo.

- Aplicação publicada: **https://skillswap.vizihub.com.br**
- Documentação da API: [docs/documentacao-api.md](docs/documentacao-api.md)
- Coleção Postman (com variáveis e respostas salvas): [postman/projeto-integrador.postman_collection.json](postman/projeto-integrador.postman_collection.json)
- Conferência dos requisitos da entrega: [docs/requisitos-entrega.md](docs/requisitos-entrega.md)

## Integrantes

| Nome completo | ID acadêmico |
| --- | --- |
| Lucas Leite de Oliveira | 272826 |
| Otávio César de Oliveira | 274090 |
| Gabriel Muniz Gonçalves de Oliveira | 272995 |
| Guilherme Ribeiro de Oliveira | 272917 |
| Luiz Felipe Borges | 273825 |

## Descrição

O SkillSwap conecta quem oferece e quem procura serviços e aulas em Florianópolis e região. Um profissional publica um anúncio de **venda** (com preço) ou de **troca** (informando a habilidade que deseja receber). Interessados conversam pelo anúncio e enviam uma **proposta** (à vista, parcelada ou por troca). Quando o anunciante aceita, nasce um **acordo**; os dois confirmam a conclusão e, então, registram uma **avaliação verificada**. Perfis profissionais reúnem portfólio, experiências, certificações e reputação; a equipe de moderação trata denúncias.

### Objetivo do backend

Disponibilizar uma API REST em Spring Boot que persista em MySQL todo o ciclo de negociação (anúncio → conversa → proposta → acordo → conclusão → avaliação), aplique as regras de negócio na camada de serviços, proteja os recursos com Spring Security (HTTP Basic e JWT, perfis `USER` e `ADMIN`) e forneça os indicadores do dashboard operacional.

### Fluxo principal pela interface

1. Criar conta ou entrar em **Minha conta**.
2. **Anunciar** um serviço ou aula (venda ou troca, com disponibilidade).
3. Com outra conta, abrir o anúncio, **conversar** e **enviar proposta** pelo chat.
4. O anunciante **aceita** a proposta em **Mensagens**, gerando o acordo.
5. As duas partes **confirmam a conclusão** e o cliente **avalia** o profissional.
6. Acompanhar os indicadores em **Meu painel** (`/painel`).

### Acesso para teste

Qualquer pessoa pode criar uma conta pela tela **Minha conta**. Localmente, e no Postman, há três contas de teste criadas quando `DEMO_USERS_ENABLED=true`:

| E-mail | Senha local | Perfil |
| --- | --- | --- |
| `admin@skillswap.dev` | `Admin@Teste2026` | ADMIN (moderação, usuários, painel da plataforma) |
| `profissional@skillswap.dev` | `Usuario@Teste2026` | USER |
| `cliente@skillswap.dev` | `Usuario@Teste2026` | USER |

São senhas exclusivas de teste. No site publicado as senhas são definidas no servidor e informadas na entrega do AVA.

## Tecnologias

| Camada | Tecnologias |
| --- | --- |
| Frontend | Angular 20.3 com **NgModules** (`standalone: false`), roteamento com módulos carregados sob demanda, SSR desabilitado, **Angular Material** 20.2, TypeScript estrito, SCSS, Jasmine/Karma |
| Backend | Java 21, Spring Boot 4.0.8 (Web MVC, Data JPA, Validation, Security, Actuator), Flyway, Lombok, JJWT, Maven Wrapper |
| Banco | MySQL 8 (8.0.46 no servidor, 8.4 validado localmente); H2 apenas nos testes |
| Publicação | Docker Compose, Nginx (SPA) e Caddy (HTTPS) em VPS |
| Testes da API | Postman (coleção exportada em JSON) e testes de integração com MockMvc |

## Estrutura do repositório

```text
.
├── README.md
├── backend/                     # API Spring Boot (pom.xml, Maven Wrapper, Dockerfile)
│   └── src/main/java/com/skillswap/api/
│       ├── domain/<dominio>/    # entidades, repositories, services, controllers e DTOs por domínio
│       └── infra/               # segurança (HTTP Basic/JWT, perfis) e exceções (@RestControllerAdvice)
├── src/app/                     # Angular: AppModule, AppRoutingModule, SharedModule e módulos de funcionalidade
├── docs/
│   ├── documentacao-api.md      # endpoints, segurança, erros, regras e consultas
│   ├── requisitos-entrega.md    # requisito → onde está implementado
│   └── decisions/               # decisões de arquitetura (ADRs)
├── postman/
│   ├── projeto-integrador.postman_collection.json
│   └── gerar-colecao.mjs        # regenera a coleção contra uma API em execução
└── scripts/                     # banco local, backend, backups
```

### Arquitetura em camadas (backend)

`Controller` (recebe a requisição, valida com `@Valid`, devolve status HTTP) → `Service` (regras de negócio e transações) → `Repository` (Spring Data JPA) → `Entity` (JPA/MySQL). Os controllers não acessam repositories; as respostas usam DTOs (`record`).

Entidades principais: `Usuario`, `Anuncio`, `DisponibilidadeAnuncio`, `Conversa`, `Mensagem`, `Proposta`, `Acordo`, `EventoAcordo`, `Avaliacao`, `ComentarioAnuncio`, `PublicacaoPortfolio`, `ComentarioPublicacao`, `CurtidaPublicacao`, `ExperienciaProfissional`, `CertificacaoProfissional`, `Seguidor`, `Denuncia`, `ContestacaoAvaliacao` e `Notificacao`.

### Organização do Angular

- `AppModule` declara o componente raiz e importa `AppRoutingModule` (`RouterModule.forRoot`).
- Módulos de funcionalidade carregados sob demanda: `AnunciosModule`, `ContaModule`, `PerfilModule`, `NegociacaoModule`, `ModeracaoModule` e `PainelModule`.
- `SharedModule` exporta CommonModule, FormsModule, RouterModule, `MaterialModule` e componentes reutilizados; `IconesModule` reúne os ícones SVG locais.
- Componentes Material usados: botões, cards, formulários (form-field, input, select, checkbox), diálogos, menu lateral (sidenav) e barras de progresso.

## Como executar localmente

Pré-requisitos: JDK 21, Node.js 20.19+ / 22.12+ / 24.x com npm, Docker Desktop (para o MySQL) e PowerShell 7. Os testes do Angular precisam de Google Chrome.

1. Banco MySQL (gera `.env` com senhas locais e sobe o container em `127.0.0.1:3307`):

   ```powershell
   ./scripts/iniciar-banco.ps1
   ```

2. Backend em `http://127.0.0.1:8080` (aplica as migrações Flyway e cria as contas de teste):

   ```powershell
   ./scripts/iniciar-backend.ps1
   ```

3. Frontend em `http://localhost:4200` (encaminha `/api` ao backend pelo `proxy.conf.json`):

   ```powershell
   npm ci
   npm start
   ```

Configuração manual: copie `.env.example` para `.env` e defina `DB_PASSWORD`, `JWT_SECRET` (mín. 32 caracteres) e, se quiser, `DB_URL` e `DB_USER`. Veja também [docs/backend.md](docs/backend.md).

### Testes

| Comando | O que verifica |
| --- | --- |
| `cd backend; ./mvnw test` | 108 testes de integração (MockMvc + H2): CRUD, regras, 400/401/403/404/409, painel |
| `npm run test:ci` | Testes Jasmine/Karma do Angular em ChromeHeadless |
| `npm run typecheck` / `npm run build` | Tipos e build de produção (`dist/skillswap/browser`) |

### Postman

Importe [postman/projeto-integrador.postman_collection.json](postman/projeto-integrador.postman_collection.json). As variáveis (`baseUrl`, e-mails e senhas de teste, IDs) já vêm preenchidas; execute as pastas na ordem — os scripts de teste guardam os IDs criados. Cada requisição tem a resposta obtida salva. Para regenerar a coleção contra uma API com banco de teste:

```powershell
node postman/gerar-colecao.mjs http://127.0.0.1:8080 postman/projeto-integrador.postman_collection.json
```

## Documentação complementar

- [Documentação da API](docs/documentacao-api.md) e [requisitos da entrega](docs/requisitos-entrega.md)
- [Backend, operação e publicação](docs/backend.md)
- [Escopo do MVP](docs/escopo-mvp.md), [política de perfis](docs/politica-perfis.md) e [política de avaliações](docs/politica-avaliacoes.md)
- [Decisões de arquitetura](docs/decisions/), em especial a [ADR-009](docs/decisions/009-ngmodules-e-http-basic-entrega-final.md)
- [Histórico de desenvolvimento](docs/historico.md) e [diretrizes da disciplina](docs/diretrizes-projeto.md)
