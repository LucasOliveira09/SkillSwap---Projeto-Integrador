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

- [x] Em 28/10, corrigir a regra Spring Security que deixava os GETs `/api/anuncios/meus` e `/api/anuncios/meus/{id}` caírem no matcher público abrangente; ambos agora exigem autenticação. Testes de regressão confirmam HTTP 401 sem JWT; suíte Java completa: 56 testes, sem falhas ou erros.

O plano completo e a definição de pronto estão em [plan.md](plan.md). Pagamentos ficam para fase posterior. Os itens abaixo acompanham a implementação; código parcial já existente deve ser revisado e completado, não tratado como funcionalidade pronta.

### Fundação e perfis

- [x] Revisar detalhe existente, corrigir retorno de login para rota interna permitida e remover promessas sem suporte real da tela de anúncio.
- [x] Fechar regras de perfil público, privacidade, avaliações, conclusão de acordo e moderação, com as limitações documentadas.
- [x] Adotar Flyway, baseline SQL para banco vazio e Hibernate em modo validate; testar o baseline contra H2/MySQL mode.
- [x] Executar Flyway V1–V14 e Hibernate `validate` em schema vazio e descartável MySQL 8.0.46, usando a aplicação Java 21.
- [x] Reexecutar a preparação/baseline guardada em cópia integral do banco público MySQL 8.0.46. Flyway V1–V14 e Hibernate `validate` passaram; os cinco enums foram preservados e uma comparação de IDs e todos os campos compartilhados nas sete tabelas legadas encontrou zero divergências. As cópias foram removidas depois da validação.
- [x] Fazer backup verificável do banco e dos uploads públicos e ensaiar a preparação/baseline numa cópia integral. A cópia de uploads estava vazia e não havia referências `/api/uploads/` no banco. Dump e arquivo de uploads foram validados por SHA-256; o backup ainda está no mesmo VPS e precisa de cópia externa e política de retenção.
- [x] Validar V1–V14 e Hibernate `validate` em MySQL 8.4.11 numa instância descartável isolada da VPS; API saudável e `GET /api/anuncios` aprovado. Os testes Java passaram localmente com JDK 21.0.12.1 portátil, H2 e 56 casos; a integração local com MySQL continua pendente porque o Docker Desktop está indisponível.
- [x] Validar conteúdo real de upload, dimensões e tamanho; associar upload de foto ao perfil autenticado e declarar volume persistente no Compose.
- [x] Limpar uploads sem referência após período de retenção configurável, preservando fotos ligadas a perfis, anúncios e publicações.
- [x] Aplicar quota de 100 MiB por conta, endpoint autenticado de uso, backfill de imagens referenciadas e remoção dos registros junto à limpeza dos órfãos.
- [x] Criar rotina para gerar arquivo de backup somente leitura do volume de uploads, validar tar e SHA-256, sem alterar o volume.
- [x] Fazer cópia externa e definir retenção; ensaiar restauração operacional. Em 28/10, o dump atualizado do banco ativo (7 tabelas, 44 registros no total) e o arquivo validado do diretório público de uploads (zero arquivos) foram copiados para fora da VPS, empacotados com AES-256 e verificados por SHA-256. A chave ficou protegida pelo DPAPI da conta Windows. O dump foi restaurado numa instância MySQL 8.0.46 isolada e sem rede; 7 tabelas e 44 registros foram lidos. O arquivo de uploads foi extraído em diretório temporário e continuou vazio. Container, volume e extração de teste foram removidos; MySQL e API públicos permaneceram saudáveis. Política definida: manter 30 cópias diárias e uma cópia de fim de mês por 12 meses, removendo versões antigas só depois de validar checksum e restauração da cópia nova. O processo ainda é manual e a cópia externa está na estação Windows, sem replicação para outro local físico/provedor.
- [x] Revogar acesso público à imagem quando a última referência pública for removida; uploads órfãos ficam privados do proprietário e sem cache HTTP.
- [x] Criar API pública e API autenticada para ler/editar o próprio perfil sem expor e-mail, além de contar avaliações reais.
- [x] Criar páginas de perfil público e edição, listar apenas ofertas ativas reais e ligar perfil a conta, catálogo e anúncio.
- [x] Permitir prévia, envio e remoção da foto profissional por fluxo autenticado.
- [x] Adicionar título profissional opcional ao perfil, apresentá-lo publicamente e incluí-lo na busca.
- [x] Adicionar capa opcional ao perfil, com upload autenticado, remoção, exibição pública e retenção do arquivo referenciado.
- [x] Adicionar links públicos opcionais de LinkedIn, Instagram e site HTTPS, com validação no servidor.
- [x] Permitir pausar e reativar a exposição do perfil, ofertas, portfólio, avaliações e imagens sem apagar dados da conta ou negociações. A API de denúncias também rejeita com 404 publicações e avaliações de perfis pausados, sem expor trechos; reprodução confirmada em H2 (58 testes Java passaram) e MySQL 8.4.11 descartável.
- [x] Validar perfil, foto e portfólio no MySQL 8.0.46 e persistência após reinício da API, em schema descartável da prévia Tailscale.
- [x] Validar em MySQL 8.4.11 descartável na VPS os fluxos REST completos de perfil, foto, portfólio, descoberta, venda/troca, propostas, acordos, conversa, conclusão mútua e avaliações; confirmar dados após reinício e foto após recriação da API com volume persistente. Em 27/10, percorrer o fluxo na interface Chromium integrada ao mesmo MySQL: cadastro de duas contas, perfil público, anúncio, proposta, aceite, mensagens, conclusão mútua e avaliação. A avaliação persistiu e o perfil público refletiu a média real; o stack descartável foi removido após a validação. Em 28/10, `scripts/validar-mysql84-isolado.sh` repetiu o fluxo REST de venda/reputação no MySQL 8.4.11 atual, confirmou privacidade do e-mail e filtro de amostra mínima, e verificou médias/busca após reinício da API; containers e volume descartáveis foram removidos. A execução local continua pendente porque o Docker Desktop permanece indisponível.
- [x] Validar o fluxo de perfil e portfólio pela interface Angular em MySQL 8.4.11 descartável. Em 28/10, Chromium cadastrou uma conta, selecionou JPG pelo controle nativo do navegador, publicou a foto e carregou a imagem persistida em `/api/uploads/`; a API salvou publicação e quota no banco. Stack, volume, conta e arquivos temporários foram removidos; os containers da prévia Tailscale permaneceram saudáveis.
- [x] Preservar o perfil público quando uma API secundária de ofertas, portfólio ou avaliações falhar; mostrar erro por seção e permitir nova tentativa sem exibir falha como lista vazia. Teste reproduz falhas 503 simultâneas nos três recursos, confirma perfil visível, erros individualizados e conteúdo vazio correto após nova tentativa; 5 testes da tela, typecheck, build e suíte Angular completa (89 testes) passaram em 28/10.
- [x] Mostrar o acesso à moderação no cabeçalho somente após confirmação de `/api/moderacao/acesso`; falha de rede mantém a opção oculta. Teste de componente cobre contas comuns e moderadores.
- [x] Validar na prévia Tailscale restaurada o cadastro, login, edição e busca pública de perfil em Chromium a 390 px; pausar o perfil sintético depois do teste. Confirmar que conta comum não recebe acesso/moderação no cabeçalho. A imagem web anterior está preservada para rollback.

### Portfólio e descoberta

- [x] Criar publicações persistidas com legenda, até seis fotos, vínculo opcional a oferta própria; mostrar no perfil e permitir remoção pelo autor.
- [x] Evitar reenvio de fotos aceitas quando outro upload da publicação falha; a nova tentativa envia apenas arquivos pendentes e mantém as URLs persistidas para concluir o post. Teste cobre erro 503 na segunda foto e publicação posterior com as duas URLs; typecheck, build e 90 testes Angular passaram em 28/10.
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
- [x] Validar na interface Angular integrada a MySQL 8.4.11: cadastro e login de comprador/anunciante, edição e busca do perfil, criação do anúncio, proposta, aceite, conversa, confirmação mútua, avaliação profissional (5/5) e do serviço (4/5). A API confirmou a avaliação persistida e média pública 5,0 com uma avaliação; o ambiente temporário isolado foi removido ao final.
- [x] Validar em MySQL 8.4.11 descartável o fluxo REST completo de troca: cadastro de dois participantes, perfil, anúncio sem preço e com habilidade desejada, proposta, aceite, duas mensagens, conclusão mútua e avaliação profissional/serviço. A consulta persistida retornou acordo `CONCLUIDO`, duas mensagens e nota profissional 5,0 (uma avaliação); Chromium apresentou busca, perfil, oferta e avaliação em 390 px e percorreu pela interface o cadastro do cliente, proposta, aceite, conversa, conclusão mútua e avaliação de troca. Após novo login, a API confirmou acordo `CONCLUIDO`, uma mensagem do fluxo de navegador, avaliação profissional/serviço 5/5 e nota pública 5,0. Containers, rede e volumes descartáveis foram removidos.
- [x] Validar contestação de avaliação, bloqueio da fila para conta comum, decisão de ocultação por moderador, privacidade pública e persistência após reinício em schema descartável MySQL 8.0.46; remover container, schema e usuário temporários.
- [x] Validar denúncias de publicação e avaliação em MySQL 8.0.46, acesso restrito à fila, preservação da evidência, ocultação e reexibição, além da atualização das avaliações e médias públicas.
- [ ] Completar verificação de acessibilidade nas demais telas, com leitor de tela, e repetir em MySQL 8.4. axe-core 4.14.0 não encontrou violações WCAG 2.1 A/AA no catálogo em 360, 760 e 1440 px; também não encontrou violações nos componentes Angular renderizados pela suíte ChromeHeadless anterior (87 testes). A suíte chegou a 91 testes em 28/10; ChromeHeadless em viewport compacto confirmou que a moderação permanece no menu lateral sem ocupar o cabeçalho. Em 28/10, a revisão dos controles aumentou a razão de contraste do ícone de conta/carrinho para 4,70:1, das estrelas inativas para 3,85:1 e dos contornos de campos para 3,23:1 sobre superfície suave e 3,46:1 sobre branco, atendendo ao mínimo 3:1 de [WCAG 2.1, critério 1.4.11](https://www.w3.org/WAI/WCAG21/Understanding/non-text-contrast.html). O texto do banner em azul teve 8,04:1; os círculos verdes são decorativos. As fotos do portfólio próprio agora recebem nome acessível por posição e perfil, coberto por teste. Segue pendente a inspeção manual das imagens em contexto e a revisão com leitor de tela. Chromium confirmou nomes acessíveis dos controles em catálogo, busca de profissionais, login e anúncio; a navegação da interface permanece sem erros de página na prévia. A árvore do formulário de anúncio revelou nomes repetidos para os períodos de todos os dias; o cadastro agora anuncia dia e período juntos (por exemplo, “Segunda-feira, Manhã”), com teste cobrindo as 21 opções e confirmação no Chromium local a 390 px. Em 28/10, a prévia Tailscale foi revisada em navegador a 390 e 1440 px: busca de profissionais, campos, filtro, títulos e estado vazio têm nomes acessíveis; dois avanços por Tab chegam ao controle de menu após o link de pular conteúdo; sem erros de página. As cores amostradas de texto principal, secundário, botão azul e ação verde tiveram razões 14,68:1, 5,64:1, 8,04:1 e 5,93:1, respectivamente; ícones claros decorativos e imagens não ficaram cobertos por essa amostra. A inspeção encontrou landmarks `<main>` duplicados na busca de profissionais e ordem de cabeçalhos incorreta no detalhe do anúncio; os landmarks internos foram substituídos por seções nomeadas, o detalhe recebeu um título principal acessível e títulos de seção hierarquizados, e testes cobrem os dois defeitos. Chrome confirmou a árvore de cabeçalhos do detalhe em 390 e 1440 px usando resposta local declarada como ilustrativa, sem persistência. Em 28/10, uma captura anônima da prévia Tailscale a 390 px confirmou nomes nos campos de login/cadastro, nos 21 horários do formulário de anúncio e hierarquia do detalhe; busca de profissionais apresentou estado vazio acessível, e o detalhe preservou o aviso de avaliação ausente e a necessidade de login para contato. A rota de perfil próprio redirecionou para login, sem sessão. Nenhuma ação submeteu dados e não houve erros de página. A árvore de acessibilidade e essa navegação não substituem a leitura manual com tecnologia assistiva. O estado de erro observado antes no servidor local decorreu da API indisponível, sem dados de teste locais. Essa inspeção não substitui a verificação com leitor de tela ou a revisão dos contrastes restantes; NVDA não está instalado e o uso manual do Narrador permanece pendente.
- Em 28/10, a auditoria Chromium da prévia encontrou o botão “Pesquisar” antes do campo na ordem de foco do cabeçalho. O DOM foi corrigido para campo → botão; o teste regressivo reproduziu a ordem antiga e passou depois da alteração. Após a promoção da web `d7dff80`, Chromium confirmou em 390 e 1440 px que a sequência de Tab começa por pular conteúdo, menu, marca, campo de busca e botão; todos os controles tinham nome acessível, um `<main>` e nenhum erro de console. A revisão com leitor de tela continua pendente.
- [x] Corrigir relação `aria-describedby` dos formulários de contestação para manter um identificador e uma explicação de prazo próprios por avaliação; teste com duas avaliações comprova a associação semântica. Chrome Headless: três testes da conversa, typecheck, build e suíte Angular completa (88 testes) passaram em 28/10.

### Entrega e fases seguintes

- [x] Conter exposição acidental de credenciais operacionais: rotacionar contas MySQL, manter o volume de dados, atualizar a API e redigir cópias operacionais; validações e limites registrados em [remediacao-credenciais-2026-10-28.md](../docs/remediacao-credenciais-2026-10-28.md).
- [ ] Completar validação operacional: leitor de tela e contraste, cópia para outro local/provedor, automatização de retenção e decisão de migração para o schema ativo. Concorrência, migração integral, contestação e denúncia/moderação foram validadas em cópias isoladas MySQL 8.0.46; os fluxos REST completos, incluindo upload durável, venda e troca passaram em MySQL 8.4.11 descartável na VPS. Em 28/10, perfil com especialidade e cidade foi localizado pela busca em MySQL 8.4 sem expor e-mail; o fluxo de notificações na mesma base descartável confirmou proposta, listagem, 404 para alteração por outra conta, marcação como lida e reabertura. Os fluxos de venda e troca pela interface foram percorridos em MySQL 8.4.11; busca, filtros, perfil público, foto e portfólio também foram conferidos. Em 28/10, a fila de denúncias de publicação passou pela interface 8.4, incluindo ocultar e reexibir; a interface validou contestação e as decisões de manter/ocultar uma avaliação em MySQL 8.4; demais telas e leitor de tela continuam pendentes. Moderação e persistência após reinício também passaram em MySQL 8.0.46. O backup da base e dos uploads foi restaurado em instância descartável na VPS e copiado cifrado para a estação Windows, conforme a retenção manual definida acima. A execução local permanece pendente: Docker Desktop 4.82.0 abriu, mas o serviço não inicializou porque o gerenciador de inferência não conseguiu remover o arquivo reparse point `dockerInference` em `AppData\Local\Docker\run`; numa retomada em 28/10, `com.docker.service` estava parado, a distribuição WSL `docker-desktop` também estava parada e `Start-Service` retornou `Cannot open 'com.docker.service' service` para a sessão atual. Não houve redefinição nem remoção de dados; a integração local com MySQL segue pendente.
- [ ] Reconciliar a publicação antes da próxima promoção: em 28/10 o domínio público ainda servia `skillswap-api/web:495a696` e `/profissionais` redirecionava ao catálogo; a prévia Tailscale está em `984f4c4`. Promoção pública segue pendente de solicitação explícita e das verificações de release.
  - Em 28/10, o fluxo de busca, perfil público, foto e publicação de portfólio foi conferido em Chromium a 390 e 1440 px, ligado a MySQL 8.4.11 descartável. Dados sintéticos, containers, volumes e credenciais temporárias foram removidos. Filtro por média mínima excluiu corretamente o perfil sem avaliações; nenhum dado de reputação fictício foi criado.
  - A inspeção identificou que a expressão regular de arquivos estáticos do Nginx capturava URLs `/api/uploads/*.png`, retornando 404. `location ^~ /api/` corrige a prioridade; `nginx -t`, respostas 200 de imagens no teste integrado e healthcheck da prévia passaram. API, bancos, domínio e proxy público não foram alterados.
- [ ] Fazer piloto regional com dados reais consentidos; definir métricas após linha de base observada. Em 28/10, Chromium 149 a 390 px na prévia Tailscale confirmou o estado vazio acessível da busca: zero profissionais reais cadastrados e CTA para apresentar o próprio trabalho. A carga inicial não cria perfis nem avaliações fictícias; ainda não há linha de base de uso.
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
