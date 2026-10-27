import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NegociacaoApiService } from './negociacao-api.service';

describe('NegociacaoApiService', () => {
  let api: NegociacaoApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(NegociacaoApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('consulta listas paginadas e altera proposta por PATCH de recurso', () => {
    api.propostas('RECEBIDAS').subscribe((pagina) => expect(pagina.totalItens).toBe(1));
    const listagem = http.expectOne((request) => request.url === '/api/propostas');
    expect(listagem.request.params.get('direcao')).toBe('RECEBIDAS');
    expect(listagem.request.params.get('tamanho')).toBe('20');
    listagem.flush({ itens: [{}], pagina: 0, tamanho: 20, totalItens: 1, totalPaginas: 1 });

    api.atualizarProposta(9, 'ACEITA').subscribe();
    const atualizacao = http.expectOne('/api/propostas/9');
    expect(atualizacao.request.method).toBe('PATCH');
    expect(atualizacao.request.body).toEqual({ status: 'ACEITA' });
    atualizacao.flush({ id: 9 });

    api.atualizarProposta(10, 'CANCELADA').subscribe();
    const cancelamento = http.expectOne('/api/propostas/10');
    expect(cancelamento.request.method).toBe('PATCH');
    expect(cancelamento.request.body).toEqual({ status: 'CANCELADA' });
    cancelamento.flush({ id: 10, status: 'CANCELADA' });
  });

  it('carrega acordo, conversa e avaliações por seus recursos REST', () => {
    api.acordo(3).subscribe();
    const acordo = http.expectOne('/api/acordos/3');
    expect(acordo.request.method).toBe('GET');
    acordo.flush({ id: 3 });

    api.mensagens(3).subscribe((pagina) => expect(pagina.itens.length).toBe(0));
    const mensagens = http.expectOne((request) => request.url === '/api/acordos/3/mensagens');
    expect(mensagens.request.params.get('tamanho')).toBe('50');
    mensagens.flush({ itens: [], pagina: 0, tamanho: 50, totalItens: 0, totalPaginas: 0 });

    api.atualizarAcordo(3, 'CONCLUIDO').subscribe();
    const atualizacao = http.expectOne('/api/acordos/3');
    expect(atualizacao.request.method).toBe('PATCH');
    expect(atualizacao.request.body).toEqual({ status: 'CONCLUIDO' });
    atualizacao.flush({ id: 3, status: 'CONCLUIDO' });
  });
});
