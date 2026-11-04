# Auditoria mobile — 04/11/2026

## Escopo e evidências

Auditoria do repositório atual, sem deploy, migration nova ou escrita em dados de produção. A versão publicada foi observada como visitante; a versão alterada foi servida em `127.0.0.1:4200` com um proxy que encaminha somente GETs públicos e bloqueia escrita. As respostas das telas públicas são reais; não há conta de demonstração ou dados sintéticos apresentados como produção.

Chromium emulou 320, 360, 375, 390, 412, 768, 1024 e 1440 px nas rotas catálogo, busca de profissionais, perfil público, anúncio e conta: 40 capturas antes e 40 depois. Arquivos locais: `C:\Temp\skillswap-mobile-20261104\antes\` e `C:\Temp\skillswap-mobile-20261104\depois\`. Em ambas as séries, o documento não excedeu a largura da viewport nas 40 medições. No primeiro viewport de 320 px, foram identificados alvos inferiores a 24 px antes; as cinco páginas medidas depois não apresentaram alvos desse tamanho no primeiro viewport. Isso não é uma auditoria completa de WCAG.

Em 320 px, o botão de busca de profissionais passou da posição aproximada de 1097 px para 718 px no documento; o primeiro atalho de contato do anúncio passou de aproximadamente 1140 px para 334 px; o botão de entrada passou de aproximadamente 762 px para 632 px. As posições são medições de laboratório, não métricas de campo. Foram reproduzidos o erro de preço no filtro e a preservação das rotas dos atalhos. O domínio público devolveu um 404 de imagem de anúncio; nenhum dado público foi alterado.

No navegador local com dados públicos em modo somente leitura, o link “Ver perfil e avaliações” do anúncio 24 abriu `/profissionais/8`, correspondente ao `usuarioId` retornado pela API. O perfil de destino carregou. Esse teste não autentica nem altera contas.

Não foram executados testes autenticados em contas reais, upload persistente no navegador, leitor de tela, Safari iOS, Android físico, teclado virtual real, zoom 200% ou medições de Core Web Vitals. O proxy local bloqueou ações de escrita; os testes automatizados de componentes e backend usam ambiente e dados de teste.

## Inventário resumido

| Rota ou recurso | Estado e evidência | Limite desta auditoria |
| --- | --- | --- |
| `/anuncios` | Catálogo, busca e filtros existentes; tela pública observada. | Não foram exercitadas todas as combinações de filtros. |
| `/anuncios/:id` | Detalhe, comentários, avaliações e contato existentes; rota pública observada. | Escrita e contato autenticado não executados. |
| `/anuncios/novo`, `/anuncios/minhas`, `/anuncios/:id/editar` | Cadastro, listagem própria e edição existentes; cadastro protegido por guard e alterações verificam proprietário no servidor. | Formulários autenticados não exercitados no navegador. |
| `/profissionais` | Busca paginada e filtros existentes; preços foram reproduzidos no navegador e corrigidos localmente. | Consulta nova ao MySQL real ainda precisa de validação. |
| `/profissionais/:id` | Perfil público, foto/capa quando disponíveis, experiências, portfólio, serviços, avaliações, seguir e compartilhar existentes. | Perfil público anônimo observado; seguir e perfil próprio não testados em conta real. |
| `/meu-perfil` | Edição, privacidade, foto, capa, experiências, certificações e publicações existentes. | Upload, remoção e persistência após refresh não foram executados nesta auditoria. |
| `/conta` | Login, cadastro, conta e notificações existentes; tela anônima observada. | Login/cadastro não foram exercitados com conta real; recuperação de senha não foi identificada nas rotas existentes. |
| `/mensagens`, `/acordos/:id` | Conversas, propostas, acordos e avaliações existentes; mensagens usam guard. | Não verificados no navegador autenticado nesta execução. |
| `/moderacao/denuncias` | Tela e API de moderação existentes. | Acesso de moderador não testado. |
| URL desconhecida | Roteador redireciona ao catálogo. | Não existe página 404 dedicada identificada. |
| Feed, portfólio e mídia | Publicações do portfólio, fotos, curtidas, comentários e respostas existem; não foi identificado feed geral separado nem projeto de portfólio independente. | Vídeo, recorte de imagens e compartilhamento individual de publicação não identificados. |
| Autenticação e dados | JWT em memória e restauração por cookie protegido; perfis públicos ocultam credenciais e mensagens. API Spring Boot, MySQL e Flyway V24 no código. | Não foi feita auditoria de segurança completa nem nova conexão de escrita com produção. |

## 15 oportunidades priorizadas

| # | Prioridade | Evidência e impacto | Ação/estado; esforço e risco |
| --- | --- | --- | --- |
| 1 | P1 | Busca por preço numérico gerava `TypeError` e não navegava. | Corrigido; mínimo e máximo verificados no Chromium e no teste Angular. Baixo/baixo. |
| 2 | P1 | Perfil 8 e anúncio ativo 24 eram públicos, mas busca pelo título do serviço retornava zero porque o perfil não tinha título nem especialidades. | Consulta corrigida e publicada; testes H2 e MySQL 8.0 isolado cobrem anúncio ativo, filtros, inativação e perfil pausado. Desempenho em escala pendente. |
| 3 | P1 | Filtros empurravam o botão de busca para 1097 px em 320 px. | Filtros avançados recolhíveis; botão a 718 px na medição local. Baixo/baixo. |
| 4 | P1 | Contato do anúncio ficava depois da galeria e descrição, por volta de 1140 px em 320 px. | Atalho para a seção existente a 334 px; preserva rota e não cria conversa extra. Baixo/baixo. |
| 5 | P1 | O cabeçalho do próprio perfil público não oferecia edição direta. | Link para `/meu-perfil` para o titular; coberto por teste de componente. Baixo/baixo. |
| 6 | P1 | O link “Conhecer profissionais” tinha altura medida de 18 px. | Área de 44 px aplicada. Baixo/baixo. |
| 7 | P1 | Links do caminho estrutural mediam 17–21 px de altura nas telas pequenas. | Área de 44 px em até 768 px; conferir outras áreas fora da primeira tela. Baixo/baixo. |
| 8 | P1 | Texto da busca do cabeçalho aparecia cortado em 320 px. | Placeholder curto e coerente com a busca real. Baixo/baixo. |
| 9 | P1 | Ações secundárias do perfil ficavam empilhadas e repetiam explicação longa. | Ações secundárias lado a lado e texto objetivo; botão principal preservado. Baixo/baixo. |
| 10 | P1 | Logo de acesso consumia boa parte da primeira tela em 320 px. | Logo e espaçamentos reduzidos somente no celular. Baixo/baixo. |
| 11 | P1 | Na lista de profissionais, o nome abria o perfil, mas foto e avatar de fallback não eram links. | Ambos apontam para o perfil correto, com nome acessível e teste de componente. Baixo/baixo. |
| 12 | P1 | Rede pública retornou 404 para imagem vinculada a anúncio; já existe fallback no card. | Proprietário precisa substituir imagem ou os dados precisam ser classificados; nenhuma mudança em produção foi feita. Baixo/médio. |
| 13 | P2 | Foto/capa aceitam JPEG, PNG e GIF; preview existe, mas não foram encontrados recorte e suporte WebP no fluxo. | Planejar prévia/recorte e formatos com validação no servidor; não prometer suporte inexistente. Médio/médio. |
| 14 | P2 | Comentários exibem nome, porém o DTO consultado não fornece ID de autor para link ao perfil. | Evoluir contrato e privacidade antes de criar link. Médio/médio. |
| 15 | P1 estrutural | Proposta no chat ainda recebe valor manual e não usa regras configuradas no anúncio. | Seguir plano de serviços configuráveis; cálculo, snapshot e contraproposta continuam pendentes. Alto/alto. |

## Backlog executável

**MVP real:** validar com duas contas isoladas foto/capa, edição, publicação com imagem, comentário, seguir, contato e avaliação após acordo; revisar desempenho e plano de índices da busca em MySQL; classificar a imagem 404 com o proprietário; proteger rascunhos de formulários em falhas e navegação.

As legendas do resumo de perfil, que chegavam a 9 px, foram aumentadas para 11 px. A leitura de textos pequenos e o contraste do restante do sistema ainda exigem auditoria específica.

**Melhoria incremental:** links de autores com contrato e privacidade, confirmação de remoção de mídia, recorte e prévia de avatar/capa, estados vazios e erro, revisão de alvos de toque fora da primeira tela, navegação mobile e filtros avançados, teclado/zoom/leitor de tela, paginação e otimização de imagens. Avaliar uma barra inferior apenas após testar sobreposição e teclado.

**Visão futura:** feed separado e portfólio de projetos com mídia e vínculo a serviços; catálogo de serviços persistido; configuração de tarifas, campos e condições no anúncio; cálculo no servidor, snapshots e contrapropostas; segurança e moderação adicionais; métricas de desempenho de campo. Pagamentos continuam fora da plataforma.

## Roteiro seguro para a apresentação

1. Abrir o catálogo no celular e localizar um serviço real.
2. Abrir o anúncio 24 e usar o atalho de contato ou o link do anunciante para chegar ao perfil público 8; o atalho pede login antes de iniciar uma conversa.
3. Mostrar as informações e as seções efetivamente preenchidas, sem interpretar ausência de conteúdo como falha ou apresentar dados fictícios.
4. Usar busca de profissionais e filtros; a melhoria da consulta e da interface foi publicada após autorização.
5. Mostrar chat, proposta e edição de conta apenas com uma conta autorizada e previamente verificada. O valor de proposta atual é manual; não apresentar cálculo parametrizado ou pagamentos como disponíveis.

## Verificações executadas

- `npm run typecheck`: aprovado.
- `npm run test:ci`: 133 testes Angular aprovados em Chrome Headless.
- `npm run build`: aprovado sem avisos de budget.
- `backend/mvnw.cmd test`, JDK 21: 101 testes, zero falhas e um teste MySQL optativo ignorado por falta de banco isolado.
- Chromium: 40 capturas antes e 40 depois; atalhos de perfil e contato preservaram a rota e encontraram o destino; preço mínimo e máximo geraram os parâmetros corretos sem erro de console.
- Lint e E2E automatizado não estão configurados no `package.json`. O motor Docker local não iniciou; posteriormente, a API candidata foi testada em MySQL 8.0 isolado na VPS, sem acesso ao banco público. A busca, os filtros, o perfil pausado e o anúncio inativo foram verificados com dados sintéticos; o plano de execução em escala permanece pendente.

## Publicação autorizada

Após autorização explícita, os commits foram integrados à `main` e a versão `aec5e00` foi publicada em 04/11/2026. A busca pública por “Beach Tennis” passou a retornar o perfil 8; API, web e MySQL ficaram saudáveis. A publicação e a reversão estão descritas em `docs/backend.md`. As capturas de produção de 320 e 390 px estão em `C:\Temp\skillswap-mobile-20261104\publicado\`. As limitações de testes autenticados, acessibilidade e dispositivos reais continuam válidas.
