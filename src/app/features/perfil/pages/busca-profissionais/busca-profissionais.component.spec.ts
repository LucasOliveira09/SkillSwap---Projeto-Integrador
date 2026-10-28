import { TestBed } from '@angular/core/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { appConfig } from '../../../../app.config';
import { BuscaProfissionaisComponent } from './busca-profissionais.component';

describe('BuscaProfissionaisComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [...appConfig.providers, provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('busca por especialidade e cidade e mostra somente dados públicos', async () => {
    const harness = await RouterTestingHarness.create(
      '/profissionais?busca=Eletricista&cidade=Florian%C3%B3polis',
    );
    http.expectOne('/api/categorias').flush([{ valor: 'MANUTENCAO', rotulo: 'Manutenção e reparos' }]);
    const requisicao = http.expectOne((request) => request.url === '/api/profissionais');
    expect(requisicao.request.params.get('busca')).toBe('Eletricista');
    expect(requisicao.request.params.get('cidade')).toBe('Florianópolis');
    expect(requisicao.request.params.get('pagina')).toBe('0');
    expect(requisicao.request.params.get('tamanho')).toBe('12');
    requisicao.flush({
      itens: [
        {
          id: 12,
          nome: 'Ana Profissional',
          tituloProfissional: 'Eletricista residencial',
          bio: 'Atendo residências.',
          cidade: 'Florianópolis',
          fotoUrl: null,
          capaUrl: null,
          especialidades: 'Eletricista, instalações',
          criadoEm: '2026-10-01T10:00:00',
          mediaNotas: 4.6,
          quantidadeAvaliacoes: 7,
        },
      ],
      pagina: 0,
      tamanho: 12,
      totalItens: 1,
      totalPaginas: 1,
    });
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent ?? '';
    expect(texto).toContain('Ana Profissional');
    expect(texto).toContain('Eletricista residencial');
    expect(texto).toContain('Eletricista, instalações');
    expect(texto).toContain('Florianópolis');
    expect(texto).toContain('4,6');
    expect(texto).toContain('7 avaliações');
    const reputacao = harness.routeNativeElement?.querySelector('.reputacao');
    expect(reputacao?.hasAttribute('aria-label')).toBeFalse();
    expect(reputacao?.textContent).toContain('7');
    expect(texto).not.toContain('@');
    expect(harness.routeNativeElement?.querySelector('a[href="/profissionais/12"]')).not.toBeNull();
  });

  it('mostra estado vazio quando a API não encontra perfis', async () => {
    const harness = await RouterTestingHarness.create('/profissionais');
    http.expectOne('/api/categorias').flush([]);
    http.expectOne((request) => request.url === '/api/profissionais').flush({
      itens: [],
      pagina: 0,
      tamanho: 12,
      totalItens: 0,
      totalPaginas: 0,
    });
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Nenhum perfil encontrado');
  });

  it('mantém o host da página como bloco que pode encolher em telas estreitas', async () => {
    const harness = await RouterTestingHarness.create('/profissionais');
    http.expectOne('/api/categorias').flush([]);
    http.expectOne((request) => request.url === '/api/profissionais').flush({
      itens: [],
      pagina: 0,
      tamanho: 12,
      totalItens: 0,
      totalPaginas: 0,
    });
    harness.detectChanges();

    const host = harness.routeNativeElement;
    expect(host).not.toBeNull();
    expect(getComputedStyle(host!).display).toBe('block');
    expect(getComputedStyle(host!).minWidth).toBe('0px');
  });

  it('envia filtro de reputação pela URL e explica a amostra mínima', async () => {
    const harness = await RouterTestingHarness.create('/profissionais?notaMinima=4.5');
    http.expectOne('/api/categorias').flush([{ valor: 'MANUTENCAO', rotulo: 'Manutenção e reparos' }]);
    const requisicao = http.expectOne((request) => request.url === '/api/profissionais');
    expect(requisicao.request.params.get('notaMinima')).toBe('4.5');
    requisicao.flush({ itens: [], pagina: 0, tamanho: 12, totalItens: 0, totalPaginas: 0 });
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('pelo menos 3 avaliações');
    expect(harness.routeNativeElement?.textContent).toContain('correspondência com o serviço');
    expect(harness.routeNativeElement?.textContent).toContain('avaliações não definem a posição');
    expect(harness.routeNativeElement?.querySelector<HTMLSelectElement>('[name="notaMinima"]')?.value).toBe('4.5');
  });

  it('envia filtros da oferta e apresenta somente o resumo real retornado pela API', async () => {
    const harness = await RouterTestingHarness.create(
      '/profissionais?categoria=MANUTENCAO&modalidade=VENDA&precoMinimo=100&precoMaximo=200',
    );
    http.expectOne('/api/categorias').flush([{ valor: 'MANUTENCAO', rotulo: 'Manutenção e reparos' }]);
    const request = http.expectOne((candidate) => candidate.url === '/api/profissionais');
    expect(request.request.params.get('categoria')).toBe('MANUTENCAO');
    expect(request.request.params.get('modalidade')).toBe('VENDA');
    expect(request.request.params.get('precoMinimo')).toBe('100');
    expect(request.request.params.get('precoMaximo')).toBe('200');
    request.flush({
      itens: [{
        id: 19, nome: 'Ana Profissional', tituloProfissional: null, bio: null, cidade: 'Campinas',
        fotoUrl: null, capaUrl: null, especialidades: 'Manutenção', criadoEm: '2026-10-01T10:00:00',
        mediaNotas: null, quantidadeAvaliacoes: 0,
        resumoOfertas: [{ profissionalId: 19, categoria: 'MANUTENCAO', modalidade: 'VENDA', quantidade: 2,
          precoMinimo: 150, precoMaximo: 190 }],
      }],
      pagina: 0, tamanho: 12, totalItens: 1, totalPaginas: 1,
    });
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('Manutenção e reparos');
    expect(harness.routeNativeElement?.textContent).toContain('2 ofertas');
    expect(harness.routeNativeElement?.textContent).toContain('R$ 150,00 a R$ 190,00');
  });
});
