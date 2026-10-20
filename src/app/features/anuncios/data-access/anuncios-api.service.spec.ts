import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AnunciosApiService } from './anuncios-api.service';

describe('AnunciosApiService', () => {
  let http: HttpTestingController;
  let api: AnunciosApiService;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    api = TestBed.inject(AnunciosApiService);
  });
  afterEach(() => http.verify());

  it('envia busca, categoria e página para o backend', () => {
    api.listar({ busca: 'violão', categoria: 'aulas', pagina: 2 }).subscribe();
    const req = http.expectOne(r => r.url === '/api/anuncios');
    expect(req.request.params.get('busca')).toBe('violão');
    expect(req.request.params.get('categoria')).toBe('aulas');
    expect(req.request.params.get('pagina')).toBe('2');
    req.flush({ itens: [], pagina: 2, tamanho: 5, totalItens: 0, totalPaginas: 0 });
  });

  it('aguarda a gravação e propaga a falha sem simular sucesso local', () => {
    let concluido = false;
    let falhou = false;
    api.criar({ titulo: 'Aula', descricao: 'Violão', categoria: 'aulas', cidade: 'Ourinhos',
      modalidade: 'venda', preco: 90, habilidadeDesejada: '' }).subscribe({
      next: () => concluido = true, error: () => falhou = true,
    });
    expect(concluido).toBeFalse();
    const req = http.expectOne('/api/anuncios');
    expect(req.request.method).toBe('POST');
    req.flush({ mensagem: 'Indisponível' }, { status: 503, statusText: 'Unavailable' });
    expect(falhou).toBeTrue();
    expect(concluido).toBeFalse();
  });
});
