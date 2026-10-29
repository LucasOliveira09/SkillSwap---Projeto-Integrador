# ADR-006: Restaurar o login com cookie de sessão persistente

Status: implementada em 28/10/2026 a pedido do usuário; substitui o encerramento obrigatório da sessão ao recarregar.

## Decisão

- O JWT de acesso continua somente na memória do Angular, enviado como Bearer apenas a `/api/` da própria origem; sua validade passa a 15 minutos. Não usar localStorage ou sessionStorage para credenciais.
- Login/cadastro web solicitam persistência por `X-SkillSwap-Session: 1`. A API cria 32 bytes aleatórios e emite cookie `skillswap_session`, HttpOnly, SameSite=Strict, host-only, Path=/api/sessoes, Max-Age=30 dias. Secure é true por padrão e obrigatório no público HTTPS. Clientes Bearer sem o header continuam recebendo JWT sem cookie.
- Flyway V15 cria `sessoes_persistentes` com usuário, SHA-256 do segredo e expiração absoluta UTC. O segredo em texto não é salvo nem registrado. Sessões expiradas são limpas ao criar uma sessão. A renovação não estende os 30 dias.
- `POST /api/sessoes/atual` restaura/renova o JWT pelo cookie. Sem cookie, retorna 204; inválido/expirado, 401 e cookie removido. `DELETE /api/sessoes/atual` revoga a sessão e remove o cookie, inclusive se repetido. Respostas de autenticação usam no-store.
- Os endpoints que aceitam o cookie exigem o header próprio, rejeitam Sec-Fetch-Site=cross-site e continuam submetidos ao CORS com origens explícitas. Formulários externos sem header não podem renovar nem revogar sessões. Os demais endpoints continuam exigindo Bearer e não autenticam pelo cookie.
- A inicialização real em `main.ts` aguarda a restauração antes de navegar. O interceptor compartilha uma única renovação para vários 401 simultâneos e repete cada requisição uma vez. Falha 503 não provoca um falso logout. A saída só é confirmada depois da revogação no servidor, com mensagem e nova tentativa em caso de falha.

## Consequências e limites

- Recarregar o chat ou reabrir o navegador não exige novo login durante a validade da sessão; após 30 dias ou saída explícita, autenticar novamente.
- Logout revoga o segredo persistente. JWTs já emitidos em outras abas permanecem válidos até sua expiração de até 15 minutos; não há revogação instantânea de todos os tokens de acesso.
- Desenvolvimento HTTP usa SESSION_COOKIE_SECURE=false apenas em ambiente local/controlado. HTTPS público mantém true. Cookies essenciais à autenticação não contêm perfil, conversa ou senha.
- Rascunhos do chat ficam na memória durante a navegação entre conversas; não sobrevivem ao recarregamento. A atualização de mensagens usa consulta a cada oito segundos enquanto a página está visível e na página recente, sem indicar presença online ou confirmação de leitura.

## Referências

- [CSRF no Spring Security](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).
- [Inicialização no Angular](https://angular.dev/api/core/provideAppInitializer).
- [Interceptores HTTP no Angular](https://angular.dev/guide/http/interceptors).
