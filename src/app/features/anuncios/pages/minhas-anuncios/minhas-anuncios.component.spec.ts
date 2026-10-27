import { TestBed } from '@angular/core/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { appConfig } from '../../../../app.config';
import { SessaoService } from '../../../conta/data-access/sessao.service';

describe('Meus anúncios', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [...appConfig.providers, provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista as ofertas da conta e pausa uma oferta persistida', async () => {
    TestBed.inject(SessaoService).entrar('ana@teste.invalid', 'senha').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'token-de-teste' });
    const harness = await RouterTestingHarness.create('/anuncios/minhas');
    const item = {
      id: 17, titulo: 'Aula de violão', descricao: 'Para iniciantes', categoria: 'aulas',
      categoriaRotulo: 'Aulas e instrução', cidade: 'Florianópolis', modalidade: 'venda',
      preco: 90, unidade: '/hora', habilidadeDesejada: '', status: 'ATIVO', usuarioId: 12,
      criadoEm: '2026-10-20T12:00:00', imagem: null, autor: 'Ana', avatar: null,
      estrelas: null, exemplo: false, mediaAvaliacoesAnuncio: null, quantidadeAvaliacoesAnuncio: 0,
    };
    http.expectOne('/api/anuncios/meus?pagina=0&tamanho=20').flush({
      itens: [item], pagina: 0, tamanho: 20, totalItens: 1, totalPaginas: 1,
    });
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Aula de violão');
    expect(harness.routeNativeElement?.textContent).toContain('Publicado');

    (harness.routeNativeElement?.querySelector('button:not([disabled])') as HTMLButtonElement).click();
    const atualizar = http.expectOne('/api/anuncios/17');
    expect(atualizar.request.method).toBe('PATCH');
    expect(atualizar.request.body).toEqual({ status: 'INATIVO' });
    atualizar.flush({ ...item, status: 'INATIVO' });
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Pausado');
    expect(harness.routeNativeElement?.textContent).toContain('Anúncio pausado.');
  });
});
