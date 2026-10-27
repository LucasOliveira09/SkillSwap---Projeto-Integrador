import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ModeracaoApiService } from './moderacao-api.service';

describe('ModeracaoApiService', () => {
  let api: ModeracaoApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(ModeracaoApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envia denúncia como recurso e valida duplicidade pelo status HTTP', () => {
    api.denunciar('PUBLICACAO', 42, 'SPAM', 'Link repetido').subscribe((denuncia) => {
      expect(denuncia.status).toBe('ABERTA');
    });
    const request = http.expectOne('/api/denuncias');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      tipoConteudo: 'PUBLICACAO', recursoId: 42, motivo: 'SPAM', descricao: 'Link repetido',
    });
    request.flush({ id: 9, tipoConteudo: 'PUBLICACAO', recursoId: 42, motivo: 'SPAM',
      descricao: 'Link repetido', status: 'ABERTA', criadaEm: '2026-10-27T10:00:00',
      analisadaEm: null, autorId: 7, autorNome: 'Cliente', trechoConteudo: 'Anúncio repetido' });
  });

  it('consulta e decide denúncias usando paginação e PATCH parcial', () => {
    api.listar('ABERTA', 2).subscribe((pagina) => expect(pagina.totalItens).toBe(1));
    const listar = http.expectOne('/api/moderacao/denuncias?status=ABERTA&pagina=2&tamanho=20');
    expect(listar.request.method).toBe('GET');
    listar.flush({ itens: [], pagina: 2, tamanho: 20, totalItens: 1, totalPaginas: 1 });

    api.decidir(9, 'OCULTAR').subscribe();
    const decidir = http.expectOne('/api/moderacao/denuncias/9');
    expect(decidir.request.method).toBe('PATCH');
    expect(decidir.request.body).toEqual({ acao: 'OCULTAR' });
    decidir.flush({ id: 9, status: 'OCULTADA' });
  });

  it('lista contestações paginadas e envia decisão fundamentada', () => {
    api.listarContestacoes('ABERTA', 1).subscribe((pagina) => expect(pagina.totalItens).toBe(1));
    const listar = http.expectOne('/api/moderacao/contestacoes?status=ABERTA&pagina=1&tamanho=20');
    expect(listar.request.method).toBe('GET');
    listar.flush({ itens: [], pagina: 1, tamanho: 20, totalItens: 1, totalPaginas: 1 });

    const decisao = 'A avaliação descreve cobrança não acordada e foi ocultada.';
    api.decidirContestacao(5, 'OCULTAR', decisao).subscribe((contestacao) => {
      expect(contestacao.status).toBe('OCULTADA');
    });
    const atualizar = http.expectOne('/api/moderacao/contestacoes/5');
    expect(atualizar.request.method).toBe('PATCH');
    expect(atualizar.request.body).toEqual({ acao: 'OCULTAR', decisao });
    atualizar.flush({ id: 5, status: 'OCULTADA' });
  });
});
