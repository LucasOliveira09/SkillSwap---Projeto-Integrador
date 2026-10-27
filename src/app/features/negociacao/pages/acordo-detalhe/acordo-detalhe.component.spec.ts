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
    http.expectOne('/api/acordos/7/avaliacoes').flush([{
      id: 12, acordoId: 7, avaliadorId: 2, avaliadorNome: 'Bruno', avaliadoId: 1, avaliadoNome: 'Ana',
      nota: 5, notaAnuncio: 4, depoimento: 'Ótimo serviço', criadoEm: '2026-10-26T10:30:00',
      respostaProfissional: null, respondidaEm: null,
    }]);
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

  it('permite ao profissional responder à avaliação sem avaliar o próprio acordo', async () => {
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
    http.expectOne('/api/acordos/7/avaliacoes').flush([{
      id: 12, acordoId: 7, avaliadorId: 2, avaliadorNome: 'Bruno', avaliadoId: 1, avaliadoNome: 'Ana',
      nota: 5, notaAnuncio: 4, depoimento: 'Ótimo serviço', criadoEm: '2026-10-26T10:30:00',
      respostaProfissional: null, respondidaEm: null,
    }]);
    http.expectOne('/api/perfil').flush({
      id: 1, nome: 'Ana', bio: null, cidade: null, fotoUrl: null, especialidades: null,
      criadoEm: '2026-10-01T10:00:00', mediaNotas: null, quantidadeAvaliacoes: 0,
    });
    http.expectOne('/api/avaliacoes/12/contestacoes').flush([]);
    harness.detectChanges();

    const secao = harness.routeNativeElement!.querySelector('.avaliacao')!;
    expect(secao.textContent).toContain('O cliente que enviou a proposta poderá avaliar');
    expect(secao.querySelector('fieldset')).toBeNull();
    const campoContestacao = secao.querySelector<HTMLTextAreaElement>('.contestar-avaliacao textarea')!;
    campoContestacao.value = 'O depoimento relata uma etapa que não fazia parte do combinado.';
    campoContestacao.dispatchEvent(new Event('input'));
    harness.detectChanges();
    secao.querySelector<HTMLButtonElement>('.contestar-avaliacao button')!.click();
    const contestar = http.expectOne('/api/avaliacoes/12/contestacoes');
    expect(contestar.request.method).toBe('POST');
    expect(contestar.request.body).toEqual({ justificativa: 'O depoimento relata uma etapa que não fazia parte do combinado.' });
    contestar.flush({
      id: 4, avaliacaoId: 12, acordoId: 7, contestadorId: 1, contestadorNome: 'Ana',
      avaliadorNome: 'Bruno', avaliadoNome: 'Ana', nota: 5, notaAnuncio: 4,
      trechoAvaliacao: 'Ótimo serviço', justificativa: 'O depoimento relata uma etapa que não fazia parte do combinado.',
      status: 'ABERTA', criadaEm: '2026-10-27T11:00:00', decisao: null, analisadaEm: null, analisadaPor: null,
    });
    harness.detectChanges();
    expect(secao.textContent).toContain('Contestação em análise');
    expect(secao.querySelector('.contestar-avaliacao')).toBeNull();

    const campoResposta = secao.querySelector<HTMLTextAreaElement>('.responder-avaliacao textarea')!;
    campoResposta.value = '  Obrigado pelo retorno.  ';
    campoResposta.dispatchEvent(new Event('input'));
    harness.detectChanges();
    const publicar = secao.querySelector<HTMLButtonElement>('.responder-avaliacao button')!;
    expect(publicar.disabled).toBeFalse();
    publicar.click();
    const resposta = http.expectOne('/api/acordos/7/avaliacoes/12/resposta');
    expect(resposta.request.method).toBe('PATCH');
    expect(resposta.request.body).toEqual({ resposta: 'Obrigado pelo retorno.' });
    resposta.flush({
      id: 12, acordoId: 7, avaliadorId: 2, avaliadorNome: 'Bruno', avaliadoId: 1, avaliadoNome: 'Ana',
      nota: 5, notaAnuncio: 4, depoimento: 'Ótimo serviço', criadoEm: '2026-10-26T10:30:00',
      respostaProfissional: 'Obrigado pelo retorno.', respondidaEm: '2026-10-26T11:00:00',
    });
    harness.detectChanges();
    expect(secao.textContent).toContain('Obrigado pelo retorno.');
    expect(secao.querySelector('.responder-avaliacao')).toBeNull();
    expect(harness.routeNativeElement!.querySelector('.historico-acordo')?.textContent)
      .toContain('Confirmou que o trabalho foi concluído');
  });

  it('permite ao autor editar uma vez a avaliação dentro do prazo', async () => {
    TestBed.inject(SessaoService).entrar('cliente@teste.invalid', 'senha').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'jwt-teste' });

    const harness = await RouterTestingHarness.create('/acordos/7');
    http.expectOne('/api/acordos/7').flush({
      id: 7, propostaId: 5, anuncioId: 3, anuncioTitulo: 'Aula de violão',
      anuncianteId: 1, anuncianteNome: 'Ana', proponenteId: 2, proponenteNome: 'Bruno',
      status: 'CONCLUIDO', criadoEm: '2026-10-26T10:00:00', concluidoEm: '2026-10-26T11:00:00',
      conclusaoConfirmadaAnunciante: true, conclusaoConfirmadaProponente: true,
    });
    http.expectOne('/api/acordos/7/eventos').flush([]);
    http.expectOne('/api/acordos/7/mensagens?pagina=0&tamanho=50').flush({
      itens: [], pagina: 0, tamanho: 50, totalItens: 0, totalPaginas: 0,
    });
    http.expectOne('/api/acordos/7/avaliacoes').flush([{
      id: 12, acordoId: 7, avaliadorId: 2, avaliadorNome: 'Bruno', avaliadoId: 1, avaliadoNome: 'Ana',
      nota: 3, notaAnuncio: 4, depoimento: 'Relato original', criadoEm: new Date().toISOString(),
      respostaProfissional: null, respondidaEm: null, edicaoRealizada: false, editadaEm: null,
    }]);
    http.expectOne('/api/perfil').flush({
      id: 2, nome: 'Bruno', bio: null, cidade: null, fotoUrl: null, especialidades: null,
      criadoEm: '2026-10-01T10:00:00', mediaNotas: null, quantidadeAvaliacoes: 0,
    });
    harness.detectChanges();

    const secao = harness.routeNativeElement!.querySelector('.avaliacao')!;
    secao.querySelector<HTMLButtonElement>('article.avaliacao-publicada > button')!.click();
    harness.detectChanges();
    expect(secao.textContent).toContain('Apenas uma edição é permitida nas primeiras 24 horas');
    const notaProfissional = secao.querySelectorAll<HTMLButtonElement>('.editar-avaliacao fieldset')[0].querySelectorAll('button');
    notaProfissional[4].click();
    const notaOferta = secao.querySelectorAll<HTMLButtonElement>('.editar-avaliacao fieldset')[1].querySelectorAll('button');
    notaOferta[3].click();
    const depoimento = secao.querySelector<HTMLTextAreaElement>('.editar-avaliacao textarea')!;
    depoimento.value = 'Relato com contexto revisado';
    depoimento.dispatchEvent(new Event('input'));
    harness.detectChanges();
    secao.querySelector<HTMLButtonElement>('.acoes-edicao button[type="submit"]')!.click();
    const request = http.expectOne('/api/acordos/7/avaliacoes/12');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ nota: 5, notaAnuncio: 4, depoimento: 'Relato com contexto revisado' });
    request.flush({
      id: 12, acordoId: 7, avaliadorId: 2, avaliadorNome: 'Bruno', avaliadoId: 1, avaliadoNome: 'Ana',
      nota: 5, notaAnuncio: 4, depoimento: 'Relato com contexto revisado', criadoEm: new Date().toISOString(),
      respostaProfissional: null, respondidaEm: null, edicaoRealizada: true, editadaEm: new Date().toISOString(),
    });
    harness.detectChanges();
    expect(secao.textContent).toContain('Relato com contexto revisado');
    expect(secao.textContent).toContain('foi editada em');
    expect(secao.querySelector('.editar-avaliacao')).toBeNull();
  });
});
