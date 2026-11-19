# Documentação da API REST

Base local: `http://localhost:8080` (o Angular acessa por `/api` via proxy). Todas as respostas usam JSON e as datas seguem ISO 8601. Listagens paginadas devolvem `{ itens, pagina, tamanho, totalItens, totalPaginas }`.

A coleção [`postman/projeto-integrador.postman_collection.json`](../postman/projeto-integrador.postman_collection.json) contém 83 requisições organizadas por objetivo, com as respostas reais salvas (geradas contra MySQL 8.0.46).

## Autenticação e autorização

| Mecanismo | Uso | Como enviar |
| --- | --- | --- |
| HTTP Basic | Postman e clientes de API | `Authorization: Basic base64(email:senha)` |
| Bearer JWT | Frontend Angular | `POST /api/sessoes` devolve `{ token }`; enviar `Authorization: Bearer <token>` |

Perfis de acesso:

| Perfil | Quem recebe | Permissões exclusivas |
| --- | --- | --- |
| `USER` | Toda conta autenticada | Anunciar, negociar, comentar, avaliar, denunciar, ver o próprio painel |
| `ADMIN` | E-mails em `MODERATOR_EMAILS` e o administrador de teste | Listar e excluir usuários, fila de moderação, decidir denúncias e contestações, painel da plataforma |

Contas de teste (criadas com `DEMO_USERS_ENABLED=true`; senhas configuráveis por ambiente):

| E-mail | Senha padrão local | Perfil |
| --- | --- | --- |
| `admin@skillswap.dev` | `Admin@Teste2026` | ADMIN + USER |
| `profissional@skillswap.dev` | `Usuario@Teste2026` | USER |
| `cliente@skillswap.dev` | `Usuario@Teste2026` | USER |

- **401 Unauthorized**: sem credenciais, senha incorreta ou token inválido em rota protegida.
- **403 Forbidden**: autenticado, mas sem o perfil exigido (ex.: `USER` em `GET /api/usuarios`) ou sem ser dono do recurso (ex.: editar anúncio de outra pessoa).

## Formato de erro

```json
{ "codigo": "DADOS_INVALIDOS", "mensagem": "Revise os campos informados.",
  "campos": { "email": "Email inválido", "senha": "Senha deve ter entre 6 e 100 caracteres" } }
```

| Status | `codigo` | Quando |
| --- | --- | --- |
| 400 | `DADOS_INVALIDOS` | Bean Validation (`@Valid`), JSON malformado, parâmetro inválido, regra de entrada |
| 401 | `NAO_AUTENTICADO` | Falta de autenticação ou credenciais inválidas |
| 403 | `ACESSO_NEGADO` | Perfil ou propriedade insuficiente |
| 404 | `NAO_ENCONTRADO` | Recurso inexistente (`NaoEncontradoException`) |
| 409 | `CONFLITO` | Regra de negócio ou estado atual (`ConflitoException`) |

O tratamento é centralizado em `infra/exception/GlobalExceptionHandler` (`@RestControllerAdvice`). As exceções personalizadas são `NaoEncontradoException`, `ConflitoException` e `NaoAutorizadoException`.

## Endpoints por recurso

Legenda: 🔓 público · 🔑 autenticado (USER) · 🛡️ ADMIN.

### Sessão e usuários

| Método | Caminho | Acesso | Descrição |
| --- | --- | --- | --- |
| POST | `/api/usuarios` | 🔓 | Cria conta (201 + `Location`); 409 se o e-mail já existir |
| POST | `/api/sessoes` | 🔓 | Login JWT; `DELETE /api/sessoes/atual` encerra a sessão persistente |
| GET | `/api/usuarios` | 🛡️ | Lista usuários; `?busca=` filtra por nome ou e-mail |
| GET | `/api/usuarios/{id}` | 🔓 | Perfil público |
| PUT | `/api/usuarios/{id}` | 🔑 | Atualiza o próprio perfil; 403 para outra conta |
| DELETE | `/api/usuarios/{id}` | 🛡️ | Exclui conta sem histórico (204); 409 se houver anúncios, negociações ou conteúdo |
| GET / PATCH | `/api/perfil` | 🔑 | Perfil da conta autenticada |
| PUT / DELETE | `/api/perfil/foto`, `/api/perfil/capa` | 🔑 | Envio (multipart) e remoção de imagens |

### Anúncios

| Método | Caminho | Acesso | Descrição |
| --- | --- | --- | --- |
| GET | `/api/anuncios` | 🔓 | Catálogo paginado com filtros (ver consultas personalizadas) |
| GET | `/api/anuncios/{id}` | 🔓 | Detalhe |
| POST | `/api/anuncios` | 🔑 | Cria (201); valida preço, categoria, modalidade e disponibilidade |
| PUT | `/api/anuncios/{id}` | 🔑 dono | Atualiza todos os campos |
| PATCH | `/api/anuncios/{id}` | 🔑 dono | Altera status (`ATIVO`/`INATIVO`) |
| DELETE | `/api/anuncios/{id}` | 🔑 dono | Exclui (204); 409 se houver negociações, conversas ou comentários — nesse caso, pausar com PATCH |
| GET | `/api/anuncios/meus`, `/api/anuncios/meus/{id}` | 🔑 | Anúncios da conta, inclusive pausados |
| GET / POST / PUT / DELETE | `/api/anuncios/{id}/comentarios[/{comentarioId}]` | 🔓 leitura / 🔑 autor | Comentários e respostas; 409 ao excluir comentário com respostas |
| GET | `/api/anuncios/{id}/avaliacoes` | 🔓 | Avaliações verificadas do anúncio |
| GET | `/api/categorias`, `/api/vitrine` | 🔓 | Categorias e coleções da página inicial |

### Negociação (funcionalidade específica do PI)

| Método | Caminho | Acesso | Descrição |
| --- | --- | --- | --- |
| POST | `/api/anuncios/{id}/conversas` | 🔑 | Primeiro contato pelo anúncio (201) |
| GET | `/api/conversas`, `/api/conversas/{id}` | 🔑 participante | Conversas e resumo de não lidas (`/resumo`) |
| GET / POST | `/api/conversas/{id}/mensagens` | 🔑 participante | Mensagens (imutáveis, para preservar o histórico) |
| PATCH | `/api/conversas/{id}/leitura` | 🔑 participante | Marca leitura (204) |
| POST | `/api/conversas/{id}/propostas` | 🔑 | Proposta à vista, parcelada (2–12), por hora, por dia, assinatura mensal ou por troca, com validade |
| GET | `/api/propostas?direcao=ENVIADAS\|RECEBIDAS`, `/api/propostas/{id}` | 🔑 | Consulta |
| PATCH | `/api/propostas/{id}` | 🔑 proponente | `{"status":"CANCELADA"}` retira proposta pendente |
| PATCH | `/api/propostas/{id}/aceitar` · `/recusar` | 🔑 anunciante | Aceite gera o acordo; 409 se não estiver pendente |
| GET | `/api/acordos`, `/api/acordos/meus`, `/api/acordos/{id}`, `/api/acordos/{id}/eventos` | 🔑 participante | Acordos e linha do tempo |
| PATCH | `/api/acordos/{id}/concluir` · `/cancelar` | 🔑 participante | Conclusão exige confirmação das duas partes |
| POST | `/api/acordos/{id}/avaliacoes` | 🔑 participante | Avaliação verificada (1–5) após conclusão; 409 antes ou em duplicidade |
| PUT | `/api/acordos/{id}/avaliacoes/{avaliacaoId}` | 🔑 autor | Edição única |
| PATCH | `/api/acordos/{id}/avaliacoes/{avaliacaoId}/resposta` | 🔑 avaliado | Resposta profissional |

### Perfil profissional e portfólio

| Método | Caminho | Acesso | Descrição |
| --- | --- | --- | --- |
| GET / POST / PUT / DELETE | `/api/experiencias[/{id}]` | 🔑 dono | CRUD de experiências (datas não podem ser futuras) |
| GET / POST / PUT / DELETE | `/api/certificacoes[/{id}]` | 🔑 dono | CRUD de certificações |
| GET / POST / PATCH / DELETE | `/api/publicacoes[/{id}]` | 🔑 dono | Portfólio com fotos enviadas em `/api/uploads` |
| PUT / DELETE | `/api/publicacoes/{id}/curtida` | 🔑 | Curtir e desfazer |
| GET / POST / PUT / DELETE | `/api/publicacoes/{id}/comentarios[/{comentarioId}]` | 🔓 leitura / 🔑 autor | Comentários do portfólio |
| PUT / DELETE | `/api/perfis/{id}/seguidores/me` | 🔑 | Seguir e deixar de seguir |
| GET | `/api/perfis/{id}`, `/anuncios`, `/publicacoes`, `/experiencias`, `/certificacoes` | 🔓 | Perfil público |

### Dashboard

| Método | Caminho | Acesso | KPIs |
| --- | --- | --- | --- |
| GET | `/api/painel` | 🔑 | Anúncios ativos e pausados, propostas pendentes recebidas e enviadas, acordos em andamento e concluídos, taxa de aceitação (%), nota média e total de avaliações |
| GET | `/api/painel/plataforma` | 🛡️ | Usuários, anúncios ativos, propostas e acordos por situação, taxa de conclusão dos acordos (%), média geral e anúncios por categoria |

### Moderação

| Método | Caminho | Acesso | Descrição |
| --- | --- | --- | --- |
| POST | `/api/denuncias` | 🔑 | Denuncia publicação, avaliação ou comentário; 409 em duplicidade |
| GET | `/api/denuncias/{id}` | 🔑 autor | Acompanha a denúncia |
| GET | `/api/moderacao/acesso` | 🔑 | Informa se a conta é ADMIN (usado pelo Angular) |
| GET / PATCH | `/api/moderacao/denuncias[/{id}]` | 🛡️ | Fila e decisão `OCULTAR`, `DESCARTAR` ou `REEXIBIR`; 409 se já decidida |
| GET / PATCH | `/api/moderacao/contestacoes[/{id}]` | 🛡️ | Contestações de avaliações |
| GET | `/api/moderacao/avaliacoes/{id}/historico` | 🛡️ | Versões anteriores de avaliações editadas |

## Regras de negócio (camada Service)

| Regra | Onde | Resposta |
| --- | --- | --- |
| Anúncio de venda exige preço maior que zero; de troca exige habilidade desejada | `AnuncioService` + Bean Validation | 400 |
| Ninguém negocia o próprio anúncio | `PropostaService`, `ConversaService` | 400 |
| Uma proposta ativa por cliente e anúncio | `PropostaService` | 409 |
| Parcelado: 2 a 12 parcelas; demais formas de cobrança sem parcelas; troca só em anúncio de troca; validade não pode estar no passado | `PropostaService` | 400 |
| Somente o anunciante aceita ou recusa, e só propostas pendentes; aceite concorrente gera um único acordo (bloqueio pessimista) | `PropostaService` | 403 / 409 |
| Conclusão do acordo exige confirmação das duas partes | `AcordoService` | 409 se não estiver em andamento |
| Avaliação somente após a conclusão, uma por participante, nota 1–5 | `AvaliacaoService` | 409 / 400 |
| Anúncio com negociações, conversas ou comentários não pode ser excluído (deve ser pausado) | `AnuncioService` | 409 |
| Comentário com respostas não pode ser excluído | `ComentarioAnuncioService`, `ComentarioPublicacaoService` | 409 |
| Conta com histórico de negócio não pode ser excluída; ADMIN não exclui a si mesmo | `UsuarioService` | 409 |
| Denúncia única por autor e conteúdo; decisão apenas sobre denúncia aberta | `ModeracaoService` | 409 |

## Consultas personalizadas (Spring Data JPA)

| Consulta | Repositório | Tipo |
| --- | --- | --- |
| Catálogo por categoria, cidade, modalidade, faixa de preço, dia/período e termo | `AnuncioRepository.pesquisar` | `@Query` JPQL |
| Profissionais por termo, cidade, categoria, modalidade, preço e nota mínima | `UsuarioRepository.buscarProfissionais` | `@Query` JPQL |
| Usuários por nome ou e-mail | `UsuarioRepository.findByNomeContainingIgnoreCaseOrEmailContainingIgnoreCase` | Método derivado |
| Indicadores do dashboard (contagens, médias e agrupamentos) | `PainelRepository` | `@Query` JPQL |
| Vínculos que impedem excluir uma conta ou um anúncio | `UsuarioRepository.contarVinculosDeNegocio`, `AnuncioRepository.contarVinculos` | `@Query` nativa |
| Resumo de avaliações por profissional e por anúncio | `AvaliacaoRepository` | `@Query` JPQL |
