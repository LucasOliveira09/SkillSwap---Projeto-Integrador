# ADR-003: Backend seguindo o projeto de eventos

Status: aceita em 20/10/2026 por solicitação do usuário. Complementa a [ADR-002](002-stack-backend-banco.md).

## Contexto e decisão

O usuário pediu que o backend seguisse o projeto local `backend-eventos-java/backend-eventos-java/eventos` e que os dados fossem servidos pelo backend. Foram mantidos Java 21, Spring Boot 4.1.1, Maven Wrapper 3.9.16, pacote `br.edu.unifio`, entidades Jakarta Persistence com Lombok, IDs identity e repositórios JpaRepository. O backend fica em `backend/`, sem mover o workspace Angular da raiz.

O domínio inicial é anúncio com categoria e itens de vitrine. Controllers expõem DTOs; o serviço valida regras de venda/troca e delimita transações; os repositórios executam consultas parametrizadas e paginadas. O Angular usa HttpClient e proxy `/api`; não mantém anúncios em memória nem usa exemplos como fallback quando o servidor falha. Os dados ilustrativos são carregados no MySQL e continuam identificados.

MySQL 8.4 (8.4.11 no ambiente validado), imagem Docker fixada por digest, porta local 3307 e volume persistente. O usuário também pode apontar para um MySQL existente por variáveis de ambiente. JPA `update` é uma escolha da etapa acadêmica local; a referência usa `create`, que apagaria os cadastros em cada reinício. A carga inicial é idempotente e não sobrescreve registros.

## Consequências e limites

- A referência é consultada sem alterações e sem copiar credenciais ou entidades de eventos.
- API vinculada a loopback, sem autenticação nesta etapa; não disponibilizar publicamente. Edição, exclusão, contas e autorização são próximas entregas.
- Credenciais locais ficam em `.env` ignorado pelo Git ou no ambiente; scripts não exibem senhas. Nenhum dado financeiro ou senha de usuário é solicitado.
- Os testes automatizados usam H2 isolado; integração e reinício também são verificados com MySQL real. H2 não substitui o MySQL da aplicação.
- Migrações versionadas e estratégia de deploy serão necessárias antes de produção. Esta etapa não introduz um framework de migrações para reproduzir a base didática solicitada.
- Os arquivos de imagem e fontes continuam estáticos no Angular; conteúdo do catálogo, categorias e coleções vem do banco. Banner e textos de interface são componentes de apresentação.
