# Interações em publicações do portfólio

Data: 30/10/2026

## Diagnóstico

O SkillSwap já persiste publicações de portfólio com até seis fotos, legenda opcional, anúncio vinculado, moderação e visibilidade herdada do perfil. A API lista publicações públicas paginadas e o titular gerencia as próprias publicações. Não existem curtidas nem comentários nessas publicações. Comentários em anúncios já definem uma regra reutilizável de respostas em uma camada, leitura pública, escrita autenticada e denúncia/moderação.

## Contrato escolhido para esta entrega

- Estender a publicação de portfólio existente; não criar outro feed nem aceitar vídeo.
- `GET /api/perfis/{id}/publicacoes` e `GET /api/publicacoes` informam `totalCurtidas` e `curtidaPeloUsuario`; esta última é falsa para visitantes. A contagem é agregada e não publica a lista de curtidores.
- `PUT` e `DELETE /api/publicacoes/{id}/curtida` criam ou removem a curtida do usuário autenticado. `PUT` é idempotente; `DELETE` também é seguro para recurso já ausente. Ambas retornam o resumo atualizado.
- `GET /api/publicacoes/{id}/comentarios?pagina=0&tamanho=30` lista comentários visíveis em páginas, sem revelar e-mail nem ID privado do autor.
- `POST /api/publicacoes/{id}/comentarios` exige sessão, aceita texto simples de até 1000 caracteres e `respostaAId` opcional. Só o autor da raiz ou o dono da publicação pode responder a uma raiz visível; respostas em segundo nível são recusadas.
- Comentários seguem a visibilidade pública da publicação e do perfil; publicação ou perfil pausado resulta em `404`. Usuários pausados podem ler enquanto a publicação não existir publicamente, mas sua identidade aparece como “Usuário SkillSwap” nos comentários.
- Denúncias usam a fila e decisões atuais de moderação via novo tipo `COMENTARIO_PUBLICACAO`; ocultar/reexibir opera no comentário e preserva o histórico.
- A migração é aditiva e não altera registros anteriores. Consultas em lote evitam uma consulta de curtidas para cada publicação.

## Incrementos e aceite

### 1. Curtidas

- [x] V22 cria relação única usuário/publicação e índice de consulta.
- [x] Serviço REST exige autenticação e valida publicação/perfil visíveis.
- [x] Listas paginadas incluem contagem e estado do usuário sem N+1.
- [x] Testes cobrem visitante, idempotência, remoção, publicação oculta e isolamento entre contas.
- [x] Perfil público permite curtir/deixar de curtir, atualizando contagem somente após resposta do servidor.

### 2. Comentários e moderação

- [x] Leitura pública paginada e escrita autenticada com validação de texto.
- [x] Resposta de uma camada apenas pelo autor da raiz ou dono da publicação.
- [x] Perfil/publicação pausados não vazam texto, autores nem imagens.
- [x] Denúncia e moderação ocultam/reexibem comentários sem apagar dados.
- [x] UI oferece estados vazios, erro/repetir, envio, resposta e denúncia.
- [x] Testes cobrem autorização, visibilidade, paginação, validação e moderação.

### 3. Verificação e registro

- [x] Angular: `npm run typecheck`, `npm run build` sem avisos de budget, `npm run test:ci` (131 testes).
- [x] Java 21: `./mvnw.cmd test` (97 testes). H2 aplicou V22–V24; MySQL local indisponível (Docker Desktop parado).
- [x] Chromium 149 verificou perfil público em 390 e 1440 px; sem erros de console ou página e sem overflow visível.
- [x] Revisão de segurança e diff; atualizar planos e estado sem alegar verificação manual de leitor de tela.
- [x] Commit da fatia de curtidas e comentários: `2b53907`.
- [x] Commit das experiências/certificações (`dcc2850`), ensaio V22–V24 em MySQL 8.0.46 isolado, envio ao GitHub e publicação; backups e tags de rollback preservados.

## Próximas fatias do perfil social

Experiências e certificações autodeclaradas permanecem em tarefa separada: perfil titular cria/edita/remove, visitantes só leem dados enquanto o perfil está público, sem documentos, dados sensíveis ou selo de verificação. Vídeos permanecem bloqueados até plano operacional de armazenamento e moderação. Nenhuma avaliação é criada sem acordo concluído.
