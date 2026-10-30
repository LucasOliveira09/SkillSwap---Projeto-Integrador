import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AnunciosApiService } from './anuncios-api.service';

describe('AnunciosApiService', () => {
  let http: HttpTestingController;
  let api: AnunciosApiService;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    api = TestBed.inject(AnunciosApiService);
  });
  afterEach(() => http.verify());

  it('envia busca, categoria e página para o backend', () => {
    api.listar({ busca: 'violão', categoria: 'aulas', cidade: 'Florianópolis', pagina: 2, modalidade: 'VENDA', precoMinimo: '50', precoMaximo: '200', diaSemana: 'SABADO', periodo: 'TARDE' }).subscribe();
    const req = http.expectOne((r) => r.url === '/api/anuncios');
    expect(req.request.params.get('busca')).toBe('violão');
    expect(req.request.params.get('categoria')).toBe('aulas');
    expect(req.request.params.get('cidade')).toBe('Florianópolis');
    expect(req.request.params.get('pagina')).toBe('2');
    expect(req.request.params.get('modalidade')).toBe('VENDA');
    expect(req.request.params.get('precoMinimo')).toBe('50');
    expect(req.request.params.get('precoMaximo')).toBe('200');
    expect(req.request.params.get('diaSemana')).toBe('SABADO');
    expect(req.request.params.get('periodo')).toBe('TARDE');
    req.flush({ itens: [], pagina: 2, tamanho: 5, totalItens: 0, totalPaginas: 0 });
  });

  it('aguarda a gravação e propaga a falha sem simular sucesso local', () => {
    let concluido = false;
    let falhou = false;
    api
      .criar({
        titulo: 'Aula',
        descricao: 'Violão',
        categoria: 'aulas',
        cidade: 'Ourinhos',
        modalidade: 'venda',
        preco: 90,
        habilidadeDesejada: '',
        disponibilidades: [{ diaSemana: 'SEGUNDA', periodo: 'MANHA' }],
      })
      .subscribe({
        next: () => (concluido = true),
        error: () => (falhou = true),
      });
    expect(concluido).toBeFalse();
    const req = http.expectOne('/api/anuncios');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.disponibilidades).toEqual([{ diaSemana: 'SEGUNDA', periodo: 'MANHA' }]);
    req.flush({ mensagem: 'Indisponível' }, { status: 503, statusText: 'Unavailable' });
    expect(falhou).toBeTrue();
    expect(concluido).toBeFalse();
  });

  it('lista e consulta somente anúncios gerenciáveis pela conta autenticada', () => {
    api.listarMeus(1).subscribe();
    const lista = http.expectOne('/api/anuncios/meus?pagina=1&tamanho=20');
    expect(lista.request.method).toBe('GET');
    lista.flush({ itens: [], pagina: 1, tamanho: 20, totalItens: 0, totalPaginas: 0 });

    api.obterMeu(42).subscribe();
    const detalhe = http.expectOne('/api/anuncios/meus/42');
    expect(detalhe.request.method).toBe('GET');
    detalhe.flush({ id: 42, categoria: 'AULAS', modalidade: 'VENDA' });
  });

  it('edita o anúncio e altera apenas o status pela API REST', () => {
    const dados = {
      titulo: 'Aula de piano', descricao: 'Iniciante', categoria: 'aulas', cidade: 'Florianópolis',
      modalidade: 'venda' as const, preco: 75, habilidadeDesejada: '',
    };
    api.atualizar(42, dados).subscribe();
    const edicao = http.expectOne('/api/anuncios/42');
    expect(edicao.request.method).toBe('PUT');
    expect(edicao.request.body).toEqual(jasmine.objectContaining({ categoria: 'AULAS', modalidade: 'VENDA', preco: 75 }));
    edicao.flush({ id: 42, categoria: 'AULAS', modalidade: 'VENDA' });

    api.atualizarStatus(42, 'INATIVO').subscribe();
    const status = http.expectOne('/api/anuncios/42');
    expect(status.request.method).toBe('PATCH');
    expect(status.request.body).toEqual({ status: 'INATIVO' });
    status.flush({ id: 42, categoria: 'AULAS', modalidade: 'VENDA', status: 'INATIVO' });
  });
});
