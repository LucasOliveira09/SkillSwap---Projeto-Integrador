import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot } from '@angular/router';
import { sessaoGuard } from './sessao.guard';
import { SessaoService } from './sessao.service';

describe('acesso ao cadastro de anúncio', () => {
  const autenticado = signal(false);
  beforeEach(() => {
    autenticado.set(false);
    TestBed.configureTestingModule({ providers: [provideRouter([]),
      { provide: SessaoService, useValue: { autenticado } }] });
  });

  it('direciona o visitante ao login preservando o destino', () => {
    const resultado = TestBed.runInInjectionContext(() => sessaoGuard(
      {} as ActivatedRouteSnapshot, { url: '/anuncios/novo' } as RouterStateSnapshot));
    expect(resultado?.toString()).toBe(TestBed.inject(Router).createUrlTree(['/conta'], {
      queryParams: { retorno: '/anuncios/novo' },
    }).toString());
  });

  it('permite que uma sessão autenticada abra o cadastro', () => {
    autenticado.set(true);
    expect(TestBed.runInInjectionContext(() => sessaoGuard(
      {} as ActivatedRouteSnapshot, { url: '/anuncios/novo' } as RouterStateSnapshot))).toBeTrue();
  });
});
