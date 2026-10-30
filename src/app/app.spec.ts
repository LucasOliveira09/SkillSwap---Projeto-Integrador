import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { SessaoService } from './features/conta/data-access/sessao.service';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

describe('App', () => {
  const autenticado = signal(false);
  const credencial = signal<string | null>(null);

  beforeEach(async () => {
    autenticado.set(false);
    credencial.set(null);
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SessaoService, useValue: { autenticado, credencial: () => credencial(), sair: () => undefined } },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('oferece navegação identificada entre catálogo, profissionais, mensagens e cadastro', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const links = compiled.querySelectorAll('nav a');
    expect(links.length).toBe(4);
    expect(links[0].getAttribute('href')).toBe('/anuncios');
    expect(links[1].getAttribute('href')).toBe('/profissionais');
    expect(links[2].getAttribute('href')).toBe('/mensagens');
    expect(links[3].getAttribute('href')).toBe('/anuncios/novo');
  });

  it('ordena o campo de busca antes do botão de envio', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const formulario = fixture.nativeElement.querySelector('form.busca') as HTMLFormElement;
    const campo = formulario.querySelector('input[type="search"]')!;
    const botao = formulario.querySelector('button[type="submit"]')!;

    expect(campo.compareDocumentPosition(botao) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('mantém nome acessível para o botão de conta quando o texto visual está oculto', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const botaoConta = compiled.querySelector('header .conta');

    expect(botaoConta?.getAttribute('aria-label')).toBe('Entrar/Registrar-se');

    autenticado.set(true);
    credencial.set('token-teste');
    fixture.detectChanges();
    TestBed.inject(HttpTestingController).expectOne('/api/moderacao/acesso').flush(false);
    fixture.detectChanges();

    expect(botaoConta?.getAttribute('aria-label')).toBe('Meu perfil');
  });

  it('exibe a navegação de moderação somente após confirmação do servidor', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Moderação');

    autenticado.set(true);
    credencial.set('token-moderador');
    fixture.detectChanges();
    TestBed.inject(HttpTestingController).expectOne('/api/moderacao/acesso').flush(true);
    fixture.detectChanges();

    const links = fixture.nativeElement.querySelectorAll('a[href="/moderacao/denuncias"]');
    expect(links.length).toBe(2);
  });

  it('mantém a moderação no menu lateral em telas compactas', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    autenticado.set(true);
    credencial.set('token-moderador');
    fixture.detectChanges();
    TestBed.inject(HttpTestingController).expectOne('/api/moderacao/acesso').flush(true);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const linkCabecalho = compiled.querySelector('header a[href="/moderacao/denuncias"]');
    const linkMenu = compiled.querySelector('mat-sidenav a[href="/moderacao/denuncias"]');

    expect(window.innerWidth).toBeLessThanOrEqual(800);
    expect(linkCabecalho).not.toBeNull();
    expect(getComputedStyle(linkCabecalho!).display).toBe('none');
    expect(linkMenu).not.toBeNull();
  });
  it('abre o próprio perfil pelo ícone após consultar a identidade na API', () => {
    const fixture = TestBed.createComponent(App);
    autenticado.set(true);
    credencial.set('token-teste');
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/moderacao/acesso').flush(false);
    const navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture.nativeElement.querySelector('header .conta').click();
    http.expectOne('/api/perfil').flush({ id: 42, perfilPublico: true });
    expect(navegar).toHaveBeenCalledWith(['/profissionais', 42]);
  });

});
