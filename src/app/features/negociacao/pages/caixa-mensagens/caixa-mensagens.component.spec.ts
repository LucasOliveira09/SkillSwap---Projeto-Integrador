import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { appConfig } from '../../../../app.config';
import { SessaoService } from '../../../conta/data-access/sessao.service';

describe('CaixaMensagensComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    spyOnProperty(document, 'visibilityState', 'get').and.returnValue('visible');
    TestBed.configureTestingModule({ providers: [...appConfig.providers, provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('apresenta conversas persistidas e envia mensagens pelo acordo selecionado', async () => {
    TestBed.inject(SessaoService).entrar('cliente@teste.invalid', 'senha').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'jwt-teste' });

    const harness = await RouterTestingHarness.create('/mensagens?conversa=7');
    http.expectOne('/api/perfil').flush({
      id: 2, nome: 'Bruno', tituloProfissional: null, bio: null, cidade: null,
      fotoUrl: null, capaUrl: null, especialidades: null,
      criadoEm: '2026-10-01T10:00:00', mediaNotas: null, quantidadeAvaliacoes: 0,
    });
    http.expectOne('/api/conversas?pagina=0&tamanho=20').flush({
      itens: [{
        id: 7, propostaId: 5, anuncioId: 3, anuncioTitulo: 'Aula de violão',
        anuncianteId: 1, anuncianteNome: 'Ana', proponenteId: 2, proponenteNome: 'Bruno',
        status: 'PENDENTE', podeEnviar: true, acordoId: null, naoLidas: 0, ultimaMensagem: null, criadoEm: '2026-10-26T10:00:00', concluidoEm: null,
        conclusaoConfirmadaAnunciante: false, conclusaoConfirmadaProponente: false,
      }], pagina: 0, tamanho: 20, totalItens: 1, totalPaginas: 1,
    });
    http.expectOne('/api/conversas/7/mensagens?pagina=0&tamanho=50').flush({
      itens: [{ id: 9, conversaId: 7, remetenteId: 1, remetenteNome: 'Ana', conteudo: 'Olá, podemos combinar?', enviadoEm: '2026-10-27T11:00:00' }],
      pagina: 0, tamanho: 50, totalItens: 1, totalPaginas: 1,
    });
    http.expectOne('/api/conversas/7/leitura').flush(null);
    harness.detectChanges();

    expect(harness.routeNativeElement!.textContent).toContain('Aula de violão');
    expect(harness.routeNativeElement!.textContent).toContain('Olá, podemos combinar?');
    (harness.routeDebugElement!.componentInstance as { texto: string }).texto = 'Confirmo para amanhã';
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector<HTMLButtonElement>('.form-chat button[type="submit"]')!.disabled).toBeFalse();
    harness.routeNativeElement!.querySelector<HTMLButtonElement>('.form-chat button[type="submit"]')!.click();

    const envio = http.expectOne('/api/conversas/7/mensagens');
    expect(envio.request.method).toBe('POST');
    expect(envio.request.body).toEqual({ conteudo: 'Confirmo para amanhã' });
    envio.flush({ id: 10, conversaId: 7, remetenteId: 2, remetenteNome: 'Bruno', conteudo: 'Confirmo para amanhã', enviadoEm: '2026-10-27T11:02:00' });
    http.expectOne('/api/conversas/7/mensagens?pagina=0&tamanho=50').flush({
      itens: [{ id: 10, conversaId: 7, remetenteId: 2, remetenteNome: 'Bruno', conteudo: 'Confirmo para amanhã', enviadoEm: '2026-10-27T11:02:00' }],
      pagina: 0, tamanho: 50, totalItens: 2, totalPaginas: 1,
    });
    http.expectOne('/api/conversas/7/leitura').flush(null);
    harness.detectChanges();
    expect(harness.routeNativeElement!.textContent).toContain('Confirmo para amanhã');
  });
  it('abre na lista, guarda o rascunho e respeita a navegação de volta', async () => {
    TestBed.inject(SessaoService).entrar('cliente@teste.invalid', 'senha').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'jwt-teste' });
    const harness = await RouterTestingHarness.create('/mensagens');
    http.expectOne('/api/perfil').flush({ id: 2, nome: 'Bruno' });
    http.expectOne('/api/conversas?pagina=0&tamanho=20').flush({
      itens: [{ id: 7, anuncianteId: 1, proponenteId: 2, anuncianteNome: 'Ana',
        proponenteNome: 'Bruno', anuncioTitulo: 'Aula', status: 'PENDENTE', podeEnviar: true, acordoId: null, naoLidas: 0, ultimaMensagem: null }],
      pagina: 0, totalPaginas: 1,
    });
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector('.painel-chat')).toBeNull();
    await harness.navigateByUrl('/mensagens?conversa=7');
    http.expectOne('/api/conversas/7/mensagens?pagina=0&tamanho=50').flush({ itens: [], pagina: 0, totalPaginas: 1 });
    const componente = harness.routeDebugElement!.componentInstance as { texto: string };
    componente.texto = 'Meu rascunho';
    await harness.navigateByUrl('/mensagens');
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector('.painel-chat')).toBeNull();
    expect(harness.routeNativeElement!.querySelector('.item-conversa a')).toBeNull();
    await harness.navigateByUrl('/mensagens?conversa=7');
    http.expectOne('/api/conversas/7/mensagens?pagina=0&tamanho=50').flush({ itens: [], pagina: 0, totalPaginas: 1 });
    expect(componente.texto).toBe('Meu rascunho');
  });

  it('não duplica uma conversa aberta por link ao carregar sua página depois', async () => {
    TestBed.inject(SessaoService).entrar('cliente@teste.invalid', 'senha').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'jwt-teste' });
    const harness = await RouterTestingHarness.create('/mensagens?conversa=7');
    http.expectOne('/api/perfil').flush({ id: 2, nome: 'Bruno' });
    const acordo = { id: 7, anuncianteId: 1, proponenteId: 2, anuncianteNome: 'Ana',
      proponenteNome: 'Bruno', anuncioTitulo: 'Aula', status: 'PENDENTE', podeEnviar: true, acordoId: null, naoLidas: 0, ultimaMensagem: null };
    http.expectOne('/api/conversas?pagina=0&tamanho=20').flush({
      itens: [{ ...acordo, id: 8 }], pagina: 0, totalPaginas: 2,
    });
    http.expectOne('/api/conversas/7').flush(acordo);
    http.expectOne('/api/conversas/7/mensagens?pagina=0&tamanho=50').flush({ itens: [], pagina: 0, totalPaginas: 1 });
    harness.detectChanges();
    harness.routeNativeElement!.querySelector<HTMLButtonElement>('.mais-conversas')!.click();
    http.expectOne('/api/conversas?pagina=1&tamanho=20').flush({
      itens: [acordo, { ...acordo, id: 9 }], pagina: 1, totalPaginas: 2,
    });
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelectorAll('.item-conversa').length).toBe(3);
  });

  it('recebe um primeiro contato na lista aberta e identifica o anúncio sem exigir acordo', async () => {
    TestBed.inject(SessaoService).entrar('anunciante@teste.invalid', 'senha').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'jwt-teste' });
    const harness = await RouterTestingHarness.create('/mensagens');
    http.expectOne('/api/perfil').flush({ id: 1, nome: 'Ana' });
    http.expectOne('/api/conversas?pagina=0&tamanho=20').flush({ itens: [], pagina: 0, totalPaginas: 0 });
    const componente = harness.routeDebugElement!.componentInstance as { atualizarEntrada(): void };
    componente.atualizarEntrada();
    const conversa = { id: 7, anuncioId: 42, anuncioTitulo: 'Aula de violão', anuncianteId: 1,
      anuncianteNome: 'Ana', proponenteId: 2, proponenteNome: 'Bruno', cidade: 'Ourinhos', modalidade: 'VENDA',
      status: 'PENDENTE', acordoId: null, podeEnviar: true, naoLidas: 1,
      ultimaMensagem: { id: 9, conversaId: 7, remetenteId: 2, remetenteNome: 'Bruno', conteudo: 'Atende aos sábados?', enviadoEm: '2026-10-28T12:00:00' } };
    http.expectOne('/api/conversas?pagina=0&tamanho=20').flush({ itens: [conversa], pagina: 0, totalPaginas: 1 });
    harness.detectChanges();
    expect(harness.routeNativeElement!.textContent).toContain('Atende aos sábados?');
    expect(harness.routeNativeElement!.querySelector('.nao-lidas')?.textContent).toContain('1');
    await harness.navigateByUrl('/mensagens?conversa=7');
    http.expectOne('/api/conversas/7/mensagens?pagina=0&tamanho=50').flush({ itens: [conversa.ultimaMensagem], pagina: 0, totalPaginas: 1 });
    const leitura = http.expectOne('/api/conversas/7/leitura');
    expect(leitura.request.body.ultimaMensagemId).toBe(9);
    leitura.flush(null);
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector('.contexto-anuncio')?.textContent).toContain('anúncio #42');
    expect(harness.routeNativeElement!.querySelector('.nao-lidas')).toBeNull();
    expect(harness.routeNativeElement!.querySelector('.form-chat')).not.toBeNull();
  });

});
