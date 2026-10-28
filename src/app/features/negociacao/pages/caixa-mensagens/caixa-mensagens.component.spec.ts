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
});
