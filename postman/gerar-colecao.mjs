// Gera postman/projeto-integrador.postman_collection.json executando cada requisição
// contra uma API real (MySQL) e salvando a resposta obtida (equivalente ao "Save Response").
//
// Uso: node postman/gerar-colecao.mjs http://127.0.0.1:8080 postman/projeto-integrador.postman_collection.json
// A API precisa estar com DEMO_USERS_ENABLED=true (contas de teste) e banco vazio ou de teste.
import { writeFileSync } from 'node:fs';

const BASE = process.argv[2] ?? 'http://127.0.0.1:8080';
const SAIDA = process.argv[3] ?? 'postman/projeto-integrador.postman_collection.json';

// Variáveis compartilhadas (exportadas com valores, como pede o enunciado). Somente contas de teste.
const vars = {
  baseUrl: 'http://localhost:8080',
  adminEmail: 'admin@skillswap.dev',
  adminSenha: 'Admin@Teste2026',
  profissionalEmail: 'profissional@skillswap.dev',
  profissionalSenha: 'Usuario@Teste2026',
  clienteEmail: 'cliente@skillswap.dev',
  clienteSenha: 'Usuario@Teste2026',
  novoUsuarioEmail: `aluno.${Date.now()}@skillswap.dev`,
  novoUsuarioSenha: 'Aluno@Teste2026',
};
const TEXTO_STATUS = { 200: 'OK', 201: 'Created', 204: 'No Content', 400: 'Bad Request', 401: 'Unauthorized',
  403: 'Forbidden', 404: 'Not Found', 409: 'Conflict' };

const substituir = (texto) => texto.replace(/\{\{(\w+)\}\}/g, (_, k) => {
  if (!(k in vars)) throw new Error(`Variável sem valor: ${k}`);
  return String(vars[k]);
});

const pastas = new Map();
const divergencias = [];

/**
 * @param pasta nome da pasta na coleção
 * @param nome  objetivo da requisição
 * @param def   { metodo, url, auth: [usuarioVar, senhaVar] | null | 'invalida', corpo, esperado, captura }
 */
async function req(pasta, nome, { metodo = 'GET', url, auth = null, corpo, esperado, captura = {}, descricao, preRequisicao }) {
  const headers = { Accept: 'application/json' };
  const postmanAuth = auth === null
    ? { type: 'noauth' }
    : auth === 'invalida'
      ? basicPostman('{{clienteEmail}}', 'senha-incorreta')
      : basicPostman(`{{${auth[0]}}}`, `{{${auth[1]}}}`);
  if (auth === 'invalida') headers.Authorization = basic(vars.clienteEmail, 'senha-incorreta');
  else if (auth) headers.Authorization = basic(vars[auth[0]], vars[auth[1]]);
  let corpoTexto;
  if (corpo !== undefined) {
    corpoTexto = typeof corpo === 'string' ? corpo : JSON.stringify(corpo, null, 2);
    // IDs numéricos ficam sem aspas no JSON ({{anuncioId}}), como no Postman.
    corpoTexto = corpoTexto.replace(/"\{\{(\w+Id)\}\}"/g, '{{$1}}');
    headers['Content-Type'] = 'application/json';
  }
  const resposta = await fetch(BASE + substituir(url), {
    method: metodo, headers, body: corpoTexto === undefined ? undefined : substituir(corpoTexto),
  });
  const textoResposta = await resposta.text();
  let json = null;
  try { json = textoResposta ? JSON.parse(textoResposta) : null; } catch { /* não JSON */ }
  if (esperado && resposta.status !== esperado) {
    divergencias.push(`${pasta} / ${nome}: esperado ${esperado}, obtido ${resposta.status} ${textoResposta.slice(0, 300)}`);
  }
  const scripts = [];
  for (const [variavel, caminho] of Object.entries(captura)) {
    const valor = caminho.split('.').reduce((o, p) => (o == null ? o : o[p]), json);
    if (valor !== undefined && valor !== null) vars[variavel] = valor;
    scripts.push(`pm.collectionVariables.set('${variavel}', pm.response.json().${caminho});`);
  }
  const request = {
    method: metodo,
    header: corpoTexto === undefined ? [] : [{ key: 'Content-Type', value: 'application/json' }],
    auth: postmanAuth,
    url: urlPostman(url),
    ...(corpoTexto === undefined ? {} : { body: { mode: 'raw', raw: corpoTexto, options: { raw: { language: 'json' } } } }),
    ...(descricao ? { description: descricao } : {}),
  };
  const cabecalhos = [];
  for (const chave of ['content-type', 'location']) {
    const v = resposta.headers.get(chave);
    if (v) cabecalhos.push({ key: chave.replace(/(^|-)\w/g, (m) => m.toUpperCase()), value: v });
  }
  const item = {
    name: nome,
    ...(scripts.length ? { event: [{ listen: 'test', script: { type: 'text/javascript', exec: scripts } }] } : {}),
    request,
    response: [{
      name: `${resposta.status} ${TEXTO_STATUS[resposta.status] ?? ''}`.trim(),
      originalRequest: request,
      status: TEXTO_STATUS[resposta.status] ?? String(resposta.status),
      code: resposta.status,
      _postman_previewlanguage: json === null ? 'text' : 'json',
      header: cabecalhos,
      cookie: [],
      body: json === null ? textoResposta : JSON.stringify(json, null, 2),
    }],
  };
  if (preRequisicao) item.event = [...(item.event ?? []),
    { listen: 'prerequest', script: { type: 'text/javascript', exec: [preRequisicao] } }];
  if (!pastas.has(pasta)) pastas.set(pasta, []);
  pastas.get(pasta).push(item);
  console.log(`${String(resposta.status).padEnd(4)} ${pasta} / ${nome}`);
  return json;
}

function basic(u, s) { return 'Basic ' + Buffer.from(`${u}:${s}`).toString('base64'); }
function basicPostman(u, s) {
  return { type: 'basic', basic: [
    { key: 'username', value: u, type: 'string' }, { key: 'password', value: s, type: 'string' }] };
}
function urlPostman(caminho) {
  const [path, query] = caminho.split('?');
  const url = { raw: '{{baseUrl}}' + caminho, host: ['{{baseUrl}}'], path: path.replace(/^\//, '').split('/') };
  if (query) url.query = query.split('&').map((p) => { const [key, value = ''] = p.split('='); return { key, value }; });
  return url;
}

const PROF = ['profissionalEmail', 'profissionalSenha'];
const CLI = ['clienteEmail', 'clienteSenha'];
const ADM = ['adminEmail', 'adminSenha'];
const NOVO = ['novoUsuarioEmail', 'novoUsuarioSenha'];

const anuncioBase = {
  titulo: 'Aulas de violão para iniciantes',
  descricao: 'Aulas presenciais de violão popular, com repertório escolhido pelo aluno. Duração de 1 hora.',
  categoria: 'AULAS', cidade: 'Florianópolis', modalidade: 'VENDA', preco: 80.0,
  disponibilidades: [{ diaSemana: 'SABADO', periodo: 'MANHA' }],
};

// ---------------------------------------------------------------- 1. Autenticação e autorização
const A = '01 - Autenticação e autorização (HTTP Basic)';
await req(A, 'Acesso sem autenticação → 401', { url: '/api/painel', esperado: 401,
  descricao: 'Endpoint protegido chamado sem credenciais.' });
await req(A, 'Credenciais inválidas → 401', { url: '/api/painel', auth: 'invalida', esperado: 401 });
await req(A, 'Credenciais válidas (USER) → 200', { url: '/api/perfil', auth: CLI, esperado: 200,
  captura: { clienteId: 'id' } });
await req(A, 'USER em operação de ADMIN → 403', { url: '/api/usuarios', auth: CLI, esperado: 403,
  descricao: 'Listagem de usuários é restrita ao perfil ADMIN.' });
await req(A, 'ADMIN em operação de ADMIN → 200', { url: '/api/usuarios?pagina=0&tamanho=20', auth: ADM, esperado: 200 });
await req(A, 'Perfil de acesso do usuário (moderação?)', { url: '/api/moderacao/acesso', auth: CLI, esperado: 200 });
await req(A, 'Login JWT usado pelo Angular → 200', { metodo: 'POST', url: '/api/sessoes', esperado: 200,
  corpo: { email: '{{clienteEmail}}', senha: '{{clienteSenha}}' },
  descricao: 'O frontend usa Bearer JWT; o Postman usa HTTP Basic. Ambos validam as mesmas contas.' });

// ---------------------------------------------------------------- 2. Usuários
const U = '02 - Usuários (CRUD)';
await req(U, 'Criar usuário → 201', { metodo: 'POST', url: '/api/usuarios', esperado: 201,
  corpo: { nome: 'Aluno de Teste', email: '{{novoUsuarioEmail}}', senha: '{{novoUsuarioSenha}}' },
  preRequisicao: "pm.collectionVariables.set('novoUsuarioEmail', 'aluno.' + Date.now() + '@skillswap.dev');" });
const perfilNovo = await req(U, 'Consultar o próprio perfil (novo usuário) → 200', { url: '/api/perfil', auth: NOVO,
  esperado: 200, captura: { novoUsuarioId: 'id' } });
await req(U, 'Listar usuários com busca (ADMIN) → 200', { url: '/api/usuarios?busca=aluno&pagina=0&tamanho=10',
  auth: ADM, esperado: 200 });
await req(U, 'Consultar usuário por id → 200', { url: '/api/usuarios/{{novoUsuarioId}}', esperado: 200 });
await req(U, 'Atualizar usuário → 200', { metodo: 'PUT', url: '/api/usuarios/{{novoUsuarioId}}', auth: NOVO,
  esperado: 200, corpo: { nome: 'Aluno de Teste Atualizado', cidade: 'São José', bio: 'Estudante de música.' } });
await req(U, 'Atualizar usuário de outra pessoa → 403', { metodo: 'PUT', url: '/api/usuarios/{{novoUsuarioId}}',
  auth: CLI, esperado: 403, corpo: { nome: 'Tentativa indevida' } });
await req(U, 'Criar usuário com dados inválidos → 400', { metodo: 'POST', url: '/api/usuarios', esperado: 400,
  corpo: { nome: '', email: 'email-invalido', senha: '123' } });
await req(U, 'Criar usuário com e-mail já cadastrado → 409', { metodo: 'POST', url: '/api/usuarios', esperado: 409,
  corpo: { nome: 'Duplicado', email: '{{clienteEmail}}', senha: 'Outra@Senha1' } });
await req(U, 'Excluir usuário (USER) → 403', { metodo: 'DELETE', url: '/api/usuarios/{{novoUsuarioId}}', auth: CLI,
  esperado: 403 });
await req(U, 'Excluir usuário sem histórico (ADMIN) → 204', { metodo: 'DELETE', url: '/api/usuarios/{{novoUsuarioId}}',
  auth: ADM, esperado: 204 });
await req(U, 'Consultar usuário excluído → 404', { url: '/api/usuarios/{{novoUsuarioId}}', esperado: 404 });

// ---------------------------------------------------------------- 3. Anúncios
const N = '03 - Anúncios (CRUD e validações)';
await req(N, 'Criar anúncio → 201', { metodo: 'POST', url: '/api/anuncios', auth: PROF, esperado: 201,
  corpo: anuncioBase, captura: { anuncioId: 'id' } });
await req(N, 'Criar segundo anúncio (troca) → 201', { metodo: 'POST', url: '/api/anuncios', auth: PROF, esperado: 201,
  corpo: { titulo: 'Conserto de bicicletas', descricao: 'Revisão de freios, câmbio e troca de câmara.',
    categoria: 'MANUTENCAO', cidade: 'Florianópolis', modalidade: 'TROCA', habilidadeDesejada: 'Aulas de inglês' },
  captura: { anuncioTrocaId: 'id' } });
await req(N, 'Listar anúncios públicos (paginado) → 200', { url: '/api/anuncios?pagina=0&tamanho=5', esperado: 200 });
await req(N, 'Consultar anúncio por id → 200', { url: '/api/anuncios/{{anuncioId}}', esperado: 200 });
await req(N, 'Consultar anúncio inexistente → 404', { url: '/api/anuncios/999999', esperado: 404 });
await req(N, 'Atualizar anúncio → 200', { metodo: 'PUT', url: '/api/anuncios/{{anuncioId}}', auth: PROF, esperado: 200,
  corpo: { ...anuncioBase, titulo: 'Aulas de violão e canto para iniciantes', preco: 95.0 } });
await req(N, 'Atualizar anúncio de outra pessoa → 403', { metodo: 'PUT', url: '/api/anuncios/{{anuncioId}}', auth: CLI,
  esperado: 403, corpo: anuncioBase });
await req(N, 'Criar anúncio com preço inválido → 400', { metodo: 'POST', url: '/api/anuncios', auth: PROF, esperado: 400,
  corpo: { ...anuncioBase, preco: 0 } });
await req(N, 'Criar anúncio sem campos obrigatórios → 400', { metodo: 'POST', url: '/api/anuncios', auth: PROF,
  esperado: 400, corpo: { titulo: '', descricao: '', cidade: '' } });
await req(N, 'Pausar anúncio (status INATIVO) → 200', { metodo: 'PATCH', url: '/api/anuncios/{{anuncioTrocaId}}',
  auth: PROF, esperado: 200, corpo: { status: 'INATIVO' } });
await req(N, 'Listar meus anúncios → 200', { url: '/api/anuncios/meus?pagina=0&tamanho=20', auth: PROF, esperado: 200 });
await req(N, 'Excluir anúncio sem negociações → 204', { metodo: 'DELETE', url: '/api/anuncios/{{anuncioTrocaId}}',
  auth: PROF, esperado: 204 });
await req(N, 'Consultar anúncio excluído → 404', { url: '/api/anuncios/{{anuncioTrocaId}}', esperado: 404 });

// ---------------------------------------------------------------- 4. Consultas personalizadas
const C = '04 - Consultas personalizadas (Spring Data JPA)';
await req(C, 'Anúncios por categoria, cidade e termo (JPQL)', {
  url: '/api/anuncios?categoria=AULAS&cidade=Florian%C3%B3polis&busca=viol%C3%A3o&pagina=0&tamanho=5', esperado: 200 });
await req(C, 'Anúncios por faixa de preço e modalidade', {
  url: '/api/anuncios?modalidade=VENDA&precoMinimo=50&precoMaximo=120&pagina=0&tamanho=5', esperado: 200 });
await req(C, 'Anúncios por disponibilidade (sábado de manhã)', {
  url: '/api/anuncios?diaSemana=SABADO&periodo=MANHA&pagina=0&tamanho=5', esperado: 200 });
await req(C, 'Profissionais por cidade e categoria', {
  url: '/api/profissionais?cidade=Florian%C3%B3polis&categoria=AULAS&pagina=0&tamanho=12', esperado: 200 });
await req(C, 'Usuários por nome ou e-mail (método derivado)', { url: '/api/usuarios?busca=skillswap.dev&pagina=0&tamanho=10',
  auth: ADM, esperado: 200 });
await req(C, 'Filtro com faixa de preço incoerente → 400', {
  url: '/api/profissionais?precoMinimo=200&precoMaximo=100', esperado: 400 });

// ---------------------------------------------------------------- 5. Perfil profissional
const P = '05 - Perfil profissional (experiências e certificações)';
await req(P, 'Atualizar meu perfil → 200', { metodo: 'PATCH', url: '/api/perfil', auth: PROF, esperado: 200,
  corpo: { tituloProfissional: 'Professora de violão', bio: 'Dou aulas há 8 anos.', especialidades: 'Violão, canto',
    disponivelTrabalhos: true } });
await req(P, 'Criar experiência → 201', { metodo: 'POST', url: '/api/experiencias', auth: PROF, esperado: 201,
  corpo: { cargo: 'Professora de música', organizacao: 'Escola Som Livre', cidade: 'Florianópolis',
    dataInicio: '2019-03-01', descricao: 'Turmas de violão infantil e adulto.' }, captura: { experienciaId: 'id' } });
await req(P, 'Listar minhas experiências → 200', { url: '/api/experiencias', auth: PROF, esperado: 200 });
await req(P, 'Atualizar experiência → 200', { metodo: 'PUT', url: '/api/experiencias/{{experienciaId}}', auth: PROF,
  esperado: 200, corpo: { cargo: 'Coordenadora de música', organizacao: 'Escola Som Livre', cidade: 'Florianópolis',
    dataInicio: '2019-03-01', descricao: 'Coordenação pedagógica.' } });
await req(P, 'Experiência com data futura → 400', { metodo: 'POST', url: '/api/experiencias', auth: PROF, esperado: 400,
  corpo: { cargo: 'Teste', organizacao: 'Teste', dataInicio: '2099-01-01' } });
await req(P, 'Criar certificação → 201', { metodo: 'POST', url: '/api/certificacoes', auth: PROF, esperado: 201,
  corpo: { nome: 'Teoria musical', emissor: 'Conservatório', dataEmissao: '2018-12-10' },
  captura: { certificacaoId: 'id' } });
await req(P, 'Excluir certificação → 204', { metodo: 'DELETE', url: '/api/certificacoes/{{certificacaoId}}', auth: PROF,
  esperado: 204 });
await req(P, 'Excluir experiência → 204', { metodo: 'DELETE', url: '/api/experiencias/{{experienciaId}}', auth: PROF,
  esperado: 204 });

// ---------------------------------------------------------------- 6. Fluxo principal
const F = '06 - Funcionalidade do PI: negociação (proposta → acordo → conclusão → avaliação)';
await req(F, 'Regra: profissional não negocia o próprio anúncio → 400', { metodo: 'POST',
  url: '/api/anuncios/{{anuncioId}}/conversas', auth: PROF, esperado: 400, corpo: { conteudo: 'Teste' } });
await req(F, '1. Cliente abre conversa pelo anúncio → 201', { metodo: 'POST', url: '/api/anuncios/{{anuncioId}}/conversas',
  auth: CLI, esperado: 201, corpo: { conteudo: 'Olá! Você tem horário no sábado de manhã?' },
  captura: { conversaId: 'id' } });
await req(F, '2. Profissional responde na conversa → 201', { metodo: 'POST', url: '/api/conversas/{{conversaId}}/mensagens',
  auth: PROF, esperado: 201, corpo: { conteudo: 'Tenho sim, às 9h. Pode enviar a proposta.' } });
await req(F, '3. Cliente envia proposta parcelada → 201', { metodo: 'POST', url: '/api/conversas/{{conversaId}}/propostas',
  auth: CLI, esperado: 201, captura: { propostaId: 'id' }, corpo: { anuncioId: '{{anuncioId}}', servico: 'Aulas de violão',
    escopo: 'Quatro aulas de 1 hora aos sábados.', formaPagamento: 'PARCELADO', valor: 320.0, parcelas: 4,
    duracao: '1 mês', validadeEm: '2027-01-31' } });
await req(F, 'Proposta com parcelas inválidas → 400', { metodo: 'POST', url: '/api/conversas/{{conversaId}}/propostas',
  auth: CLI, esperado: 400, corpo: { anuncioId: '{{anuncioId}}', servico: 'Aulas', escopo: 'Teste',
    formaPagamento: 'PARCELADO', valor: 100, parcelas: 30 } });
await req(F, 'Propostas recebidas pelo profissional → 200', { url: '/api/propostas?direcao=RECEBIDAS&pagina=0&tamanho=20',
  auth: PROF, esperado: 200 });
await req(F, 'Cliente não pode aceitar a própria proposta → 403', { metodo: 'PATCH',
  url: '/api/propostas/{{propostaId}}/aceitar', auth: CLI, esperado: 403 });
await req(F, '4. Profissional aceita a proposta (gera acordo) → 200', { metodo: 'PATCH',
  url: '/api/propostas/{{propostaId}}/aceitar', auth: PROF, esperado: 200 });
await req(F, 'Aceitar proposta já aceita → 409', { metodo: 'PATCH', url: '/api/propostas/{{propostaId}}/aceitar',
  auth: PROF, esperado: 409 });
const acordos = await req(F, '5. Listar meus acordos → 200', { url: '/api/acordos/meus', auth: CLI, esperado: 200 });
if (Array.isArray(acordos) && acordos[0]) vars.acordoId = acordos[0].id;
pastas.get(F).at(-1).event = [{ listen: 'test', script: { type: 'text/javascript',
  exec: ["pm.collectionVariables.set('acordoId', pm.response.json()[0].id);"] } }];
await req(F, 'Avaliar antes da conclusão → 409', { metodo: 'POST', url: '/api/acordos/{{acordoId}}/avaliacoes', auth: CLI,
  esperado: 409, corpo: { nota: 5, notaAnuncio: 5, depoimento: 'Ainda não concluído.' } });
await req(F, '6. Profissional confirma a conclusão → 200', { metodo: 'PATCH', url: '/api/acordos/{{acordoId}}/concluir',
  auth: PROF, esperado: 200 });
await req(F, '7. Cliente confirma a conclusão (acordo CONCLUIDO) → 200', { metodo: 'PATCH',
  url: '/api/acordos/{{acordoId}}/concluir', auth: CLI, esperado: 200 });
await req(F, 'Avaliação com nota fora do intervalo → 400', { metodo: 'POST', url: '/api/acordos/{{acordoId}}/avaliacoes',
  auth: CLI, esperado: 400, corpo: { nota: 7, depoimento: 'Nota inválida' } });
await req(F, '8. Cliente avalia o profissional → 201', { metodo: 'POST', url: '/api/acordos/{{acordoId}}/avaliacoes',
  auth: CLI, esperado: 201, captura: { avaliacaoId: 'id' },
  corpo: { nota: 5, notaAnuncio: 5, depoimento: 'Aulas muito bem explicadas e pontuais.' } });
await req(F, 'Avaliar o mesmo acordo novamente → 409', { metodo: 'POST', url: '/api/acordos/{{acordoId}}/avaliacoes',
  auth: CLI, esperado: 409, corpo: { nota: 4 } });
await req(F, 'Consultar acordo concluído → 200', { url: '/api/acordos/{{acordoId}}', auth: PROF, esperado: 200 });
await req(F, 'Avaliações públicas do anúncio → 200', { url: '/api/anuncios/{{anuncioId}}/avaliacoes', esperado: 200 });
await req(F, 'Excluir anúncio com negociação → 409', { metodo: 'DELETE', url: '/api/anuncios/{{anuncioId}}', auth: PROF,
  esperado: 409 });
await req(F, 'Excluir usuário com histórico (ADMIN) → 409', { metodo: 'DELETE', url: '/api/usuarios/{{clienteId}}',
  auth: ADM, esperado: 409 });

// ---------------------------------------------------------------- 7. Comentários
const M = '07 - Comentários do anúncio (CRUD e regra)';
await req(M, 'Criar comentário → 201', { metodo: 'POST', url: '/api/anuncios/{{anuncioId}}/comentarios', auth: CLI,
  esperado: 201, corpo: { conteudo: 'As aulas incluem material?' }, captura: { comentarioId: 'id' } });
await req(M, 'Listar comentários → 200', { url: '/api/anuncios/{{anuncioId}}/comentarios', esperado: 200 });
await req(M, 'Editar comentário → 200', { metodo: 'PUT', url: '/api/anuncios/{{anuncioId}}/comentarios/{{comentarioId}}',
  auth: CLI, esperado: 200, corpo: { conteudo: 'As aulas incluem material de apoio?' } });
await req(M, 'Editar comentário de outra pessoa → 403', { metodo: 'PUT',
  url: '/api/anuncios/{{anuncioId}}/comentarios/{{comentarioId}}', auth: PROF, esperado: 403, corpo: { conteudo: 'x' } });
await req(M, 'Anunciante responde ao comentário → 201', { metodo: 'POST', url: '/api/anuncios/{{anuncioId}}/comentarios',
  auth: PROF, esperado: 201, corpo: { conteudo: 'Sim, envio apostilas em PDF.', respostaAId: '{{comentarioId}}' },
  captura: { respostaId: 'id' } });
await req(M, 'Excluir comentário com respostas → 409', { metodo: 'DELETE',
  url: '/api/anuncios/{{anuncioId}}/comentarios/{{comentarioId}}', auth: CLI, esperado: 409 });
await req(M, 'Excluir a resposta → 204', { metodo: 'DELETE', url: '/api/anuncios/{{anuncioId}}/comentarios/{{respostaId}}',
  auth: PROF, esperado: 204 });
await req(M, 'Excluir comentário inexistente → 404', { metodo: 'DELETE',
  url: '/api/anuncios/{{anuncioId}}/comentarios/999999', auth: CLI, esperado: 404 });

// ---------------------------------------------------------------- 8. Dashboard
const D = '08 - Dashboard (KPIs)';
await req(D, 'KPIs do profissional → 200', { url: '/api/painel', auth: PROF, esperado: 200 });
await req(D, 'KPIs do cliente → 200', { url: '/api/painel', auth: CLI, esperado: 200 });
await req(D, 'KPIs da plataforma (ADMIN) → 200', { url: '/api/painel/plataforma', auth: ADM, esperado: 200 });
await req(D, 'KPIs da plataforma (USER) → 403', { url: '/api/painel/plataforma', auth: CLI, esperado: 403 });

// ---------------------------------------------------------------- 9. Moderação
const O = '09 - Moderação (perfil ADMIN)';
await req(O, 'Denunciar comentário → 201', { metodo: 'POST', url: '/api/denuncias', auth: PROF, esperado: 201,
  corpo: { tipoConteudo: 'COMENTARIO_ANUNCIO', recursoId: '{{comentarioId}}', motivo: 'SPAM', descricao: 'Teste de moderação' },
  captura: { denunciaId: 'id' } });
await req(O, 'Denunciar o mesmo conteúdo novamente → 409', { metodo: 'POST', url: '/api/denuncias', auth: PROF,
  esperado: 409, corpo: { tipoConteudo: 'COMENTARIO_ANUNCIO', recursoId: '{{comentarioId}}', motivo: 'SPAM' } });
await req(O, 'Fila de denúncias (USER) → 403', { url: '/api/moderacao/denuncias?status=ABERTA', auth: CLI, esperado: 403 });
await req(O, 'Fila de denúncias (ADMIN) → 200', { url: '/api/moderacao/denuncias?status=ABERTA', auth: ADM, esperado: 200 });
await req(O, 'Descartar denúncia (ADMIN) → 200', { metodo: 'PATCH', url: '/api/moderacao/denuncias/{{denunciaId}}',
  auth: ADM, esperado: 200, corpo: { acao: 'DESCARTAR' } });
await req(O, 'Decidir denúncia já analisada → 409', { metodo: 'PATCH', url: '/api/moderacao/denuncias/{{denunciaId}}',
  auth: ADM, esperado: 409, corpo: { acao: 'DESCARTAR' } });

// ---------------------------------------------------------------- Coleção
const variaveisFinais = Object.entries(vars).map(([key, value]) => ({ key, value: String(value), type: 'string' }));
const colecao = {
  info: {
    _postman_id: 'b6a7f0e2-4c1d-4f6e-9b8a-5c3d2e1f0a9b',
    name: 'SkillSwap - Projeto Integrador (API REST)',
    description: [
      'Coleção da API REST do SkillSwap (Spring Boot + MySQL).',
      'Cada requisição tem a resposta real salva (Save Response), obtida em ' + new Date().toISOString().slice(0, 10) + '.',
      'Autenticação: HTTP Basic com as contas de teste criadas por DEMO_USERS_ENABLED=true',
      '(ADMIN: {{adminEmail}}; USER: {{profissionalEmail}} e {{clienteEmail}}). Senhas exclusivas de teste.',
      'Execute as pastas em ordem: os scripts de teste guardam os IDs criados nas variáveis da coleção.',
      'Para repetir, gere um novo {{novoUsuarioEmail}} (ex.: aluno.2@skillswap.dev).',
    ].join('\n'),
    schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
  },
  item: [...pastas.entries()].map(([name, item]) => ({ name, item })),
  variable: variaveisFinais,
};
writeFileSync(SAIDA, JSON.stringify(colecao, null, 2) + '\n');
console.log(`\nColeção gravada em ${SAIDA} com ${[...pastas.values()].flat().length} requisições.`);
if (divergencias.length) {
  console.log('\nDIVERGÊNCIAS:');
  divergencias.forEach((d) => console.log(' - ' + d));
  process.exitCode = 1;
}
