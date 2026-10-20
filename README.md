# SkillSwap - Projeto Integrador

Plataforma de serviços e aulas locais, com venda direta ou troca de habilidades, desenvolvida pelo grupo Saptura. O foco inicial é Florianópolis e região, conforme o documento de visão fornecido pela equipe.

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
- Catálogo vazio inicialmente e cadastro de serviços/aulas com validação, venda ou troca e ação Limpar.
- Demonstração em memória: anúncios aparecem durante a navegação e desaparecem ao recarregar a página. Não há publicação real, backend, autenticação, upload, chat, avaliações ou pagamentos nesta etapa.
- Interface em português, preços em reais, navegação por teclado e layout responsivo. Fontes do sistema, sem carregamento externo de fontes ou ícones.

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
```

Os testes `.spec.ts` ficam junto ao comportamento que verificam. Imports de Material e `FormsModule` ficam nos componentes que os utilizam. Não há `AppModule` ou `declarations`; a [ADR-001](docs/decisions/001-angular-standalone.md) explica a adaptação dos exemplos do professor para standalone. Pastas compartilhadas serão criadas quando houver reutilização real.

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

O instalador criou o tema com `mat.theme`. Mantivemos um tema único, ajustamos a tipografia para fonte do sistema e removemos links de fontes externas. O instalador desta versão não adicionou providers de animação; nenhuma configuração antiga foi duplicada. As páginas foram geradas com `ng generate component`, seguindo o comando acima, com testes de comportamento escritos separadamente.

## Documentação e continuidade

- [Escopo do MVP](docs/escopo-mvp.md): problema, público, seis capacidades planejadas e decisões em aberto.
- [Diretrizes da disciplina](docs/diretrizes-projeto.md) e [PDFs originais](docs/referencias/README.md).
- [Decisão de arquitetura](docs/decisions/001-angular-standalone.md), [plano](tasks/plan.md) e [tarefas](tasks/todo.md).
- [Validação da entrega](docs/validacao-inicial.md) e [contexto para agentes](AGENTS.md).

Próximo passo: definir com a equipe as exigências de backend e banco da disciplina e o contrato de contas, perfis e anúncios. Depois, substituir a simulação por persistência e autorização reais, preservando as regras e a cobertura de testes.

Criar commits por objetivo, revisar o diff e executar as verificações pertinentes antes de cada commit. Nunca versionar segredos, dependências instaladas, cache ou build.
