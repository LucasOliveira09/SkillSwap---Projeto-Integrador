import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';
import { App } from './app';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

describe('Navegação do SkillSwap', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    }),
  );
  afterEach(() => TestBed.inject(HttpTestingController).verify());
  function carregarCatalogo() {
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/vitrine').flush({ categorias: [], destaques: [], servicos: [] });
    http
      .expectOne((r) => r.url === '/api/anuncios')
      .flush({ itens: [], pagina: 0, tamanho: 5, totalItens: 0, totalPaginas: 0 });
  }

  it('abre o catálogo e exige login antes do cadastro de anúncio', async () => {
    const harness = await RouterTestingHarness.create('/');
    expect(TestBed.inject(Router).url).toBe('/anuncios');
    carregarCatalogo();
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Nenhum resultado encontrado');
    await harness.navigateByUrl('/anuncios/novo');
    expect(harness.routeNativeElement?.querySelector('form')).not.toBeNull();
    const url = TestBed.inject(Router).parseUrl(TestBed.inject(Router).url);
    expect(url.root.children['primary'].segments[0].path).toBe('conta');
    expect(url.queryParams['retorno']).toBe('/anuncios/novo');
    TestBed.inject(HttpTestingController).expectNone('/api/categorias');

  });

  it('retorna ao catálogo para um endereço desconhecido', async () => {
    await RouterTestingHarness.create('/endereco-inexistente');
    carregarCatalogo();
    expect(TestBed.inject(Router).url).toBe('/anuncios');
  });

  it('mantém um único landmark principal ao carregar a busca de profissionais', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await TestBed.inject(Router).navigateByUrl('/profissionais');
    fixture.detectChanges();
    TestBed.inject(HttpTestingController).expectOne('/api/categorias').flush([]);
    TestBed.inject(HttpTestingController)
      .expectOne((request) => request.url === '/api/profissionais')
      .flush({ itens: [], pagina: 0, tamanho: 12, totalItens: 0, totalPaginas: 0 });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('main').length).toBe(1);
  });
});
