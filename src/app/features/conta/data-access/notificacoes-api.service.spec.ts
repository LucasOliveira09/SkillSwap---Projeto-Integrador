import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { appConfig } from '../../../app.config';
import { NotificacoesApiService } from './notificacoes-api.service';

describe('NotificacoesApiService', () => {
  let http: HttpTestingController;
  let api: NotificacoesApiService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [...appConfig.providers, provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    api = TestBed.inject(NotificacoesApiService);
  });

  afterEach(() => http.verify());

  it('lista notificações recentes da API', () => {
    api.listar().subscribe((pagina) => expect(pagina.itens).toEqual([]));
    const request = http.expectOne('/api/notificacoes?pagina=0&tamanho=10');
    expect(request.request.method).toBe('GET');
    request.flush({ itens: [], pagina: 0, tamanho: 10, totalItens: 0, totalPaginas: 0 });
  });

  it('marca uma notificação como lida usando PATCH', () => {
    api.atualizarLeitura(12, true).subscribe((notificacao) => expect(notificacao.lida).toBe(true));
    const request = http.expectOne('/api/notificacoes/12');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ lida: true });
    request.flush({
      id: 12, tipo: 'NOVA_MENSAGEM', titulo: 'Nova mensagem', mensagem: 'Resposta recebida',
      recursoUrl: '/acordos/7', lida: true, criadaEm: '2026-10-27T10:00:00',
    });
  });
});
