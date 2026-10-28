import { TestBed } from '@angular/core/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { appConfig } from '../../../../app.config';

describe('Catálogo integrado ao backend', () => {
  let http: HttpTestingController;
  const resposta = { itens: [], pagina: 0, tamanho: 5, totalItens: 0, totalPaginas: 0 };
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [...appConfig.providers, provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('aguarda a API e exibe estado vazio sem ofertas locais', async () => {
    const harness = await RouterTestingHarness.create('/anuncios');
    expect(harness.routeNativeElement?.textContent).toContain('Carregando anúncios');
    expect(harness.routeNativeElement?.querySelectorAll('app-anuncio-card').length).toBe(0);
    http.expectOne('/api/vitrine').flush({ categorias: [], destaques: [], servicos: [] });
    http.expectOne((r) => r.url === '/api/anuncios').flush(resposta);
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Nenhum resultado encontrado');
  });

  it('envia filtros por URL e cancela uma busca antiga ainda pendente', async () => {
    const harness = await RouterTestingHarness.create('/anuncios?busca=violao&categoria=aulas&cidade=Florianopolis&modalidade=VENDA&precoMinimo=50&precoMaximo=200&diaSemana=SABADO&periodo=TARDE');
    http.expectOne('/api/vitrine').flush({ categorias: [], destaques: [], servicos: [] });
    const antiga = http.expectOne((r) => r.url === '/api/anuncios');
    expect(antiga.request.params.get('busca')).toBe('violao');
    expect(antiga.request.params.get('categoria')).toBe('aulas');
    expect(antiga.request.params.get('cidade')).toBe('Florianopolis');
    expect(antiga.request.params.get('modalidade')).toBe('VENDA');
    expect(antiga.request.params.get('precoMinimo')).toBe('50');
    expect(antiga.request.params.get('precoMaximo')).toBe('200');
    expect(antiga.request.params.get('diaSemana')).toBe('SABADO');
    expect(antiga.request.params.get('periodo')).toBe('TARDE');
    await harness.navigateByUrl('/anuncios?busca=pintura&pagina=1');
    expect(antiga.cancelled).toBeTrue();
    const atual = http.expectOne((r) => r.url === '/api/anuncios');
    expect(atual.request.params.get('pagina')).toBe('1');
    atual.flush({ ...resposta, pagina: 1 });
  });

  it('mostra falha e permite tentar novamente sem retornar exemplos fictícios', async () => {
    const harness = await RouterTestingHarness.create('/anuncios');
    http.expectOne('/api/vitrine').flush({ categorias: [], destaques: [], servicos: [] });
    http
      .expectOne((r) => r.url === '/api/anuncios')
      .flush({}, { status: 503, statusText: 'Unavailable' });
    harness.detectChanges();
    expect(harness.routeNativeElement?.querySelector('[role="alert"]')?.textContent).toContain(
      'carregar',
    );
    (
      harness.routeNativeElement?.querySelector('[data-testid="recarregar"]') as HTMLButtonElement
    ).click();
    http.expectOne('/api/vitrine').flush({ categorias: [], destaques: [], servicos: [] });
    http.expectOne((r) => r.url === '/api/anuncios').flush(resposta);
    harness.detectChanges();
    expect(harness.routeNativeElement?.querySelector('[role="alert"]')).toBeNull();
  });
  it('separa venda e troca a partir dos dados persistidos, sem inventar ofertas', async () => {
    const harness = await RouterTestingHarness.create('/anuncios');
    http.expectOne('/api/vitrine').flush({ categorias: [], destaques: [], servicos: [] });
    const req = http.expectOne(r => r.url === '/api/anuncios');
    expect(req.request.params.get('tamanho')).toBe('12');
    const base = { categoria: 'AULAS', categoriaRotulo: 'Aulas', descricao: 'Aula individual',
      criadoEm: '2026-10-28', autor: 'Ana', usuarioId: 7, exemplo: false, cidade: 'Ourinhos' };
    req.flush({ ...resposta, totalItens: 2, itens: [
      { ...base, id: 1, titulo: 'Piano', modalidade: 'VENDA', preco: 90 },
      { ...base, id: 2, titulo: 'Violão', modalidade: 'TROCA', preco: null },
    ] });
    harness.detectChanges();
    const secoes = harness.routeNativeElement!.querySelectorAll('.prateleira');
    expect(secoes.length).toBe(2);
    expect(secoes[0].textContent).toContain('Piano');
    expect(secoes[0].textContent).not.toContain('Violão');
    expect(secoes[1].textContent).toContain('Violão');
    expect(harness.routeNativeElement!.querySelector('.nota-demo')).toBeNull();
  });

});
