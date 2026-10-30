import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SessaoService } from './sessao.service';
import { sessaoInterceptor } from './sessao.interceptor';

describe('Sessão JWT', () => {
  let http: HttpTestingController;
  let sessao: SessaoService;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([sessaoInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    sessao = TestBed.inject(SessaoService);
  });
  afterEach(() => http.verify());

  function entrar() {
    sessao.entrar('teste@skillswap.invalid', 'senha-teste').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'jwt-de-teste' });
  }

  it('só inicia sessão após resposta válida do servidor', () => {
    sessao.entrar('teste@skillswap.invalid', 'senha-errada').subscribe({ error: () => {} });
    expect(sessao.autenticado()).toBeFalse();
    http.expectOne('/api/sessoes').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(sessao.autenticado()).toBeFalse();
    entrar();
    expect(sessao.autenticado()).toBeTrue();
    sessao.sair();
    http.expectOne('/api/sessoes/atual').flush(null, { status: 204, statusText: 'No Content' });
    expect(sessao.credencial()).toBeNull();
  });

  it('envia o JWT apenas à API da própria origem', () => {
    entrar();
    const cliente = TestBed.inject(HttpClient);
    cliente.get('/api/anuncios').subscribe();
    const interna = http.expectOne('/api/anuncios');
    expect(interna.request.headers.get('Authorization')).toBe('Bearer jwt-de-teste');
    interna.flush({});
    cliente.get('https://exemplo.invalid/api/anuncios').subscribe();
    const externa = http.expectOne('https://exemplo.invalid/api/anuncios');
    expect(externa.request.headers.has('Authorization')).toBeFalse();
    externa.flush({});
  });

  it('encerra sessão expirada e propaga a falha para a tela', () => {
    entrar();
    let falhou = false;
    TestBed.inject(HttpClient)
      .get('/api/anuncios')
      .subscribe({ error: () => (falhou = true) });
    http.expectOne('/api/anuncios').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/sessoes/atual').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(falhou).toBeTrue();
    expect(sessao.autenticado()).toBeFalse();
  });
  it('restaura o JWT pelo cookie, sem expor a credencial persistente ao JavaScript', () => {
    sessao.restaurar().subscribe();
    const req = http.expectOne('/api/sessoes/atual');
    expect(req.request.headers.get('X-SkillSwap-Session')).toBe('1');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({ token: 'restaurado' });
    expect(sessao.credencial()).toBe('restaurado');
  });

  it('compartilha a renovação entre falhas simultâneas e repete as chamadas uma vez', () => {
    entrar();
    const cliente = TestBed.inject(HttpClient);
    cliente.get('/api/perfil').subscribe();
    cliente.get('/api/acordos').subscribe();
    http.expectOne('/api/perfil').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/acordos').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/sessoes/atual').flush({ token: 'renovado' });
    for (const url of ['/api/perfil', '/api/acordos']) {
      const req = http.expectOne(url);
      expect(req.request.headers.get('Authorization')).toBe('Bearer renovado');
      req.flush({});
    }
  });

  it('não finge logout quando a revogação falha', () => {
    entrar();
    sessao.sair();
    http.expectOne('/api/sessoes/atual').flush({}, { status: 503, statusText: 'Unavailable' });
    expect(sessao.autenticado()).toBeTrue();
    expect(sessao.erroSaida()).toContain('Não foi possível');
  });

});
