# Tarefas

- [x] Incorporar PDF, definição do MVP e ADR standalone. Verificar integridade e links locais.
- [x] Criar workspace pela CLI com roteamento, SCSS, strict e testes. Verificar build e teste gerado antes do Material.
- [x] Instalar Material 20 pelo terminal. Verificar configuração e dependências compatíveis.
- [x] Criar serviço demonstrativo e regras de anúncio; cobrir campos vazios, venda e troca com testes.
- [x] Criar cadastro Material e catálogo com estado vazio e aviso de simulação. Verificar salvar, limpar e navegação.
- [x] Validar build, tipos, testes e navegador; atualizar README, AGENTS, diretrizes e comandos reproduzíveis.
- [x] Revisar alterações e criar commits, deixando o repositório sem pendências.

O MVP completo e as decisões em aberto estão em [escopo-mvp.md](../docs/escopo-mvp.md); não fazem parte da conclusão desta inicialização.

## Visão de rede profissional + marketplace (plano aprovado em 26/10/2026)

O plano completo e a definição de pronto estão em [plan.md](plan.md). Pagamentos ficam para fase posterior. Os itens abaixo acompanham a implementação; código parcial já existente deve ser revisado e completado, não tratado como funcionalidade pronta.

### Fundação e perfis

- [x] Revisar detalhe existente, corrigir retorno de login para rota interna permitida e remover promessas sem suporte real da tela de anúncio.
- [x] Fechar regras de perfil público, privacidade, avaliações, conclusão de acordo e moderação, com as limitações documentadas.
- [x] Adotar Flyway, baseline SQL para banco vazio e Hibernate em modo validate; testar o baseline contra H2/MySQL mode.
- [x] Executar Flyway V1–V14 e Hibernate `validate` em schema vazio e descartável MySQL 8.0.46, usando a aplicação Java 21.
- [ ] Reexecutar a preparação/baseline com a versão atual do script guardado em uma cópia MySQL 8.0.46: uma versão anterior já passou em clone somente de estrutura e Flyway V1–V14/Hibernate `validate`, mas a versão atual ainda só foi validada no caminho negativo (guard `0`, sete tabelas da prévia inalteradas). Repetir o caminho positivo antes de considerar o artefato pronto.
- [ ] Fazer backup verificável e ensaiar a preparação/baseline numa cópia integral do schema e dos uploads existentes; conferir contagens, valores e referências antes de qualquer decisão sobre a base ativa. A inspeção original encontrou sete tabelas sem histórico Flyway; a V1 cria nove, há cinco colunas `ENUM` e falta `avaliacoes.nota_anuncio`.
- [ ] Validar V1–V14 e Hibernate `validate` em MySQL 8.4 local antes de promoção; Docker Desktop precisa estar ativo.
- [x] Validar conteúdo real de upload, dimensões e tamanho; associar upload de foto ao perfil autenticado e declarar volume persistente no Compose.
- [x] Limpar uploads sem referência após período de retenção configurável, preservando fotos ligadas a perfis, anúncios e publicações.
- [x] Aplicar quota de 100 MiB por conta, endpoint autenticado de uso, backfill de imagens referenciadas e remoção dos registros junto à limpeza dos órfãos.
- [x] Criar rotina para gerar arquivo de backup somente leitura do volume de uploads, validar tar e SHA-256, sem alterar o volume.
- [ ] Fazer primeira cópia verificada, definir retenção externa e ensaiar restauração; migrar os arquivos públicos atuais e validar persistência após reinício.
- [x] Revogar acesso público à imagem quando a última referência pública for removida; uploads órfãos ficam privados do proprietário e sem cache HTTP.
- [x] Criar API pública e API autenticada para ler/editar o próprio perfil sem expor e-mail, além de contar avaliações reais.
- [x] Criar páginas de perfil público e edição, listar apenas ofertas ativas reais e ligar perfil a conta, catálogo e anúncio.
- [x] Permitir prévia, envio e remoção da foto profissional por fluxo autenticado.
- [x] Adicionar título profissional opcional ao perfil, apresentá-lo publicamente e incluí-lo na busca.
- [x] Adicionar capa opcional ao perfil, com upload autenticado, remoção, exibição pública e retenção do arquivo referenciado.
- [x] Adicionar links públicos opcionais de LinkedIn, Instagram e site HTTPS, com validação no servidor.
- [x] Permitir pausar e reativar a exposição do perfil, ofertas, portfólio, avaliações e imagens sem apagar dados da conta ou negociações.
- [x] Validar perfil, foto e portfólio no MySQL 8.0.46 e persistência após reinício da API, em schema descartável da prévia Tailscale.
- [ ] Repetir validação no MySQL 8.4 local; Docker Desktop precisa estar ativo.

### Portfólio e descoberta

- [x] Criar publicações persistidas com legenda, até seis fotos, vínculo opcional a oferta própria; mostrar no perfil e permitir remoção pelo autor.
- [x] Permitir editar legenda de publicação e navegar pelas páginas do portfólio público e próprio.
- [x] Denunciar e moderar publicações/avaliações com acesso por allowlist, trilha de evidência e ocultação reversível.
- [x] Ligar anúncios ao perfil e associar ofertas às publicações.
- [x] Editar, pausar e reativar anúncios próprios com leitura autenticada, validação do proprietário e persistência pela API.
- [x] Buscar profissionais por nome/especialidade e cidade com paginação, perfil público e reputação apenas quando há avaliações reais.
- [x] Filtrar ofertas do catálogo por cidade, modalidade e faixa de preço combinadas, preservando trocas sem preço.
- [x] Descobrir ofertas por disponibilidade declarada.
- [x] Harmonizar detalhe do anúncio e perfil com a identidade do primeiro SkillSwap; validar estados vazios/erro e navegação de retorno.

### Negociação e reputação

- [x] Serializar criação/aceite/recusa/cancelamento de propostas por anúncio; permitir cancelamento da própria proposta pendente.
- [x] Registrar histórico de eventos da negociação.
- [x] Validar aceite concorrente com MySQL 8.0.46 real: o cenário original reproduziu dois HTTP 200 para o mesmo anúncio; após a correção, requisições simultâneas resultaram em um HTTP 200, um HTTP 400 e apenas um acordo.
- [x] Criar área de propostas/acordos com erros visíveis, carregamento, nova tentativa e estados vazios.
- [x] Persistir mensagens por acordo, validar participantes e limitar consultas com paginação na API.
- [x] Adicionar navegação de páginas à conversa, abrir nas mensagens recentes e testar histórico extenso.
- [x] Coletar e calcular nota separada para oferta/serviço após acordo concluído; exibir média/amostra no anúncio e avaliações no perfil.
- [x] Denunciar e moderar avaliações sem apagar evidências e sem agregar notas ocultas.
- [x] Permitir resposta pública, única e imutável do profissional a uma avaliação verificada.
- [x] Implementar edição única da avaliação pelo autor em até 24 horas, preservando versões para moderação e bloqueando alteração após resposta ou denúncia/contestação.
- [x] Registrar a política de edição e contestação: prazo de sete dias, conteúdo privado, manutenção pública durante análise, decisão por allowlist e remoção das notas públicas apenas quando ocultada.
- [x] Implementar contestação privada pelo profissional avaliado em até sete dias, com decisão fundamentada por moderador, fila de análise e preservação de evidências.
- [x] Conectar conclusão do acordo a avaliação elegível do participante; validar ponta a ponta da API.
- [x] Validar pela API os fluxos de venda e troca, avaliações do profissional/serviço e persistência após reinício em MySQL 8.0.46 com Java 21.
- [x] Validar pela interface Angular, contra MySQL 8.0.46 descartável, perfil, foto/portfólio, busca, venda, troca, proposta, aceite, conversa, conclusão mútua e avaliações. Foram usados dois participantes; não houve erros de console ou overflow em 360 px/1440 px, e os dados persistiram após reinício da API.
- [ ] Complementar com verificação de acessibilidade por axe/leitor de tela e repetir em MySQL 8.4.

### Entrega e fases seguintes

- [ ] Completar validação operacional: concorrência, MySQL 8.4, moderação em MySQL, axe/leitor de tela, backup/restauração de uploads e migração segura do schema/arquivos existentes. Os fluxos de venda e troca na interface, autorização básica, persistência de perfil/foto/publicações/avaliações e reinício da API passaram em MySQL 8.0.46.
- [ ] Fazer piloto regional com dados reais consentidos; definir métricas após linha de base observada.
- [ ] Planejar pagamentos integrados em fase seguinte: selecionar provedor e operar sandbox antes de dinheiro real.
- [ ] Só promover a versão pública após revisão, backup, validação em cópia isolada e plano de reversão.

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
- [x] Implementar telas e testes dos módulos de propostas, acordos, mensagens e avaliações.
- [x] Filtrar profissionais por média mínima apenas com amostra de três avaliações visíveis, mantendo a ordenação estável por data.
- [x] Adotar migrações Flyway versionadas (V1–V14).
- [ ] Validar MySQL e preparar sincronização antes de eventual promoção pública.

## Retorno ao layout do primeiro SkillSwap e consistência dos exemplos

- [x] Comparar o HTML/CSS original no navegador e restaurar detalhes visuais dos cards, banner, coleções e acesso.
- [x] Acrescentar oito exemplos via backend, com modalidades, valores, localidades e identificação de demonstração coerentes.
- [x] Manter carga idempotente e preservar anúncios existentes.
- [x] Validar 28 testes Angular, 14 testes Java, build, tipos e responsividade.
- [x] Publicar no domínio indicado pelo usuário, preservando banco ativo, proxy e registros existentes, com backup e reversão.
