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
    expect(falhou).toBeTrue();
    expect(sessao.autenticado()).toBeFalse();
  });
});
