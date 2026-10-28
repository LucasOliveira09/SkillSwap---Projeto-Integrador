# SkillSwap - Projeto Integrador

Plataforma de serviços e aulas locais, com venda direta ou troca de habilidades, desenvolvida pelo grupo Saptura. O foco inicial é Florianópolis e região, conforme o documento de visão fornecido pela equipe.

Stack definida pela equipe: **Angular 20 + Java com Spring Boot + MySQL**. O frontend está integrado ao backend adotado do GitHub para catálogo, criação de conta, login e cadastro autenticado de anúncios. A [ADR-004](docs/decisions/004-integra-backend-github.md) registra a integração de 21/10/2026, preservando o layout antigo.

Site: [skillswap.vizihub.com.br](https://skillswap.vizihub.com.br), atualizado em 21/10/2026 com o layout do primeiro SkillSwap e dados persistidos. A publicação preserva o MySQL ativo; operação e reversão estão em [backend.md](docs/backend.md#site-público).

Em 28/10/2026, a home foi reorganizada com atalhos, categorias e grades de contratação e troca (quatro colunas no desktop e duas no celular). As ofertas continuam vindo da API, com até 12 por página e paginação. Visitantes entram na conta antes de abrir o cadastro de anúncio e retornam ao formulário após o login. Foto e nome de anunciantes reais levam ao perfil com publicações e ofertas; o ícone da conta abre o próprio perfil quando autenticado. Exemplos continuam identificados, sem perfis fictícios navegáveis. A publicação web `05b1557` passou em 110 testes Angular, tipos, build e inspeção no Chromium; a API e o banco permaneceram ativos.

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
- Backend Java 21, Spring Boot 4.0.8, Maven Wrapper 3.10.0, JPA/Lombok e JWT. Flyway V1–V14 e Hibernate `validate` passaram em MySQL 8.0.46 e 8.4.11 descartáveis. Os fluxos REST completos para perfil, fotos/portfólio, descoberta, venda/troca, conversa, conclusão, avaliações e denúncias/moderação reversível de publicação passaram em MySQL 8.4.11; a foto também sobreviveu à recriação controlada da API com volume persistente. Os fluxos completos da interface e a moderação foram verificados em MySQL 8.0.46; em MySQL 8.4.11, Chromium percorreu cadastro, perfil, anúncio, proposta, conversa, conclusão mútua e avaliação. A migração do schema legado também passou numa cópia integral com zero divergências nos campos existentes. A integração local com MySQL 8.4.11 foi validada em 28/10/2026 no schema isolado `skillswap_validation`; o schema legado local e a cópia legada da prévia em 8088 continuam preservados. O schema público foi migrado até V14 em 28/10/2026, após autorização e backup verificado. Anúncios, perfil, portfólio, acordos e avaliações persistem pela API. A sessão fica apenas em memória e precisa de novo login após recarga. Categorias e coleções vêm da API; não há fallback em memória. Upload validado sustenta foto e capa profissionais e publicações; limpeza diária remove arquivos órfãos após 30 dias. Em 28/10, banco e uploads públicos foram copiados cifrados para a estação Windows e a restauração foi ensaiada em MySQL descartável; retenção definida em 30 cópias diárias e uma cópia de fim de mês por 12 meses, ainda manual e sem destino fora do local físico. Pagamentos integrados não estão disponíveis.
- A branch `codex/sincroniza-github-vps` contém perfis editáveis com título, capa, links HTTPS e pausa de visibilidade, portfólio com fotos, busca de profissionais, propostas/acordos, conversa e avaliações verificadas com moderação. A busca de profissionais filtra por termo, cidade, categoria, modalidade e faixa de preço em ofertas públicas ativas; os resultados informam quantidade e valores reais por categoria/modalidade. Em 28/10/2026, testes Java locais passaram (70 testes), `npm run test:ci` passou (105 testes), e `npm run typecheck` e `npm run build` passaram. A prévia Tailscale `http://100.97.254.9:8089` passou por validação ponta a ponta no MySQL 8.0.46: cadastro e oferta de teste retornaram 201, a busca encontrou o perfil e resumiu o preço; o perfil de teste foi pausado ao final. Chromium 149 conferiu a busca em 390 e 1440 px sem erros de console, página ou rede. A prévia usa API `5244c72`, web `b7be2d5` e MySQL 8.0.46; as imagens anteriores foram preservadas para rollback, o banco e seus volumes permaneceram ativos e há 37 GB livres. Domínio público, Caddy, proxy e banco público não foram alterados. Pagamentos integrados ficam para fase posterior. O plano detalhado está em [tasks/plan.md](tasks/plan.md). A prévia é isolada e usa banco e volumes próprios.
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
- [Política de perfis e privacidade](docs/politica-perfis.md) e [política de avaliações](docs/politica-avaliacoes.md): campos públicos, controles existentes e limites da moderação.
- [Diretrizes da disciplina](docs/diretrizes-projeto.md) e [PDFs originais](docs/referencias/README.md).
- [Decisão de arquitetura](docs/decisions/001-angular-standalone.md), [plano](tasks/plan.md) e [tarefas](tasks/todo.md).
- [Validação da entrega](docs/validacao-inicial.md) e [contexto para agentes](AGENTS.md).
- [Layout do projeto antigo e validação visual](docs/layout-legado.md): referência, assets, adaptações e conferências.

O backend fica em `backend/src/main/java/com/skillswap/api/`, organizado por domínio em `domain`, com controllers, services, repositories, entidades e DTOs; `infra` concentra segurança e exceções. Essa organização substituiu a primeira aplicação conforme a escolha do usuário de adotar o backend novo do GitHub.

Próximas prioridades: completar a revisão manual com leitor de tela e contraste; replicar o backup cifrado para outro local/provedor e automatizar a retenção; e decidir como sincronizar o schema ativo antes de eventual promoção pública. Em MySQL 8.4.11, os fluxos REST completos passaram e Chromium percorreu cadastro, perfil, anúncio, proposta, conversa, conclusão mútua e avaliação na prévia isolada da VPS. A integração local também passou em schema separado; Chromium percorreu uma venda completa até a avaliação verificada, e os dados sintéticos foram pausados ao final. A revisão de outras telas autenticadas e a acessibilidade manual seguem pendentes. O backup público foi copiado cifrado para esta estação e restaurado em MySQL descartável. Consulte [backend.md](docs/backend.md) para contrato, execução e operação na VPS.

Criar commits por objetivo, revisar o diff e executar as verificações pertinentes antes de cada commit. Nunca versionar segredos, dependências instaladas, cache ou build.
