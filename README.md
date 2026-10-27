# SkillSwap - Projeto Integrador

Plataforma de serviços e aulas locais, com venda direta ou troca de habilidades, desenvolvida pelo grupo Saptura. O foco inicial é Florianópolis e região, conforme o documento de visão fornecido pela equipe.

Stack definida pela equipe: **Angular 20 + Java com Spring Boot + MySQL**. O frontend está integrado ao backend adotado do GitHub para catálogo, criação de conta, login e cadastro autenticado de anúncios. A [ADR-004](docs/decisions/004-integra-backend-github.md) registra a integração de 21/10/2026, preservando o layout antigo.

Site: [skillswap.vizihub.com.br](https://skillswap.vizihub.com.br), atualizado em 21/10/2026 com o layout do primeiro SkillSwap e dados persistidos. A publicação preserva o MySQL ativo; operação e reversão estão em [backend.md](docs/backend.md#site-público).

## Executar localmente

Pré-requisitos: PowerShell 7, Docker Desktop ativo, JDK 21 e Node.js 20.19+, 22.12+ ou 24.x (ambiente validado: 24.13.1); npm (validado: 11.8.0). Os testes precisam de Google Chrome instalado ou de `CHROME_BIN` apontando para Chromium compatível.

Na raiz, iniciar o banco e o backend:

```powershell
./scripts/iniciar-banco.ps1
./scripts/iniciar-backend.ps1
```

Em outro terminal, na mesma pasta:

```powershell
npm ci
npm start
```

Abrir [http://localhost:4200](http://localhost:4200). A raiz redireciona para `/anuncios`; o cadastro fica em `/anuncios/novo`. Endereços desconhecidos retornam ao catálogo. A API fica em `http://127.0.0.1:8080/api` e o MySQL em `127.0.0.1:3307`; o Angular encaminha `/api` pelo proxy local. Os scripts geram credenciais em `.env`, ignorado pelo Git, e mantêm o banco em volume Docker. Consulte [backend.md](docs/backend.md) para configuração manual, testes e operação.

## Estado da entrega

- Angular 20.3.34, CLI 20.3.40 e Material/CDK 20.2.14, com versões exatas no lockfile.
- Componentes standalone, SCSS, TypeScript e templates estritos, rotas por domínio com lazy loading.
- Catálogo com o layout do primeiro SkillSwap, exemplos ilustrativos vindos do banco, busca por URL, filtros, banner e paginação no servidor. Carga idempotente com 13 exemplos, incluindo oito novos serviços/aulas em SC, de venda ou troca e sem notas fictícias nos novos perfis. Cadastro com prévia, validação no cliente e servidor e ação Limpar.
- Backend Java 21, Spring Boot 4.0.8, Maven Wrapper 3.10.0, JPA/Lombok e JWT. MySQL local 8.4; a prévia na VPS mantém 8.0 em uma base restaurada separada. Anúncios persistem após recarregar e reiniciar. A sessão fica apenas em memória e precisa de novo login após recarga. Categorias e coleções vêm da API; não há fallback em memória. Upload validado sustenta a foto profissional e as publicações; falta validar a nova persistência com MySQL e concluir quotas/limpeza de arquivos. Pagamentos integrados não estão disponíveis. Negociações, mensagens e avaliações precisam de telas e validação integral.
- A branch `codex/sincroniza-github-vps` agora tem perfis públicos/editáveis, portfólio de fotos, busca de profissionais, área de propostas/acordos, conversa e avaliação do participante após conclusão. Fotos e publicações passam pela API; ainda faltam validação MySQL, quotas/limpeza e moderação. Nota separada para ofertas e regras avançadas de negociação também seguem pendentes. Pagamentos integrados ficam para fase posterior. O plano detalhado está em [tasks/plan.md](tasks/plan.md); estes commits não foram publicados no site público.
- Interface em português, preços em reais, navegação por teclado e layout responsivo. Fontes Poppins e Angkor locais com licenças OFL; ícones SVG locais, sem CDNs.

## Comandos

| Comando | Finalidade |
| --- | --- |
| `npm ci` | Instalar exatamente as dependências do lockfile; scripts de instalação bloqueados em `.npmrc`. |
| `npm start` | Servidor local de desenvolvimento. |
| `npm run build` | Build de produção em `dist/skillswap/browser`. |
| `npm test` | Testes Jasmine/Karma em modo de observação. |
| `npm run test:ci` | Executar testes uma vez no ChromeHeadless. |
| `npm run typecheck` | Checar tipos da aplicação e dos testes sem emitir arquivos. |
| `npm audit --omit=dev` | Auditar dependências de produção. |

Lint ainda não está configurado. O build também verifica templates Angular. Consulte o [achado conhecido nas ferramentas de teste](docs/dependencias.md) antes de interpretar o resultado completo de `npm audit`; não usar `--force` para tratar conflitos.

## Arquitetura

```text
src/
  main.ts                          # bootstrapApplication
  app/
    app.ts / app.html / app.scss    # estrutura e navegação principal
    app.config.ts                  # providers e locale pt-BR
    app.routes.ts                  # entrada das áreas funcionais
    features/
      conta/                       # sessão em memória, interceptor e tela de acesso
      anuncios/
        anuncios.routes.ts         # rotas /anuncios e /anuncios/novo
        models/                    # tipos e validação do anúncio
        data-access/               # cliente HTTP da API
        pages/                     # catálogo e cadastro standalone
        ui/                        # banner, cards e coleções visuais
    shared/ui/                     # ícones e diálogo usados em várias telas
```

Os testes `.spec.ts` ficam junto ao comportamento que verificam. Imports de Material e `FormsModule` ficam nos componentes que os utilizam. Não há `AppModule` ou `declarations`; a [ADR-001](docs/decisions/001-angular-standalone.md) explica a adaptação dos exemplos do professor para standalone. Recursos compartilhados são extraídos quando há reutilização real.

Para novos componentes, usar a CLI local:

```powershell
npm exec -- ng generate component features/nome-do-dominio/pages/nome-da-tela --standalone --style=scss --type=component
```

## Criação pelo terminal

A CLI foi executada em pasta temporária para preservar este README e os documentos existentes, depois a base foi copiada para a raiz, exceto README e configurações de editor. Comando executado a partir da pasta temporária:

```powershell
npx --yes @angular/cli@20.3.40 new skillswap --directory skillswap-angular-bootstrap --standalone --routing --style=scss --strict --ssr=false --zoneless=false --skip-git --skip-install --package-manager=npm --defaults --interactive=false
```

Após instalar, compilar, testar e criar o commit da base, foi executado na raiz:

```powershell
npm exec -- ng add @angular/material@20.2.14 --theme=azure-blue --typography=true --animations=enabled --skip-confirmation --interactive=false
```

O instalador criou o tema com `mat.theme`. Na adaptação do layout antigo, esse tema único recebeu as cores azul/verde e fontes locais com os mecanismos de customização do Material. O instalador desta versão não adicionou providers de animação; nenhuma configuração antiga foi duplicada. Páginas e componentes visuais foram gerados com `ng generate component`, seguindo o comando acima, com testes de comportamento escritos separadamente.

## Documentação e continuidade

- [Backend e contrato HTTP](docs/backend.md): execução, banco, arquitetura e validação.
- [Escopo do MVP](docs/escopo-mvp.md): problema, público, seis capacidades planejadas e decisões em aberto.
- [Diretrizes da disciplina](docs/diretrizes-projeto.md) e [PDFs originais](docs/referencias/README.md).
- [Decisão de arquitetura](docs/decisions/001-angular-standalone.md), [plano](tasks/plan.md) e [tarefas](tasks/todo.md).
- [Validação da entrega](docs/validacao-inicial.md) e [contexto para agentes](AGENTS.md).
- [Layout do projeto antigo e validação visual](docs/layout-legado.md): referência, assets, adaptações e conferências.

O backend fica em `backend/src/main/java/com/skillswap/api/`, organizado por domínio em `domain`, com controllers, services, repositories, entidades e DTOs; `infra` concentra segurança e exceções. Essa organização substituiu a primeira aplicação conforme a escolha do usuário de adotar o backend novo do GitHub.

Próximo passo: implementar as telas dos módulos de negociação, aprofundar seus testes e adotar migrações versionadas nas próximas entregas. A base atual possui contas, JWT e autorização de anúncios por proprietário. Consulte [backend.md](docs/backend.md) para contrato, execução e operação na VPS.

Criar commits por objetivo, revisar o diff e executar as verificações pertinentes antes de cada commit. Nunca versionar segredos, dependências instaladas, cache ou build.
