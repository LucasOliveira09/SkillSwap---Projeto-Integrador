# ADR-001: Base Angular standalone com Angular Material

Status: aceita para esta inicialização, conforme pedido do usuário em 19/10/2026.

## Contexto

O material do professor ensina separação por domínio, componentes e rotas com exemplos NgModules. O guia de Material também descreve a adaptação standalone. O usuário pediu explicitamente `--standalone` e criação pelo terminal.

## Decisão

- Angular/CLI 20.3 e Material/CDK 20.2, com patches resolvidos no `package-lock.json`. A linha 20 mantém proximidade com o exemplo do guia e é compatível com o Node 24.13.1 encontrado no ambiente.
- SPA no navegador, sem SSR; TypeScript estrito, SCSS e componentes standalone com `standalone: true` explícito para facilitar a leitura em aula.
- Bootstrap via `bootstrapApplication`, providers em `app.config.ts`, rotas em `app.routes.ts`; áreas de negócio carregadas por `loadChildren` e páginas por `loadComponent`.
- Sem `AppModule`, `declarations` ou módulos de funcionalidade artificiais. A organização por domínio é preservada por pastas e arquivos de rotas.
- `FormsModule`/`ngModel` no cadastro para acompanhar o guia do professor. Material importado somente nos componentes consumidores.
- Serviços específicos permanecem no domínio; recursos compartilhados serão extraídos apenas quando houver reutilização real. Sem store global ou camada genérica de repositórios nesta etapa.
- Testes Jasmine/Karma gerados pela CLI, executados com ChromeHeadless; lockfile versionado e instalação reproduzível com `npm ci`.
- O componente raiz mantém os nomes concisos `app.ts`, `app.html` e `app.scss` gerados pela CLI 20. Nas páginas, usar `--type=component` para explicitar o papel dos arquivos, como nos exemplos de aula.
- Backend, autenticação e persistência ficam para uma entrega posterior. O serviço demonstrativo em memória terá nome e mensagens explícitos de simulação.

## Consequências

Os conceitos do professor permanecem, mas os pontos de configuração diferem dos exemplos NgModules. Não copiar `imports` para um módulo inexistente. A integração futura substituirá o serviço demonstrativo e precisará validar os mesmos dados no servidor e autorizar operações.

## Fontes oficiais consultadas

- [CLI 20: ng new e opções standalone/routing/strict](https://v20.angular.dev/cli/new).
- [Compatibilidade de Angular, Node e TypeScript](https://angular.dev/reference/versions).
- [Rotas, lazy loading e componentes standalone](https://v20.angular.dev/guide/routing/define-routes).
- [Formulários com FormsModule e ngModel](https://v20.angular.dev/guide/forms/template-driven-forms).
- [Instalação do Angular Material 20](https://v20.material.angular.dev/guide/getting-started).
