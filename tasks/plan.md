# Plano de inicialização

Pedido: iniciar Angular pelo terminal com standalone e preservar as orientações arquiteturais do professor. Escopo de domínio em [escopo-mvp.md](../docs/escopo-mvp.md); decisões em [ADR-001](../docs/decisions/001-angular-standalone.md).

1. Preservar o novo PDF e registrar escopo, decisões e critérios antes de incorporar a aplicação.
2. Gerar a aplicação pela CLI em diretório temporário e copiar a base para a raiz, preservando README, AGENTS e documentação existentes. Instalar dependências, compilar e testar; criar commit de referência antes do Material.
3. Executar o instalador de Material compatível e conferir tema e providers. Criar rotas por domínio e um fluxo de cadastro em memória com catálogo, sem apresentar dados como publicados.
4. Verificar regras, formulário e rotas em testes, build e navegador em larguras estreitas e desktop. Atualizar instruções executáveis e registrar commits por objetivo.

Dependências: referência e arquitetura -> workspace validado -> Material -> fluxo demonstrativo -> validação final. Não há trabalho paralelo necessário.

Comandos de uso previstos: `npm ci`, `npm start`, `npm run build`, `npm run test:ci`, `npm run typecheck`. Registrar no README os comandos exatos de geração e a versão efetivamente instalada. Não anunciar lint enquanto não houver ferramenta configurada.

Convenções: arquivos em kebab-case com sufixos `.component.ts` e `.service.ts`, classes descritivas em PascalCase, membros em camelCase, duas posições de indentação. Testes `.spec.ts` junto ao comportamento testado. Nunca versionar segredos, `node_modules`, cache ou build. Decisões de backend e integrações externas serão tratadas na próxima etapa, conforme exigências da equipe.
