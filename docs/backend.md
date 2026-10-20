# Backend e integração Angular

## Referência e plano

Pedido de 20/10/2026: adotar os padrões do projeto local `backend-eventos-java/backend-eventos-java/eventos` e servir os dados pelo backend. A referência usa Java 21, Spring Boot 4.1.1, Maven Wrapper 3.9.16, Lombok, entidades Jakarta Persistence com IDs identity e interfaces JpaRepository. O SkillSwap mantém esses padrões em `backend/`, pacote `br.edu.unifio.skillswap`, acrescentando controllers, services, DTOs e validação HTTP.

Entregas: base compilável; listagem/cadastro persistentes com testes; carga inicial idempotente da vitrine; integração HTTP Angular com estados de carregamento/erro; validação ponta a ponta e commits. A referência é somente consultada; não será alterada.

O banco é MySQL 8.4. O backend usa `ddl-auto=update` para desenvolvimento, preservando os registros, em vez de `create`/`import.sql` a cada reinício. A carga ilustrativa possui chave estável e não duplica nem substitui dados existentes. Credenciais vêm do ambiente. Antes de produção, adotar migrações versionadas, autenticação e autorização; esta entrega é uma aplicação acadêmica local, vinculada a loopback, sem contas nem operações de edição/exclusão.

## Convenções REST

Diretriz reforçada pelo usuário em 20/10/2026 para as próximas alterações:

- Modelar recursos nas URLs, usando substantivos e nomes plurais nas coleções, como `/api/anuncios` e `/api/categorias`. `/api/vitrine` representa uma composição de leitura. Evitar rotas com verbos, como `/cadastrarAnuncio`.
- Usar GET para leitura sem modificar dados, POST para criação, PUT para substituição completa, PATCH para alteração parcial e DELETE para remoção, quando essas operações forem implementadas e autorizadas. Preservar a idempotência de PUT e DELETE.
- Retornar os códigos HTTP correspondentes ao resultado: 200 em consultas, 201 com `Location` na criação e 204 sem corpo quando aplicável. Preservar 400, 404, 405 e demais erros adequados, sem devolver 200 para uma falha.
- Validar DTOs de entrada no servidor, separar entidades da representação pública e manter o formato de erros consistente. Usar parâmetros de consulta para busca, filtros e paginação, com limites definidos.
- Manter o contrato e os testes atualizados ao alterar o comportamento da API. As convenções não autorizam acrescentar edição/exclusão sem autenticação e autorização por usuário.

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

Erros: `{codigo, mensagem, campos}`; 400 para entrada/parâmetros inválidos, 404 para anúncio/rota inexistente, 405 para método indisponível, 500/503 com mensagem genérica sem detalhes SQL ou stack trace. Busca ignora acentos e caixa e trata `%`/`_` literalmente. Paginação e filtros são processados no backend; não há fallback em memória no Angular.

## Execução e operação

Pré-requisitos: JDK 21, PowerShell 7, Node compatível com Angular 20 e Docker Desktop ativo. Não é necessário instalar Maven: o wrapper baixa a versão fixada na referência.

Na raiz, executar `./scripts/iniciar-banco.ps1` e depois `./scripts/iniciar-backend.ps1`. Em outro terminal, executar `npm ci` e `npm start`. O script do banco gera senhas aleatórias em `.env` somente se o arquivo não existir. Se tiver copiado `.env.example` manualmente, preencher as duas senhas antes de iniciar.

O script do backend procura JDK 21 na instalação Windows habitual quando `JAVA_HOME` não estiver definido. É possível informar `-JavaHome` explicitamente. Ele carrega as variáveis conhecidas de `.env` sem executar o conteúdo do arquivo. Senhas nunca são exibidas pelos scripts.

| Variável | Uso |
| --- | --- |
| `DB_PASSWORD` | Obrigatória, senha do usuário do banco. |
| `DB_USER` | Padrão `skillswap`. |
| `DB_URL` | Padrão `jdbc:mysql://127.0.0.1:3307/skillswap?serverTimezone=America/Sao_Paulo&characterEncoding=UTF-8`. |
| `MYSQL_ROOT_PASSWORD` | Usada somente pelo Compose para criar o banco local. |
| `PORT` | Porta da API, padrão 8080; se alterada, ajustar `proxy.conf.json`. |
| `SEED_ENABLED` | Padrão `true`; `false` impede inserir novos exemplos/coleções. Não apaga exemplos existentes. |

Para um MySQL já instalado, criar banco/usuário com permissões apenas sobre esse banco e informar `DB_URL`, `DB_USER` e `DB_PASSWORD`. O frontend continua usando o proxy, que funciona no servidor de desenvolvimento. Um deploy futuro precisará encaminhar `/api` ao backend na mesma origem.

O banco local usa a porta 3307 e um volume nomeado do Compose. `docker compose stop mysql` pausa sem remover dados; `docker compose up -d --wait mysql` retoma. Alterar as senhas em `.env` não muda senhas de um volume já inicializado; a rotação deve ocorrer pelo MySQL. Não usar remoção de volume como rotina de reinicialização.

Na pasta `backend`, com `JAVA_HOME` apontando para JDK 21:

```powershell
./mvnw.cmd test
./mvnw.cmd package
```

Os testes usam exclusivamente H2 em memória e não acessam o MySQL local. O jar executável fica em `backend/target/skillswap-0.0.1-SNAPSHOT.jar`. Para executá-lo diretamente, fornecer as mesmas variáveis de conexão. Não versionar `target`, `.env` ou dados do banco.

## Fontes técnicas

- [Requisitos do Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html).
- [Spring Data JPA e consultas parametrizadas](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html).
- [Configuração do HttpClient no Angular](https://angular.dev/guide/http/setup).
- [MySQL 8.4 no Windows](https://dev.mysql.com/doc/refman/8.4/en/windows-install-archive.html).
- [Status e cabeçalhos das exceções HTTP do Spring](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/ErrorResponse.html).

## Validação de 20/10/2026

- Backend: 9 testes de integração com Spring Boot/MockMvc, JPA real e H2 isolado. Cobrem venda/troca, normalização, preço e textos inválidos, categoria inválida, campos gerenciados pelo servidor, busca literal, paginação, parâmetros, 404/405 e carga idempotente sem sobrescrita. Testes executados em falha antes de implementar as rotas, a carga e a correção dos códigos HTTP. `mvnw.cmd package` passou e gerou o jar executável.
- Angular: 25 testes; cliente HTTP, validação, formulário, navegação e catálogo. Verificados confirmação somente após resposta, prevenção de envio duplicado pendente, manutenção dos campos em falha, ausência de fallback local, cancelamento de consultas antigas e repetição da busca. Build e typecheck passaram; bundle inicial de aproximadamente 447 kB, abaixo do budget de 500 kB.
- Chrome isolado com API/MySQL reais: cadastro de venda e troca pelo formulário, busca, ausência de resultados, paginação, prévia e recarga. Interceptação de falhas HTTP confirmou mensagem e tentativa novamente, sem sucesso fictício.
- Persistência real: ambos os anúncios criados no navegador foram lidos novamente após reiniciar a JVM e recriar o container MySQL com o mesmo volume. O total permaneceu estável e a carga não duplicou exemplos. Somente os registros temporários criados para essa verificação foram removidos ao concluir.
- Catálogo e formulário em 320, 768, 1024 e 1440 px sem overflow horizontal. axe-core não apontou violações nas regras automatizadas WCAG 2 A/AA e 2.1 A/AA; console sem erros/avisos nos fluxos normais. Isso não substitui uma avaliação manual completa de acessibilidade.
- Ferramentas de navegador permaneceram em pasta temporária, sem acrescentar dependências npm à aplicação. Credenciais, build e dados não foram versionados. Autenticação e disponibilização pública permanecem fora desta entrega.
