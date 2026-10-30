# Auditoria e plano: perfis profissionais e marketplace social

Data da auditoria: 30/10/2026
Escopo inicial: auditoria do repositório e planejamento. A Etapa 1 precedeu esta implementação. A atualização de 30/10 acrescenta disponibilidade, métricas derivadas e seguir/deixar de seguir com migração aditiva V18; experiências/certificações e interações nas publicações ainda estão pendentes.

## Diagnóstico atual

### Stack, organização e identidade visual

- Frontend Angular 20 standalone, TypeScript estrito, rotas lazy por domínio, HTML/SCSS separados e Angular Material 20.
- Backend Java 21 com Spring Boot 4.0.8, Maven Wrapper, JPA/Lombok, organizado em `com.skillswap.api.domain` e `infra`.
- MySQL com Flyway; a versão mais recente do schema do repositório é V17. A auditoria leu entidades e migrações versionadas, sem conectar nem modificar uma instância de banco.
- Tokens globais existentes: azul `#255188`, verde `#8bc63f`/`#79bb39`, tipografia Poppins e display Angkor, fundos claros e bordas `#e6edf5`. Imagens e fontes locais do projeto legado continuam sendo a referência visual.
- Componentes reutilizáveis relevantes: `IconeComponent`, `DenunciarComponent`, botões/controles Material e `AnuncioCardComponent` (este último deve ser avaliado para ofertas no perfil antes de reutilizar, para evitar duplicação de comportamento).

### Página pública existente

Rota lazy `/profissionais/:id` em `src/app/features/perfil/pages/perfil-publico/`. A tela atual já exibe:

- Capa, foto, nome, título profissional, especialidades, cidade e data de cadastro.
- Média e quantidade reais de avaliações visíveis, depoimentos e respostas públicas do profissional.
- Portfólio paginado com fotos/legendas e vínculo opcional à própria oferta.
- Anúncios ativos reais paginados, modalidade, cidade e preço quando aplicável.
- Navegação por links âncora para Sobre, Portfólio, Serviços e Avaliações. Não são abas com estado próprio; são seções da mesma página.
- Estados de carregamento, perfil indisponível e erros/repetição independentes para ofertas, publicações e avaliações.

O perfil é carregado primeiro; ofertas, publicações e avaliações são buscadas em paralelo depois. Os serviços usam endpoints REST existentes. A tela não precisa de fixtures para preencher o perfil.

### Edição, autenticação e privacidade

- `/meu-perfil` carrega o próprio perfil e redireciona visitantes para login. O backend edita apenas `/api/perfil` com autenticação; não há endpoint que permita escolher outro usuário para atualização.
- Foto e capa passam por upload autenticado. O backend valida conteúdo real da imagem, MIME, dimensões, limite de 5 MB e quota por usuário. Imagens sem referência pública não ficam abertas a visitantes.
- `/api/perfis/{id}` e conteúdo associado são públicos somente enquanto `perfilPublico=true`. O DTO público não inclui e-mail nem credenciais. Cidade é aproximada; não há telefone ou endereço exato no perfil.
- `/api/perfis/{id}/anuncios` lista ofertas ativas, e `/api/perfis/{id}/publicacoes` lista publicações não ocultadas. Avaliações públicas excluem conteúdo ocultado e são originadas de acordos concluídos.
- A sessão usa JWT de acesso em memória e cookie persistente HttpOnly/Secure/SameSite=Strict conforme ADR-006. O componente também consulta a sessão antes de abrir o editor; a autorização decisiva está no servidor.

### Modelo de dados e capacidades ausentes

Relações relevantes presentes no schema/código:

| Recurso | Relação e uso atual |
| --- | --- |
| `usuarios` | Identidade, bio, título, cidade, foto, capa, links HTTPS, especialidades e visibilidade pública. |
| `anuncios` | Cada oferta pertence a um usuário; possui estado, modalidade e dados comerciais. |
| `propostas` → `acordos` | Interesse em uma oferta; acordo nasce ao aceitar a proposta. Conversa usa a proposta como contexto. |
| `avaliacoes` | Liga acordo, avaliador e avaliado; contém notas de profissional/serviço, depoimento, resposta e estado de moderação. |
| `publicacoes_portfolio` → `publicacao_portfolio_fotos` | Publicação de um usuário, até seis fotos, legenda, moderação e vínculo opcional a uma oferta própria. |
| `uploads_usuario` | Arquivos validados e quota por usuário; rotina de remoção de órfãos respeita referências. |

Não encontrei modelo, migração nem endpoint atual para seguidores, experiências profissionais, certificações, publicações em vídeo, curtidas ou comentários. Também não há métrica/endereço de API específico para trabalhos concluídos no perfil; acordos possuem estado e confirmação mútua, então a contagem poderá ser derivada apenas de acordos efetivamente concluídos, se o produto confirmar a definição.

O calendário declarado em ofertas não equivale a um indicador global de “aceitando trabalhos”. Qualquer selo global precisará de regra explícita e persistida, sem inferir disponibilidade de forma enganosa.

## Reuso, dependências e riscos

- Evoluir a página e o editor dentro de `features/perfil`; manter rotas lazy, standalone e arquivos HTML/SCSS/TypeScript separados.
- Reusar cores, tipografia, `IconeComponent`, botões/controles Material e o componente de denúncia onde conteúdo exigir moderação.
- Reusar API de perfil, anúncios, portfólio, conversa e avaliação já existente; não introduzir endpoint Angular sem contrato backend correspondente.
- Ações de orçamento devem levar a uma oferta ativa e ao fluxo de conversa por anúncio existente. Uma conversa direta sem oferta não está definida no contrato atual.
- Novas métricas ou atributos precisam de DTO/validação/serviço/repositório e migração Flyway aditiva; evitar alterar ou apagar dados históricos.
- Upload de vídeo não é compatível com o armazenamento atual, que aceita apenas JPG/PNG/GIF. Vídeo exigirá decisão separada de formato, limites, conversão/serviço de mídia, retenção, moderação e impacto na quota/disco.
- Regressões principais: divergência do layout legado; perda de estados de seção; cartão de oferta duplicado; CTA que abre conversa sem contexto de anúncio; exposição de dados privados; métricas sem fonte; acessibilidade/overflow em telas estreitas.

## Plano incremental

### Etapa 1 — Página pública com dados existentes

**Estado: implementada em 30/10/2026.** Reorganiza a página pública e preserva os dados, estados e rotas existentes. Testes completos, build, typecheck e conferência visual em 320, 768 e 1440 px passaram; a captura de 390 px também foi conferida após o ajuste mobile. Não houve deploy.

Redesenhar a hierarquia da página profissional usando somente dados e recursos já persistidos: identidade e reputação na abertura, chamadas para ofertas e contato contextual, navegação clara por seções, portfólio visual, ofertas e avaliações. Manter a identidade original do SkillSwap e os estados de carregamento, erro, repetição e vazio existentes. Não apresentar botões de seguir, disponibilidade ou contagens ainda inexistentes como funcionais.

**Arquivos prováveis:**

- `src/app/features/perfil/pages/perfil-publico/perfil-publico.component.html`
- `src/app/features/perfil/pages/perfil-publico/perfil-publico.component.scss`
- `src/app/features/perfil/pages/perfil-publico/perfil-publico.component.ts` somente se navegação/estado realmente mudar
- `src/app/features/perfil/pages/perfil-publico/perfil-publico.component.spec.ts`
- Eventualmente um componente local reutilizável em `src/app/features/perfil/ui/` se houver composição repetida demonstrada.

**Aceite:** exibir somente dados retornados pela API; orçamento leva a uma oferta e conversa existentes; perfil pausado mantém o 404 atual; erros de seções secundárias não apagam o cabeçalho; não expor e-mail, endereço preciso ou métricas inventadas; teclado funcional e sem rolagem horizontal em 320, 390, 768 e 1440 px; `npm run typecheck`, `npm run build` e `npm run test:ci` passam; validar também em Chromium antes de concluir.

**Verificação:** testes de componente para renderização, estados, links/CTAs e ausência de dados fictícios; build e typecheck; testes CI; Chromium em desktop e celular, conferindo foco, navegação por teclado e erros de console/rede. A revisão manual com leitor de tela continua necessária.

### Etapa 2 — Atributos profissionais e métricas reais

**Parcialmente implementada em 30/10/2026:** disponibilidade fica em `usuarios.disponivel_trabalhos`; seguidores e trabalhos concluídos são contagens consultadas no banco, e a conclusão usa somente acordos com status `CONCLUIDO`. A edição da disponibilidade exige sessão e o perfil público apresenta os valores retornados pelo servidor. Experiências/certificações ainda precisam de CRUD próprio.

**Decisões:** o profissional controla se aceita novos trabalhos; trabalhos contam após acordo concluído pelos dois participantes; experiências e certificados serão autodeclarados e públicos enquanto o perfil estiver visível, sem selo de verificação. Não publicar arquivos/documentos de identidade.

### Etapa 3 — Seguir e deixar de seguir

**Implementada em 30/10/2026, migration V18:** relação única `(seguidor, seguido)`, auto-seguimento proibido, mutações e leitura do estado autenticadas, idempotência, contagem derivada e bloqueio a perfil pausado. A lista de seguidores não é pública nem exposta por endpoint.

### Etapa 4 — Feed e interação social

**Implementada no código em 30/10/2026 (V22/V23; ainda sem publicação):** o portfólio existente recebeu curtidas persistidas e comentários paginados, sem criar outro feed. A API inclui contagem de curtidas e estado do usuário em consultas em lote, e oferece leitura pública, escrita autenticada, respostas de uma camada e denúncia/moderação de comentários. O perfil carrega comentários quando o painel é aberto; mostra falhas, repetição, vazio, envio, resposta e login.

**Limites mantidos:** vídeos continuam fora, pois o armazenamento aceita apenas imagens e não há política de mídia/moderação para vídeo. Não há ranking, lista pública de curtidores, notificações de interação nem edição/remoção individual de comentários nesta fatia.

**Verificação local:** suíte Java (95 testes), suíte Angular (128), typecheck e build passaram. As migrações V22/V23 passaram em H2; o Docker Desktop local está indisponível, então não foi feita a validação MySQL local. A publicação/VPS e o ensaio de migração isolada ainda são pendentes.

### Etapa 5 — Integração comercial e reputação

Manter ofertas e conversas por anúncio existentes. Exibir avaliações somente quando válidas segundo regras do acordo concluído, considerando ocultação/moderação. Testar a relação visual anúncio → perfil → conversa e preservar o fluxo de venda/troca. Pagamentos permanecem fora do escopo e dependem de tarefa/decisão própria.

## Decisões de produto ainda necessárias

1. “Disponível para trabalhos” será uma configuração explícita do profissional ou derivada de anúncios ativos? Recomendação: configuração explícita, independente do calendário de uma oferta.
2. O que conta como trabalho concluído: acordo com confirmação dos dois participantes (regra atual) ou outro critério?
3. Quais dados de experiência/certificação serão públicos, e devem ser declarados pelo profissional ou verificados por evidência?
4. **Decidido:** contagem agregada de seguidores é pública; a lista individual não é publicada.
5. Curtidas/comentários pertencem às publicações de portfólio existentes ou a um novo feed? Recomendação: reutilizar a publicação atual e estender em etapas, preservando vínculo opcional à oferta.
6. Vídeos são necessários já no MVP social? A stack atual só aceita imagens; especificar orçamento de armazenamento e moderação antes de incluir vídeo.

## Estado

Etapas 1, 3 e 4 estão implementadas no código; disponibilidade e métricas reais da Etapa 2 também estão prontas. Experiências/certificações da Etapa 2 seguem pendentes. Etapa 4 ainda precisa de validação em MySQL real e Chromium com a nova API antes de publicação. Acessibilidade manual permanece pendente. Consulte [interacoes-publicacoes-plano.md](interacoes-publicacoes-plano.md) para contrato e aceite da entrega, e [comunicacao-propostas-plano.md](comunicacao-propostas-plano.md) para a mudança publicada de conversas/propostas.
