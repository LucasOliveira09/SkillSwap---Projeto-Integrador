import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { SessaoService } from './features/conta/data-access/sessao.service';

describe('App', () => {
  const autenticado = signal(false);

  beforeEach(async () => {
    autenticado.set(false);
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        { provide: SessaoService, useValue: { autenticado, sair: () => undefined } },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('oferece navegação identificada entre catálogo, profissionais e cadastro', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const links = compiled.querySelectorAll('nav a');
    expect(links.length).toBe(3);
    expect(links[0].getAttribute('href')).toBe('/anuncios');
    expect(links[1].getAttribute('href')).toBe('/profissionais');
    expect(links[2].getAttribute('href')).toBe('/anuncios/novo');
  });

  it('mantém nome acessível para o botão de conta quando o texto visual está oculto', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const botaoConta = compiled.querySelector('header .conta');

    expect(botaoConta?.getAttribute('aria-label')).toBe('Entrar ou registrar-se');

    autenticado.set(true);
    fixture.detectChanges();

    expect(botaoConta?.getAttribute('aria-label')).toBe('Minha conta');
  });
});
