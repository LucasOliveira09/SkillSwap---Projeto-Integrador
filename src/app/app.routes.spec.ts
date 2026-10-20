import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';

describe('Navegação do SkillSwap', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideRouter(routes)] }));

  it('abre o catálogo a partir da raiz e permite navegar até o cadastro', async () => {
    const harness = await RouterTestingHarness.create('/');
    expect(TestBed.inject(Router).url).toBe('/anuncios');
    expect(harness.routeNativeElement?.textContent).toContain('Nenhum anúncio nesta sessão');
    await harness.navigateByUrl('/anuncios/novo');
    expect(harness.routeNativeElement?.querySelector('form')).not.toBeNull();
    expect(harness.routeNativeElement?.textContent).toContain('Cadastro demonstrativo');
  });

  it('retorna ao catálogo para um endereço desconhecido', async () => {
    await RouterTestingHarness.create('/endereco-inexistente');
    expect(TestBed.inject(Router).url).toBe('/anuncios');
  });
});
