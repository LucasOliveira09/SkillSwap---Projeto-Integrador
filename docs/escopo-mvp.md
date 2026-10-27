# Definição inicial e escopo do SkillSwap

Fonte de domínio: [Documento de Visão e Escopo do grupo Saptura](referencias/saptura-definicao-inicial-do-projeto.pdf), seis páginas. O nome do produto continua sendo SkillSwap.

## Objetivo e público

Conectar moradores a prestadores de manutenção e serviços manuais e a instrutores locais, com foco inicial em Florianópolis e região. A proposta combina descoberta de profissionais, portfólio e reputação com negociação por venda direta ou troca de habilidades entre duas pessoas.

## Visão ampliada confirmada pelo usuário

O SkillSwap deve combinar uma rede de apresentação profissional com um marketplace local: cada pessoa pode construir um perfil público e portfólio visual, divulgar ofertas e ser encontrada por clientes; clientes podem comparar profissionais, conversar, negociar e avaliar trabalhos concluídos. Publicações devem ajudar a mostrar a qualidade e o estilo do serviço, sem transformar o produto em uma rede social genérica.

Uma conta pode atuar como profissional e cliente. A descoberta deve responder “quem atende ao que preciso e na minha região?” e sustentar a confiança com avaliações de operações reais, contexto e quantidade de avaliações. Não apresentar notas, selos, identidade verificada, popularidade ou pagamento protegido sem mecanismos implementados e evidências correspondentes.

O usuário definiu que pagamentos dentro da plataforma ficam para uma fase posterior à validação de perfis, ofertas, descoberta, propostas, acordos, conversa e avaliações. Até lá, a plataforma organiza a negociação, mas não processa nem garante pagamento.

O [plano de produto e implementação](../tasks/plan.md) detalha os fluxos, critérios de aceite, dependências, riscos e decisões em aberto. O usuário aprovou o plano em 26/10/2026 e a implementação está em andamento; pagamentos foram deixados para uma fase posterior. A visibilidade atual dos dados está registrada em [politica-perfis.md](politica-perfis.md).

O documento relata dificuldade de descoberta, desconfiança na contratação e falta de vitrine para autônomos. As conclusões de pesquisa são relatos do documento; faltam amostra, roteiro e registros para sustentar percentuais em uma apresentação. Evitar prometer segurança total, identidade verificada ou proteção financeira antes de implementar e validar esses mecanismos.

## Seis capacidades do MVP planejado

| Capacidade | Entrega e regra de aceite futura |
| --- | --- |
| Contas e perfis profissionais | Acesso autenticado, perfil público com biografia, foto, especialidades e métricas derivadas de operações reais. Um usuário poderá buscar e oferecer serviços. |
| Anúncios de serviços e aulas | Criar e editar anúncios próprios com título, descrição, categoria, localidade, modalidade e portfólio. Venda informa preço; troca informa a habilidade desejada. |
| Catálogo e detalhe | Buscar e filtrar por categoria, localidade e modalidade; abrir anúncio e perfil correspondente; tratar carregamento, erro e ausência de resultados. |
| Propostas e acordos | Registrar intenção de venda ou troca entre duas pessoas, acompanhar status e confirmar conclusão. Não processar dinheiro nesta fase. |
| Comunicação | Conversa entre participantes de uma negociação, com controle de acesso e mensagens associadas ao acordo. |
| Reputação e histórico | Avaliação de 1 a 5 e depoimento associados a serviço concluído; impedir avaliação de operação inexistente e autoavaliação. |

Esta decomposição organiza o documento em seis capacidades para planejamento. Autenticação, estados de acordo e regras contra abuso são definições técnicas propostas para viabilizar o domínio, ainda dependentes do contrato do backend.

## Entrega da inicialização (histórico)

- Workspace Angular standalone, modo estrito, roteamento e arquivos HTML/SCSS/TypeScript separados.
- Angular Material compatível, tema único e imports locais nos componentes.
- Cadastro de anúncio demonstrativo, sem upload, autenticação ou backend. A inicialização começou com catálogo vazio; na adaptação do layout antigo, foram acrescentados exemplos visuais identificados e filtros no catálogo, separados dos dados da sessão.
- Anúncios de demonstração mantidos somente em memória durante a sessão da aplicação, perdidos ao recarregar. Nenhuma promessa de publicação real.
- Validação de obrigatórios e espaços, venda com preço positivo e troca com habilidade desejada; limpar reinicia campos e feedback.
- Testes de regras, cadastro e navegação; build e conferência em navegador.

Parâmetros iniciais da demonstração: categorias “Manutenção e reparos” e “Aulas e instrução”; título de até 100 caracteres, descrição de até 1500, cidade de até 80 e habilidade desejada de até 120. São escolhas desta base, não limites prescritos pelo PDF; rever com a equipe antes da API definitiva.

## Integração do backend em 20/10/2026

Na primeira integração, anúncios, categorias e coleções visuais passaram a vir do backend Spring Boot; sessão em memória de tela e fixtures do Angular foram removidas. A adoção posterior do backend escolhido pelo usuário acrescentou autenticação, perfis, portfólio, negociação, conversa e reputação, descritos nas seções seguintes. Busca, paginação e gravação persistem pela API; a validação real em MySQL das novas migrações continua pendente. Consulte o [contrato e a execução](backend.md).

## Integração do GitHub em 21/10/2026

Por escolha do usuário, o backend novo do GitHub passou a ser a base canônica: Java 21, Spring Boot 4.0.8 e Maven Wrapper 3.10.0, mantendo MySQL. O Angular preserva o layout antigo e tem conta, login JWT, cadastro autenticado, perfil, busca de profissionais, portfólio, propostas, acordos, conversa e avaliações. A validação integral com MySQL e as regras de moderação ainda estão pendentes. A [ADR-004](decisions/004-integra-backend-github.md) substitui a organização inicial; o [contrato HTTP](backend.md) descreve o estado atual.

## Implementação de perfis iniciada em 26/10/2026

A API permite consultar campos públicos de um profissional, editar o perfil autenticado, listar ofertas ativas e criar, editar legenda ou remover publicações de portfólio com até seis fotos. O perfil público e o editor paginam publicações pela API. O DTO público não retorna e-mail; a média e a quantidade de avaliações vêm de registros do backend. O Angular oferece página pública responsiva, editor de perfil e portfólio, prévia e envio das imagens. O backend valida conteúdo real JPG/PNG/GIF, dimensões, tamanho e existência das fotos; cada conta tem quota de 100 MiB por padrão, exibida no editor, e a migração V6 registra imagens já referenciadas por perfis, anúncios e publicações. A rotina diária remove arquivos hash sem referências após 30 dias e os metadados de quota associados. Denúncias de portfólio e avaliações são analisadas por uma allowlist de moderadores, preservam evidência e permitem ocultar ou reexibir; métricas públicas ignoram avaliações ocultadas. O Compose local e de publicação declaram volume persistente. Backup dos uploads, migração dos arquivos do container público atual, teste MySQL e persistência após reinício continuam pendentes.

A primeira busca de profissionais também está disponível em `/profissionais`, por nome/especialidade, cidade e nota mínima opcional. O filtro de nota considera apenas profissionais com pelo menos três avaliações visíveis; a listagem não é ordenada por nota e continua estável por data. Ela lista apenas pessoas que preencheram especialidades, apresenta contagem e média só quando vierem de avaliações existentes e não usa perfis ou notas fictícias. Após acordo concluído, o cliente que enviou a proposta avalia o profissional e a oferta/serviço em campos separados; o detalhe apresenta média e amostra reais, e o perfil lista os depoimentos. O profissional avaliado pode publicar uma resposta curta e única, exibida no perfil. Edição pelo autor e contestação pelo profissional seguem a [política de avaliações](politica-avaliacoes.md), com histórico preservado e moderação restrita à allowlist. Contas autenticadas denunciam portfólio e avaliações; avaliações ocultadas deixam de contar nas médias públicas. A visibilidade de dados e o limite de revogação de imagens seguem a [política de perfis](politica-perfis.md). A integração completa precisa de validação em MySQL real.

## Funcionalidades futuras

O PDF propõe gateway PIX/cartão e retenção/liberação de valores, destaque pago, publicidade, galeria antes/depois, fotos em avaliações, geolocalização precisa, verificação documental e app/PWA. Permanecem fora desta inicialização. Pagamentos e verificação de identidade exigem decisões próprias de integração e operação; não serão simulados como garantias existentes.

O trecho do PDF que chama o MVP de “disponível na aplicação” descreve o escopo de referência, não comprova implementação neste repositório.

## O que definir antes das próximas entregas

1. Backend e banco: adotar migrações versionadas e evoluir o contrato dos demais domínios, conforme a [ADR-004](decisions/004-integra-backend-github.md).
2. Contas: recuperação de acesso, foto de perfil, prévia e validação do perfil com persistência real. Registro/login, edição básica e autorização de anúncios por proprietário já estão implementados.
3. Anúncios: categorias definitivas, cidade/bairro, limites, regras de imagens e moderação.
4. Acordos: proposta, aceite, cancelamento, conclusão e resolução de divergências em vendas e trocas.
5. Chat e reputação: participantes, retenção de mensagens, denúncia e critérios de avaliação.
6. Entrega acadêmica: cronograma, divisão de responsabilidades e evidências da pesquisa.

Modelo conceitual inicial: usuário possui perfil e anúncios; anúncio recebe propostas; proposta aceita gera acordo entre participantes; acordo possui conversa e pode gerar avaliações após conclusão. O backend recebido contém esses domínios; somente catálogo, conta e cadastro de anúncios tiveram o fluxo Angular/MySQL validado nesta integração.
