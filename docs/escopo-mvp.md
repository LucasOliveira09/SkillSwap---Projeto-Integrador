# Definição inicial e escopo do SkillSwap

Fonte de domínio: [Documento de Visão e Escopo do grupo Saptura](referencias/saptura-definicao-inicial-do-projeto.pdf), seis páginas. O nome do produto continua sendo SkillSwap.

## Objetivo e público

Conectar moradores a prestadores de manutenção e serviços manuais e a instrutores locais, com foco inicial em Florianópolis e região. A proposta combina descoberta de profissionais, portfólio e reputação com negociação por venda direta ou troca de habilidades entre duas pessoas.

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

## Entrega desta inicialização

- Workspace Angular standalone, modo estrito, roteamento e arquivos HTML/SCSS/TypeScript separados.
- Angular Material compatível, tema único e imports locais nos componentes.
- Cadastro de anúncio demonstrativo, sem upload, autenticação ou backend. A inicialização começou com catálogo vazio; na adaptação do layout antigo, foram acrescentados exemplos visuais identificados e filtros no catálogo, separados dos dados da sessão.
- Anúncios de demonstração mantidos somente em memória durante a sessão da aplicação, perdidos ao recarregar. Nenhuma promessa de publicação real.
- Validação de obrigatórios e espaços, venda com preço positivo e troca com habilidade desejada; limpar reinicia campos e feedback.
- Testes de regras, cadastro e navegação; build e conferência em navegador.

Parâmetros iniciais da demonstração: categorias “Manutenção e reparos” e “Aulas e instrução”; título de até 100 caracteres, descrição de até 1500, cidade de até 80 e habilidade desejada de até 120. São escolhas desta base, não limites prescritos pelo PDF; rever com a equipe antes da API definitiva.

## Funcionalidades futuras

O PDF propõe gateway PIX/cartão e retenção/liberação de valores, destaque pago, publicidade, galeria antes/depois, fotos em avaliações, geolocalização precisa, verificação documental e app/PWA. Permanecem fora desta inicialização. Pagamentos e verificação de identidade exigem decisões próprias de integração e operação; não serão simulados como garantias existentes.

O trecho do PDF que chama o MVP de “disponível na aplicação” descreve o escopo de referência, não comprova implementação neste repositório.

## O que definir antes das próximas entregas

1. Backend e banco: Java com Spring Boot e MySQL confirmados pelo usuário, mantendo Angular 20 no frontend ([ADR-002](decisions/002-stack-backend-banco.md)). Definir versões, ferramenta de build, ambiente e contrato da API.
2. Contas: método de acesso, dados mínimos, recuperação de conta e autorização de edição.
3. Anúncios: categorias definitivas, cidade/bairro, limites, regras de imagens e moderação.
4. Acordos: proposta, aceite, cancelamento, conclusão e resolução de divergências em vendas e trocas.
5. Chat e reputação: participantes, retenção de mensagens, denúncia e critérios de avaliação.
6. Entrega acadêmica: cronograma, divisão de responsabilidades e evidências da pesquisa.

Modelo conceitual inicial: usuário possui perfil e anúncios; anúncio recebe propostas; proposta aceita gera acordo entre participantes; acordo possui conversa e pode gerar avaliações após conclusão. É um modelo de planejamento, sem schema de banco ou API já criados.
