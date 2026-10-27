import { TestBed } from '@angular/core/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { appConfig } from '../../../../app.config';
import { SessaoService } from '../../../conta/data-access/sessao.service';

describe('AcordoDetalheComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [...appConfig.providers, provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('aguarda a confirmação da outra parte antes de liberar avaliações', async () => {
    TestBed.inject(SessaoService).entrar('cliente@teste.invalid', 'senha').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'jwt-teste' });

    const harness = await RouterTestingHarness.create('/acordos/7');
    const acordo = {
      id: 7, propostaId: 5, anuncioId: 3, anuncioTitulo: 'Aula de violão',
      anuncianteId: 1, anuncianteNome: 'Ana', proponenteId: 2, proponenteNome: 'Bruno',
      status: 'EM_ANDAMENTO', criadoEm: '2026-10-26T10:00:00', concluidoEm: null,
      conclusaoConfirmadaAnunciante: false, conclusaoConfirmadaProponente: false,
    };
    http.expectOne('/api/acordos/7').flush(acordo);
    http.expectOne('/api/acordos/7/eventos').flush([]);
    http.expectOne('/api/acordos/7/mensagens?pagina=0&tamanho=50').flush({
      itens: [], pagina: 0, tamanho: 50, totalItens: 0, totalPaginas: 0,
    });
    http.expectOne('/api/acordos/7/avaliacoes').flush([]);
    http.expectOne('/api/perfil').flush({
      id: 2, nome: 'Bruno', bio: null, cidade: null, fotoUrl: null, especialidades: null,
      criadoEm: '2026-10-01T10:00:00', mediaNotas: null, quantidadeAvaliacoes: 0,
    });
    harness.detectChanges();

    harness.routeNativeElement!.querySelector<HTMLButtonElement>('.concluir')!.click();
    harness.detectChanges();
    const confirmar = Array.from(harness.routeNativeElement!.querySelectorAll('button'))
      .find((botao) => botao.textContent?.includes('Confirmar conclusão')) as HTMLButtonElement;
    confirmar.click();
    const atualizacao = http.expectOne('/api/acordos/7');
    expect(atualizacao.request.method).toBe('PATCH');
    expect(atualizacao.request.body).toEqual({ status: 'CONCLUIDO' });
    atualizacao.flush({ ...acordo, conclusaoConfirmadaProponente: true });
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent || '';
    expect(texto).toContain('Sua confirmação foi registrada');
    expect(texto).toContain('quando a outra pessoa também confirmar');
    expect(harness.routeNativeElement?.querySelector('.avaliacao')).toBeNull();
    expect(harness.routeNativeElement?.querySelector('.concluir')).toBeNull();
  });

  it('não oferece avaliação ao profissional depois da conclusão', async () => {
    TestBed.inject(SessaoService).entrar('profissional@teste.invalid', 'senha').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'jwt-teste' });

    const harness = await RouterTestingHarness.create('/acordos/7');
    http.expectOne('/api/acordos/7').flush({
      id: 7, propostaId: 5, anuncioId: 3, anuncioTitulo: 'Aula de violão',
      anuncianteId: 1, anuncianteNome: 'Ana', proponenteId: 2, proponenteNome: 'Bruno',
      status: 'CONCLUIDO', criadoEm: '2026-10-26T10:00:00', concluidoEm: '2026-10-26T11:00:00',
      conclusaoConfirmadaAnunciante: true, conclusaoConfirmadaProponente: true,
    });
    http.expectOne('/api/acordos/7/eventos').flush([
      { id: 1, tipo: 'ACORDO_INICIADO', atorId: 1, atorNome: 'Ana', criadoEm: '2026-10-26T10:00:00' },
      { id: 2, tipo: 'CONCLUSAO_CONFIRMADA', atorId: 1, atorNome: 'Ana', criadoEm: '2026-10-26T11:00:00' },
    ]);
    http.expectOne('/api/acordos/7/mensagens?pagina=0&tamanho=50').flush({
      itens: [], pagina: 0, tamanho: 50, totalItens: 0, totalPaginas: 0,
    });
    http.expectOne('/api/acordos/7/avaliacoes').flush([]);
    http.expectOne('/api/perfil').flush({
      id: 1, nome: 'Ana', bio: null, cidade: null, fotoUrl: null, especialidades: null,
      criadoEm: '2026-10-01T10:00:00', mediaNotas: null, quantidadeAvaliacoes: 0,
    });
    harness.detectChanges();

    const secao = harness.routeNativeElement!.querySelector('.avaliacao')!;
    expect(secao.textContent).toContain('O cliente que enviou a proposta poderá avaliar');
    expect(secao.querySelector('form')).toBeNull();
    expect(harness.routeNativeElement!.querySelector('.historico-acordo')?.textContent)
      .toContain('Confirmou que o trabalho foi concluído');
  });
});
