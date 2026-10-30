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

    api.eventos(3).subscribe((eventos) => expect(eventos).toEqual([]));
    const eventos = http.expectOne('/api/acordos/3/eventos');
    expect(eventos.request.method).toBe('GET');
    eventos.flush([]);

    api.mensagens(3, 4).subscribe((pagina) => expect(pagina.itens.length).toBe(0));
    const mensagens = http.expectOne((request) => request.url === '/api/acordos/3/mensagens');
    expect(mensagens.request.params.get('pagina')).toBe('4');
    expect(mensagens.request.params.get('tamanho')).toBe('50');
    mensagens.flush({ itens: [], pagina: 4, tamanho: 50, totalItens: 0, totalPaginas: 0 });

    api.atualizarAcordo(3, 'CONCLUIDO').subscribe();
    const atualizacao = http.expectOne('/api/acordos/3');
    expect(atualizacao.request.method).toBe('PATCH');
    expect(atualizacao.request.body).toEqual({ status: 'CONCLUIDO' });
    atualizacao.flush({ id: 3, status: 'CONCLUIDO' });

    api.responderAvaliacao(3, 8, 'Obrigado pelo retorno.').subscribe();
    const resposta = http.expectOne('/api/acordos/3/avaliacoes/8/resposta');
    expect(resposta.request.method).toBe('PATCH');
    expect(resposta.request.body).toEqual({ resposta: 'Obrigado pelo retorno.' });
    resposta.flush({ id: 8, respostaProfissional: 'Obrigado pelo retorno.' });
  });

  it('envia contestação privada e consulta o estado da avaliação', () => {
    const justificativa = 'O texto relata uma etapa que não fazia parte do serviço combinado.';
    api.contestarAvaliacao(8, justificativa).subscribe((contestacao) => {
      expect(contestacao.status).toBe('ABERTA');
    });
    const criar = http.expectOne('/api/avaliacoes/8/contestacoes');
    expect(criar.request.method).toBe('POST');
    expect(criar.request.body).toEqual({ justificativa });
    criar.flush({ id: 4, avaliacaoId: 8, status: 'ABERTA' });

    api.buscarContestacao(8).subscribe((contestacoes) => expect(contestacoes.length).toBe(1));
    const buscar = http.expectOne('/api/avaliacoes/8/contestacoes');
    expect(buscar.request.method).toBe('GET');
    buscar.flush([{ id: 4, avaliacaoId: 8, status: 'ABERTA' }]);
  });

  it('substitui a avaliação usando PUT no recurso do acordo', () => {
    api.editarAvaliacao(3, 8, 4, 5, 'Texto revisado').subscribe((avaliacao) => {
      expect(avaliacao.edicaoRealizada).toBeTrue();
    });
    const request = http.expectOne('/api/acordos/3/avaliacoes/8');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ nota: 4, notaAnuncio: 5, depoimento: 'Texto revisado' });
    request.flush({ id: 8, edicaoRealizada: true });
  });
});
