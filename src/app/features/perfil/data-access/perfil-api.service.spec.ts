import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PerfilApiService } from './perfil-api.service';

describe('PerfilApiService', () => {
  let api: PerfilApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(PerfilApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta o perfil público sem solicitar dados privados', () => {
    api.publico(12).subscribe((perfil) => {
      expect(perfil.nome).toBe('Ana Profissional');
      expect(perfil.quantidadeAvaliacoes).toBe(0);
    });

    const request = http.expectOne('/api/perfis/12');
    expect(request.request.method).toBe('GET');
    request.flush({
      id: 12,
      nome: 'Ana Profissional',
      tituloProfissional: null,
      bio: null,
      cidade: 'Florianópolis',
      fotoUrl: null,
      capaUrl: null,
      especialidades: 'Aulas de violão',
      criadoEm: '2026-10-01T10:00:00',
      mediaNotas: null,
      quantidadeAvaliacoes: 0,
    });
  });

  it('mantém a trajetória profissional em recursos próprios e públicos separados', () => {
    const experiencia = { cargo: 'Designer', organizacao: 'Estúdio', cidade: null,
      dataInicio: '2022-01-01', dataFim: null, descricao: null };
    api.experiencias(12).subscribe(itens => expect(itens[0].cargo).toBe('Designer'));
    http.expectOne('/api/perfis/12/experiencias').flush([{ id: 5, ...experiencia }]);
    api.minhasExperiencias().subscribe();
    http.expectOne('/api/experiencias').flush([]);
    api.criarExperiencia(experiencia).subscribe();
    const criarExperiencia = http.expectOne('/api/experiencias');
    expect(criarExperiencia.request.method).toBe('POST');
    criarExperiencia.flush({ id: 5, ...experiencia });
    api.atualizarExperiencia(5, experiencia).subscribe();
    const atualizarExperiencia = http.expectOne('/api/experiencias/5');
    expect(atualizarExperiencia.request.method).toBe('PUT');
    atualizarExperiencia.flush({ id: 5, ...experiencia });

    const certificacao = { nome: 'UX', emissor: 'Escola', dataEmissao: '2023-01-01',
      dataValidade: null, urlCredencial: null };
    api.certificacoes(12).subscribe();
    http.expectOne('/api/perfis/12/certificacoes').flush([{ id: 8, ...certificacao }]);
    api.minhasCertificacoes().subscribe();
    http.expectOne('/api/certificacoes').flush([]);
    api.criarCertificacao(certificacao).subscribe();
    const criarCertificacao = http.expectOne('/api/certificacoes');
    expect(criarCertificacao.request.method).toBe('POST');
    criarCertificacao.flush({ id: 8, ...certificacao });
    api.atualizarCertificacao(8, certificacao).subscribe();
    const atualizarCertificacao = http.expectOne('/api/certificacoes/8');
    expect(atualizarCertificacao.request.method).toBe('PUT');
    atualizarCertificacao.flush({ id: 8, ...certificacao });
    api.excluirExperiencia(5).subscribe();
    const excluirExperiencia = http.expectOne('/api/experiencias/5');
    expect(excluirExperiencia.request.method).toBe('DELETE');
    excluirExperiencia.flush(null);
    api.excluirCertificacao(8).subscribe();
    const excluirCertificacao = http.expectOne('/api/certificacoes/8');
    expect(excluirCertificacao.request.method).toBe('DELETE');
    excluirCertificacao.flush(null);
  });

  it('consulta o uso de armazenamento da conta autenticada', () => {
    api.usoArmazenamento().subscribe((uso) => {
      expect(uso.usoBytes).toBe(4096);
      expect(uso.limiteBytes).toBe(104857600);
    });
    const request = http.expectOne('/api/uploads/uso');
    expect(request.request.method).toBe('GET');
    request.flush({ usoBytes: 4096, limiteBytes: 104857600, limitePorArquivoBytes: 5242880 });
  });

  it('busca perfis profissionais com termo, cidade e paginação', () => {
    api.buscarProfissionais('Eletricista', 'Florianópolis', 1).subscribe((pagina) => {
      expect(pagina.totalItens).toBe(1);
      expect(pagina.itens[0].nome).toBe('Ana Profissional');
    });

    const request = http.expectOne(
      (candidate) =>
        candidate.url === '/api/profissionais' &&
        candidate.params.get('busca') === 'Eletricista' &&
        candidate.params.get('cidade') === 'Florianópolis' &&
        candidate.params.get('pagina') === '1' &&
        candidate.params.get('tamanho') === '12',
    );
    expect(request.request.method).toBe('GET');
    request.flush({
      itens: [{
        id: 12,
        nome: 'Ana Profissional',
        tituloProfissional: null,
        bio: null,
        cidade: 'Florianópolis',
        fotoUrl: null,
        capaUrl: null,
        especialidades: 'Eletricista',
        criadoEm: '2026-10-01T10:00:00',
        mediaNotas: null,
        quantidadeAvaliacoes: 0,
      }],
      pagina: 1,
      tamanho: 12,
      totalItens: 1,
      totalPaginas: 1,
    });
  });

  it('envia nota mínima como filtro opcional', () => {
    api.buscarProfissionais('', '', 0, 4.5).subscribe();
    const requisicao = http.expectOne((candidate) => candidate.url === '/api/profissionais');
    expect(requisicao.request.params.get('notaMinima')).toBe('4.5');
    requisicao.flush({ itens: [], pagina: 0, tamanho: 12, totalItens: 0, totalPaginas: 0 });
  });

  it('envia categoria, modalidade e faixa de preço da busca profissional', () => {
    api.buscarProfissionais('', '', 0, undefined, {
      categoria: 'MANUTENCAO', modalidade: 'VENDA', precoMinimo: 100, precoMaximo: 200,
    }).subscribe();
    const requisicao = http.expectOne((candidate) => candidate.url === '/api/profissionais');
    expect(requisicao.request.params.get('categoria')).toBe('MANUTENCAO');
    expect(requisicao.request.params.get('modalidade')).toBe('VENDA');
    expect(requisicao.request.params.get('precoMinimo')).toBe('100');
    expect(requisicao.request.params.get('precoMaximo')).toBe('200');
    requisicao.flush({ itens: [], pagina: 0, tamanho: 12, totalItens: 0, totalPaginas: 0 });
  });

  it('atualiza o próprio perfil pelo recurso autenticado', () => {
    const dados = {
      nome: 'Ana Lima', tituloProfissional: 'Professora de violão', bio: 'Aulas com prática', cidade: 'São José',
      especialidades: 'Violão', linkedinUrl: '', instagramUrl: '', siteUrl: 'https://ana-profissional.com.br',
    };
    api.atualizarMeuPerfil(dados).subscribe((perfil) => expect(perfil.nome).toBe('Ana Lima'));

    const request = http.expectOne('/api/perfil');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual(dados);
    request.flush({
      id: 12,
      nome: dados.nome,
      tituloProfissional: dados.tituloProfissional,
      bio: dados.bio,
      cidade: dados.cidade,
      fotoUrl: null,
      capaUrl: null,
      especialidades: dados.especialidades,
      criadoEm: '2026-10-01T10:00:00',
      mediaNotas: null,
      quantidadeAvaliacoes: 0,
    });
  });

  it('envia a foto do próprio perfil como multipart autenticado', () => {
    const arquivo = new File(['dados-da-imagem'], 'perfil.png', { type: 'image/png' });
    api.atualizarFoto(arquivo).subscribe((perfil) => expect(perfil.fotoUrl).toBe('/api/uploads/foto.png'));

    const request = http.expectOne('/api/perfil/foto');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body instanceof FormData).toBeTrue();
    expect((request.request.body as FormData).get('file')).toBe(arquivo);
    request.flush({
      id: 12,
      nome: 'Ana Profissional',
      tituloProfissional: null,
      bio: null,
      cidade: null,
      fotoUrl: '/api/uploads/foto.png',
      capaUrl: null,
      especialidades: null,
      criadoEm: '2026-10-01T10:00:00',
      mediaNotas: null,
      quantidadeAvaliacoes: 0,
    });
  });

  it('remove a foto pelo recurso do perfil', () => {
    api.removerFoto().subscribe();
    const request = http.expectOne('/api/perfil/foto');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });

  it('envia e remove a capa do próprio perfil', () => {
    const arquivo = new File(['imagem'], 'capa.png', { type: 'image/png' });
    api.atualizarCapa(arquivo).subscribe((perfil) => expect(perfil.capaUrl).toBe('/api/uploads/capa.png'));

    const atualizar = http.expectOne('/api/perfil/capa');
    expect(atualizar.request.method).toBe('PUT');
    expect((atualizar.request.body as FormData).get('file')).toBe(arquivo);
    atualizar.flush({
      id: 12, nome: 'Ana', tituloProfissional: null, bio: null, cidade: null, fotoUrl: null,
      capaUrl: '/api/uploads/capa.png', especialidades: null, criadoEm: '2026-10-01T10:00:00',
      mediaNotas: null, quantidadeAvaliacoes: 0,
    });

    api.removerCapa().subscribe();
    const remover = http.expectOne('/api/perfil/capa');
    expect(remover.request.method).toBe('DELETE');
    remover.flush(null);
  });

  it('envia e remove publicações de portfólio pelos recursos REST', () => {
    const fotos = ['/api/uploads/' + 'a'.repeat(64) + '.png'];
    api.criarPublicacao('Trabalho concluído', fotos).subscribe((post) => {
      expect(post.fotos).toEqual(fotos);
    });
    const criar = http.expectOne('/api/publicacoes');
    expect(criar.request.method).toBe('POST');
    expect(criar.request.body).toEqual({ legenda: 'Trabalho concluído', fotos });
    criar.flush({ id: 9, profissionalId: 12, profissionalNome: 'Ana', anuncioId: null, anuncioTitulo: null,
      legenda: 'Trabalho concluído', fotos, criadoEm: '2026-10-02T10:00:00', totalCurtidas: 0, curtidaPeloUsuario: false });

    api.excluirPublicacao(9).subscribe();
    const remover = http.expectOne('/api/publicacoes/9');
    expect(remover.request.method).toBe('DELETE');
    remover.flush(null);

    api.atualizarLegendaPublicacao(9, 'Projeto revisado').subscribe((post) => {
      expect(post.legenda).toBe('Projeto revisado');
    });
    const atualizar = http.expectOne('/api/publicacoes/9');
    expect(atualizar.request.method).toBe('PATCH');
    expect(atualizar.request.body).toEqual({ legenda: 'Projeto revisado' });
    atualizar.flush({ id: 9, legenda: 'Projeto revisado', fotos, criadoEm: '2026-10-02T10:00:00' });
  });

  it('curte e remove curtida por recursos REST idempotentes da publicação', () => {
    api.curtirPublicacao(9).subscribe((resumo) => {
      expect(resumo).toEqual({ publicacaoId: 9, totalCurtidas: 1, curtidaPeloUsuario: true });
    });
    const curtir = http.expectOne('/api/publicacoes/9/curtida');
    expect(curtir.request.method).toBe('PUT');
    curtir.flush({ publicacaoId: 9, totalCurtidas: 1, curtidaPeloUsuario: true });

    api.deixarDeCurtirPublicacao(9).subscribe((resumo) => {
      expect(resumo.totalCurtidas).toBe(0);
      expect(resumo.curtidaPeloUsuario).toBeFalse();
    });
    const remover = http.expectOne('/api/publicacoes/9/curtida');
    expect(remover.request.method).toBe('DELETE');
    remover.flush({ publicacaoId: 9, totalCurtidas: 0, curtidaPeloUsuario: false });
  });

  it('consulta comentários paginados e envia resposta sem criar uma conversa paralela', () => {
    api.comentariosPublicacao(9, 1).subscribe((pagina) => expect(pagina.pagina).toBe(1));
    const leitura = http.expectOne('/api/publicacoes/9/comentarios?pagina=1&tamanho=10');
    expect(leitura.request.method).toBe('GET');
    leitura.flush({ itens: [], pagina: 1, tamanho: 10, totalItens: 12, totalPaginas: 2 });

    api.comentarPublicacao(9, 'Obrigado', 5).subscribe((comentario) => {
      expect(comentario.respostaAId).toBe(5);
    });
    const envio = http.expectOne('/api/publicacoes/9/comentarios');
    expect(envio.request.method).toBe('POST');
    expect(envio.request.body).toEqual({ conteudo: 'Obrigado', respostaAId: 5 });
    envio.flush({ id: 10, publicacaoId: 9, respostaAId: 5, autorNome: 'Ana', conteudo: 'Obrigado',
      criadoEm: '2026-10-30T10:00:00', respostas: [], podeResponder: false, podeDenunciar: false });
  });
});
