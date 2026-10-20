# Validação da inicialização

Entrega de 19/10/2026. Escopo: workspace Angular standalone e cadastro/catálogo demonstrativos em memória. O MVP completo permanece planejado em [escopo-mvp.md](escopo-mvp.md).

## Verificações técnicas

- Base gerada com Angular CLI 20.3.40 pelo terminal, com `--standalone`, `--routing`, `--strict` e SCSS. Build e dois testes gerados passaram antes da instalação do Material.
- Regras do serviço, formulário e rotas tiveram testes executados em falha antes de sua implementação. Suíte final: 19 testes Jasmine/Karma no ChromeHeadless.
- Build de produção e checagem de tipos da aplicação/testes passaram. O build emite chunks separados para as rotas e as páginas.
- Instalação reproduzível com `npm ci --ignore-scripts` verificada, seguida de build e testes. `.npmrc` mantém scripts de instalação bloqueados.
- PDFs preservados, links da documentação conferidos e `git diff --check` sem erros. Lint não foi configurado nesta entrega.

## Conferência em navegador real

Chrome headless controlado por Playwright em perfil isolado, contra o servidor local. Ferramentas de inspeção instaladas fora do repositório para esta verificação; os testes de regressão da aplicação permanecem em `.spec.ts`.

1. Abrir a raiz e conferir redirecionamento para catálogo vazio.
2. Abrir cadastro; verificar bloqueio com campos vazios e título composto somente por espaços.
3. Preencher e cadastrar venda; conferir feedback explícito de simulação e preço formatado em reais no catálogo.
4. Cadastrar troca com habilidade desejada; conferir bloqueio enquanto ela estiver ausente e exibição correta no catálogo.
5. Limpar; conferir campos, mensagem e botão de envio reiniciados.
6. Navegar sem recarregar: anúncios permanecem. Recarregar: catálogo volta ao estado vazio.
7. Abrir URL desconhecida: retornar ao catálogo.
8. Conferir catálogo e cadastro em 320, 768, 1024 e 1440 px, sem rolagem horizontal; capturas desktop/mobile inspecionadas visualmente.
9. Navegar por teclado e usar “Pular para o conteúdo”; conferir foco no conteúdo principal.

Console sem erros ou avisos na execução dos fluxos. axe-core não encontrou violações nas verificações automatizadas WCAG 2 A/AA e 2.1 A/AA das duas páginas iniciais. Isso complementa, mas não substitui, uma avaliação manual completa de acessibilidade.

## Dependências e limites

Auditoria das dependências de produção: zero vulnerabilidades. A auditoria completa registra seis entradas altas derivadas do achado conhecido em `braces` na cadeia de testes; análise, mitigação e data de revisão em [dependencias.md](dependencias.md).

Ainda não foram implementados backend, autorização, armazenamento permanente, upload, chat, contratação, avaliações ou pagamentos. A aceitação acadêmica da explicação de imports e bindings depende da apresentação pela equipe.
