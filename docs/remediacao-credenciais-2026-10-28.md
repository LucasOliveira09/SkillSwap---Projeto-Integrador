# Remediação de credenciais da VPS — 28/10/2026

Durante um diagnóstico da publicação, a saída de uma ferramenta exibiu variáveis de ambiente do MySQL, incluindo credenciais ativas. O diagnóstico não executou consultas nem alterou dados. As credenciais foram tratadas como comprometidas e rotacionadas imediatamente.

## Ações aplicadas

- Criada uma conta de aplicação com credencial nova e privilégios limitados ao schema `skillswap`; a conta antiga da aplicação foi removida.
- Senhas de `root` no MySQL foram rotacionadas. Os novos valores estão somente nos arquivos `.env` da VPS, com permissão `0600`; nenhum valor foi incluído neste repositório.
- API pública recriada com a mesma imagem em uso. MySQL recriado com o volume nomeado existente, sem apagar o volume nem executar migrações.
- Compose operacional e sua cópia de rollback passaram a referenciar variáveis de ambiente. O snapshot anterior de containers foi redigido para remover valores sensíveis.
- O proxy público, o schema e os registros de negócio não foram alterados.

## Verificações

- API e MySQL saudáveis depois da rotação; `GET /api/anuncios` respondeu HTTP 200.
- A conta nova autenticou no banco; a conta anterior não existe mais.
- O volume `skillswap-integrador_skillswap-mysql-data` foi preservado. O schema continuou com sete tabelas e os 14 anúncios existentes.
- Os Compose ativos validaram sem exibir valores; os arquivos operacionais sanitizados foram verificados.

Os valores antigos permanecem em commits históricos do repositório operacional, mas foram invalidados. Não foi reescrita a história compartilhada do Git nem feita publicação de commits como parte desta contenção.
