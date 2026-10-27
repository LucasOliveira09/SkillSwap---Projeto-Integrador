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
      bio: null,
      cidade: 'Florianópolis',
      fotoUrl: null,
      especialidades: 'Aulas de violão',
      criadoEm: '2026-10-01T10:00:00',
      mediaNotas: null,
      quantidadeAvaliacoes: 0,
    });
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
        bio: null,
        cidade: 'Florianópolis',
        fotoUrl: null,
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

  it('atualiza o próprio perfil pelo recurso autenticado', () => {
    const dados = { nome: 'Ana Lima', bio: 'Aulas com prática', cidade: 'São José', especialidades: 'Violão' };
    api.atualizarMeuPerfil(dados).subscribe((perfil) => expect(perfil.nome).toBe('Ana Lima'));

    const request = http.expectOne('/api/perfil');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual(dados);
    request.flush({
      id: 12,
      nome: dados.nome,
      bio: dados.bio,
      cidade: dados.cidade,
      fotoUrl: null,
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
      bio: null,
      cidade: null,
      fotoUrl: '/api/uploads/foto.png',
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

  it('envia e remove publicações de portfólio pelos recursos REST', () => {
    const fotos = ['/api/uploads/' + 'a'.repeat(64) + '.png'];
    api.criarPublicacao('Trabalho concluído', fotos).subscribe((post) => {
      expect(post.fotos).toEqual(fotos);
    });
    const criar = http.expectOne('/api/publicacoes');
    expect(criar.request.method).toBe('POST');
    expect(criar.request.body).toEqual({ legenda: 'Trabalho concluído', fotos });
    criar.flush({ id: 9, profissionalId: 12, profissionalNome: 'Ana', anuncioId: null, anuncioTitulo: null,
      legenda: 'Trabalho concluído', fotos, criadoEm: '2026-10-02T10:00:00' });

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
});
