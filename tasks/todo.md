# Tarefas

- [x] Incorporar PDF, definição do MVP e ADR standalone. Verificar integridade e links locais.
- [x] Criar workspace pela CLI com roteamento, SCSS, strict e testes. Verificar build e teste gerado antes do Material.
- [x] Instalar Material 20 pelo terminal. Verificar configuração e dependências compatíveis.
- [x] Criar serviço demonstrativo e regras de anúncio; cobrir campos vazios, venda e troca com testes.
- [x] Criar cadastro Material e catálogo com estado vazio e aviso de simulação. Verificar salvar, limpar e navegação.
- [x] Validar build, tipos, testes e navegador; atualizar README, AGENTS, diretrizes e comandos reproduzíveis.
- [x] Revisar alterações e criar commits, deixando o repositório sem pendências.

O MVP completo e as decisões em aberto estão em [escopo-mvp.md](../docs/escopo-mvp.md); não fazem parte da conclusão desta inicialização.

## Adaptação do layout antigo

- [x] Inspecionar o repositório indicado e comparar catálogo/cadastro no navegador.
- [x] Incorporar identidade, imagens otimizadas, fontes locais e licenças.
- [x] Adaptar cabeçalho, menu, rodapé, banner, cards e coleções em componentes standalone.
- [x] Manter exemplos identificados, implementar busca/filtros e preservar anúncios da sessão.
- [x] Aplicar identidade ao cadastro e incluir prévia sem alterar as regras do serviço.
- [x] Conferir testes, build, tipos, responsividade, teclado e acessibilidade; atualizar documentação e registrar commits.

## Backend e persistência

- [x] Inspecionar a referência local de eventos e registrar contrato/padrões.
- [x] Criar base Java 21, Spring Boot 4.1.1, Maven Wrapper e MySQL com credenciais fora do Git.
- [x] Criar entidades, repositórios, serviços, DTOs e controllers de catálogo/cadastro com validação e paginação.
- [x] Mover anúncios ilustrativos e coleções para carga inicial idempotente no banco.
- [x] Integrar Angular à API, remover estado em memória e tratar carregamento/falha/repetição.
- [x] Concluir persistência após reinício, revisão final, documentação e commits.

## Integração do backend novo do GitHub (21/10/2026)

- [x] Adotar a base escolhida pelo usuário e preservar o layout Angular.
- [x] Integrar conta, JWT e cadastro autorizado; manter tokens apenas em memória.
- [x] Compatibilizar enums, paginação, busca, erros e metadados da vitrine.
- [x] Preservar repositório e banco da VPS; restaurar backup transacional em stack separada pelo Tailscale.
- [x] Corrigir índice dos anúncios existentes e enum de categoria no MySQL 8.0 sem atribuir proprietários ou substituir dados.
- [x] Validar 28 testes Angular, 13 testes Java, build, tipos e fluxo real no navegador/MySQL.
- [x] Conferir catálogo, conta e formulário em quatro larguras e com axe.
- [ ] Implementar telas e testes dos módulos de propostas, acordos, mensagens e avaliações.
- [ ] Adotar migrações versionadas e preparar sincronização dos dados antes de eventual promoção pública.

## Retorno ao layout do primeiro SkillSwap e consistência dos exemplos

- [x] Comparar o HTML/CSS original no navegador e restaurar detalhes visuais dos cards, banner, coleções e acesso.
- [x] Acrescentar oito exemplos via backend, com modalidades, valores, localidades e identificação de demonstração coerentes.
- [x] Manter carga idempotente e preservar anúncios existentes.
- [x] Validar 28 testes Angular, 14 testes Java, build, tipos e responsividade.
- [x] Publicar no domínio indicado pelo usuário, preservando banco ativo, proxy e registros existentes, com backup e reversão.
