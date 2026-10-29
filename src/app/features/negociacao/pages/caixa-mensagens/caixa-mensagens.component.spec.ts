import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { appConfig } from '../../../../app.config';
import { SessaoService } from '../../../conta/data-access/sessao.service';

describe('CaixaMensagensComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [...appConfig.providers, provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('apresenta conversas persistidas e envia mensagens pelo acordo selecionado', async () => {
    TestBed.inject(SessaoService).entrar('cliente@teste.invalid', 'senha').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'jwt-teste' });

    const harness = await RouterTestingHarness.create('/mensagens?acordo=7');
    http.expectOne('/api/perfil').flush({
      id: 2, nome: 'Bruno', tituloProfissional: null, bio: null, cidade: null,
      fotoUrl: null, capaUrl: null, especialidades: null,
      criadoEm: '2026-10-01T10:00:00', mediaNotas: null, quantidadeAvaliacoes: 0,
    });
    http.expectOne('/api/acordos?pagina=0&tamanho=20').flush({
      itens: [{
        id: 7, propostaId: 5, anuncioId: 3, anuncioTitulo: 'Aula de violão',
        anuncianteId: 1, anuncianteNome: 'Ana', proponenteId: 2, proponenteNome: 'Bruno',
        status: 'EM_ANDAMENTO', criadoEm: '2026-10-26T10:00:00', concluidoEm: null,
        conclusaoConfirmadaAnunciante: false, conclusaoConfirmadaProponente: false,
      }], pagina: 0, tamanho: 20, totalItens: 1, totalPaginas: 1,
    });
    http.expectOne('/api/acordos/7/mensagens?pagina=0&tamanho=50').flush({
      itens: [{ id: 9, acordoId: 7, remetenteId: 1, remetenteNome: 'Ana', conteudo: 'Olá, podemos combinar?', enviadoEm: '2026-10-27T11:00:00' }],
      pagina: 0, tamanho: 50, totalItens: 1, totalPaginas: 1,
    });
    harness.detectChanges();

    expect(harness.routeNativeElement!.textContent).toContain('Aula de violão');
    expect(harness.routeNativeElement!.textContent).toContain('Olá, podemos combinar?');
    (harness.routeDebugElement!.componentInstance as { texto: string }).texto = 'Confirmo para amanhã';
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector<HTMLButtonElement>('.form-chat button[type="submit"]')!.disabled).toBeFalse();
    harness.routeNativeElement!.querySelector<HTMLButtonElement>('.form-chat button[type="submit"]')!.click();

    const envio = http.expectOne('/api/acordos/7/mensagens');
    expect(envio.request.method).toBe('POST');
    expect(envio.request.body).toEqual({ conteudo: 'Confirmo para amanhã' });
    envio.flush({ id: 10, acordoId: 7, remetenteId: 2, remetenteNome: 'Bruno', conteudo: 'Confirmo para amanhã', enviadoEm: '2026-10-27T11:02:00' });
    http.expectOne('/api/acordos/7/mensagens?pagina=0&tamanho=50').flush({
      itens: [{ id: 10, acordoId: 7, remetenteId: 2, remetenteNome: 'Bruno', conteudo: 'Confirmo para amanhã', enviadoEm: '2026-10-27T11:02:00' }],
      pagina: 0, tamanho: 50, totalItens: 2, totalPaginas: 1,
    });
    harness.detectChanges();
    expect(harness.routeNativeElement!.textContent).toContain('Confirmo para amanhã');
  });
  it('abre na lista, guarda o rascunho e respeita a navegação de volta', async () => {
    TestBed.inject(SessaoService).entrar('cliente@teste.invalid', 'senha').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'jwt-teste' });
    const harness = await RouterTestingHarness.create('/mensagens');
    http.expectOne('/api/perfil').flush({ id: 2, nome: 'Bruno' });
    http.expectOne('/api/acordos?pagina=0&tamanho=20').flush({
      itens: [{ id: 7, anuncianteId: 1, proponenteId: 2, anuncianteNome: 'Ana',
        proponenteNome: 'Bruno', anuncioTitulo: 'Aula', status: 'EM_ANDAMENTO' }],
      pagina: 0, totalPaginas: 1,
    });
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector('.painel-chat')).toBeNull();
    await harness.navigateByUrl('/mensagens?acordo=7');
    http.expectOne('/api/acordos/7/mensagens?pagina=0&tamanho=50').flush({ itens: [], pagina: 0, totalPaginas: 1 });
    const componente = harness.routeDebugElement!.componentInstance as { texto: string };
    componente.texto = 'Meu rascunho';
    await harness.navigateByUrl('/mensagens');
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector('.painel-chat')).toBeNull();
    expect(harness.routeNativeElement!.querySelector('.item-conversa a')).toBeNull();
    await harness.navigateByUrl('/mensagens?acordo=7');
    http.expectOne('/api/acordos/7/mensagens?pagina=0&tamanho=50').flush({ itens: [], pagina: 0, totalPaginas: 1 });
    expect(componente.texto).toBe('Meu rascunho');
  });

  it('não duplica uma conversa aberta por link ao carregar sua página depois', async () => {
    TestBed.inject(SessaoService).entrar('cliente@teste.invalid', 'senha').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'jwt-teste' });
    const harness = await RouterTestingHarness.create('/mensagens?acordo=7');
    http.expectOne('/api/perfil').flush({ id: 2, nome: 'Bruno' });
    const acordo = { id: 7, anuncianteId: 1, proponenteId: 2, anuncianteNome: 'Ana',
      proponenteNome: 'Bruno', anuncioTitulo: 'Aula', status: 'EM_ANDAMENTO' };
    http.expectOne('/api/acordos?pagina=0&tamanho=20').flush({
      itens: [{ ...acordo, id: 8 }], pagina: 0, totalPaginas: 2,
    });
    http.expectOne('/api/acordos/7').flush(acordo);
    http.expectOne('/api/acordos/7/mensagens?pagina=0&tamanho=50').flush({ itens: [], pagina: 0, totalPaginas: 1 });
    harness.detectChanges();
    harness.routeNativeElement!.querySelector<HTMLButtonElement>('.mais-conversas')!.click();
    http.expectOne('/api/acordos?pagina=1&tamanho=20').flush({
      itens: [acordo, { ...acordo, id: 9 }], pagina: 1, totalPaginas: 2,
    });
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelectorAll('.item-conversa').length).toBe(3);
  });

});
