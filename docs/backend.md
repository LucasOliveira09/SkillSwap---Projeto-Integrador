# Backend e integração Angular

## Referência e plano

Pedido de 20/10/2026: adotar os padrões do projeto local `backend-eventos-java/backend-eventos-java/eventos` e servir os dados pelo backend. A referência usa Java 21, Spring Boot 4.1.1, Maven Wrapper 3.9.16, Lombok, entidades Jakarta Persistence com IDs identity e interfaces JpaRepository. O SkillSwap mantém esses padrões em `backend/`, pacote `br.edu.unifio.skillswap`, acrescentando controllers, services, DTOs e validação HTTP.

Entregas: base compilável; listagem/cadastro persistentes com testes; carga inicial idempotente da vitrine; integração HTTP Angular com estados de carregamento/erro; validação ponta a ponta e commits. A referência é somente consultada; não será alterada.

O banco é MySQL 8.4. O backend usa `ddl-auto=update` para desenvolvimento, preservando os registros, em vez de `create`/`import.sql` a cada reinício. A carga ilustrativa possui chave estável e não duplica nem substitui dados existentes. Credenciais vêm do ambiente. Antes de produção, adotar migrações versionadas, autenticação e autorização; esta entrega é uma aplicação acadêmica local, vinculada a loopback, sem contas nem operações de edição/exclusão.

## Contrato HTTP

Base `/api`; Angular usa proxy local, sem configurar CORS permissivo.

| Método | Rota | Resultado |
| --- | --- | --- |
| GET | `/categorias` | Categorias disponíveis para cadastrar, `{valor, rotulo}`. |
| GET | `/vitrine` | Coleções visuais `categorias`, `destaques`, `servicos`, lidas do banco. |
| GET | `/anuncios?busca=&categoria=&pagina=0&tamanho=5` | `{itens, pagina, tamanho, totalItens, totalPaginas}`; tamanho entre 1 e 50; busca até 100 caracteres; mais recentes primeiro, ID como desempate. |
| GET | `/anuncios/{id}` | Um anúncio; 404 se inexistente. |
| POST | `/anuncios` | 201, Location e anúncio gravado; confirmação somente após commit da transação. |

Entrada de cadastro: `titulo` (1–100), `descricao` (1–1500), `categoria` (código existente), `cidade` (1–80), `modalidade` (`venda`/`troca`), `preco`, `habilidadeDesejada` (até 120). Obrigatórios rejeitam espaços. Venda exige preço entre 0,01 e 99.999.999,99 com duas casas; troca exige habilidade e ignora preço. Campos de imagem, perfil, avaliação e indicação de exemplo não são aceitos no cadastro. Identificadores e data são gerados no servidor.

Resposta de anúncio inclui os dados de cadastro, `id` numérico, `criadoEm` e metadados visuais para o card (`categoriaRotulo`, `imagem`, `unidade`, `autor`, `avatar`, `estrelas`, `exemplo`). Dados ilustrativos continuam identificados; anúncios novos não recebem nome de perfil nem avaliações inventadas.

Erros: `{codigo, mensagem, campos}`; 400 para entrada/parâmetros inválidos, 404 para anúncio inexistente, 500/503 com mensagem genérica sem detalhes SQL ou stack trace. Busca ignora acentos e caixa e trata `%`/`_` literalmente. Paginação e filtros são processados no backend; não há fallback em memória no Angular.

## Fontes técnicas

- [Requisitos do Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html).
- [Spring Data JPA e consultas parametrizadas](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html).
- [Configuração do HttpClient no Angular](https://angular.dev/guide/http/setup).
- [MySQL 8.4 no Windows](https://dev.mysql.com/doc/refman/8.4/en/windows-install-archive.html).
