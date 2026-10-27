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
});
