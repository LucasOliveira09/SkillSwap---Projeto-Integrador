# SkillSwap - Projeto Integrador

Plataforma de serviços e aulas locais, com venda direta ou troca de habilidades, desenvolvida pelo grupo Saptura. O foco inicial é Florianópolis e região, conforme o documento de visão fornecido pela equipe.

Stack definida pela equipe: **Angular 20 + Java com Spring Boot + MySQL**. O frontend está implementado como demonstração; backend e banco serão iniciados na próxima etapa, conforme a [ADR-002](docs/decisions/002-stack-backend-banco.md).

## Executar localmente

Pré-requisitos: Node.js 20.19+, 22.12+ ou 24.x (ambiente validado: 24.13.1); npm (validado: 11.8.0). Os testes precisam de Google Chrome instalado ou de `CHROME_BIN` apontando para Chromium compatível.

```powershell
npm ci
npm start
```

Abrir [http://localhost:4200](http://localhost:4200). A raiz redireciona para `/anuncios`; o cadastro fica em `/anuncios/novo`. Endereços desconhecidos retornam ao catálogo.

## Estado da entrega

- Angular 20.3.34, CLI 20.3.40 e Material/CDK 20.2.14, com versões exatas no lockfile.
- Componentes standalone, SCSS, TypeScript e templates estritos, rotas por domínio com lazy loading.
- Catálogo com o layout do projeto antigo, exemplos ilustrativos, busca por URL, filtros, banner e paginação. Cadastro de serviços/aulas com prévia, validação, venda ou troca e ação Limpar.
- Demonstração em memória: os anúncios criados aparecem durante a navegação e desaparecem ao recarregar; os exemplos visuais continuam disponíveis. Não há publicação real, backend, autenticação, upload, chat, avaliações ou pagamentos nesta etapa.
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
      anuncios/
        anuncios.routes.ts         # rotas /anuncios e /anuncios/novo
        models/                    # tipos e validação do anúncio
        data-access/               # serviço demonstrativo em memória
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

- [Escopo do MVP](docs/escopo-mvp.md): problema, público, seis capacidades planejadas e decisões em aberto.
- [Diretrizes da disciplina](docs/diretrizes-projeto.md) e [PDFs originais](docs/referencias/README.md).
- [Decisão de arquitetura](docs/decisions/001-angular-standalone.md), [plano](tasks/plan.md) e [tarefas](tasks/todo.md).
- [Validação da entrega](docs/validacao-inicial.md) e [contexto para agentes](AGENTS.md).
- [Layout do projeto antigo e validação visual](docs/layout-legado.md): referência, assets, adaptações e conferências.

Próximo passo: iniciar o backend Java com Spring Boot e MySQL, definindo as versões e o contrato de contas, perfis e anúncios. Depois, substituir a simulação por persistência e autorização reais, preservando as regras e a cobertura de testes.

Criar commits por objetivo, revisar o diff e executar as verificações pertinentes antes de cada commit. Nunca versionar segredos, dependências instaladas, cache ou build.
