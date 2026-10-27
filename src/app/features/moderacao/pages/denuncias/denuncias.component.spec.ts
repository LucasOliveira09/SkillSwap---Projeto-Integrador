import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ContestacaoAvaliacao } from '../../../avaliacao/models/contestacao.model';
import { Denuncia, HistoricoAvaliacao } from '../../models/moderacao.model';
import { ModeracaoApiService } from '../../data-access/moderacao-api.service';
import { DenunciasComponent } from './denuncias.component';

describe('DenunciasComponent', () => {
  let fixture: ComponentFixture<DenunciasComponent>;
  let api: jasmine.SpyObj<ModeracaoApiService>;

  const contestacao: ContestacaoAvaliacao = {
    id: 5,
    avaliacaoId: 8,
    acordoId: 12,
    contestadorId: 2,
    contestadorNome: 'Profissional',
    avaliadorNome: 'Cliente',
    avaliadoNome: 'Profissional',
    nota: 2,
    notaAnuncio: 3,
    trechoAvaliacao: 'O serviço não foi feito conforme combinado.',
    justificativa: 'O texto relata uma etapa que não fazia parte do serviço combinado.',
    status: 'ABERTA',
    criadaEm: '2026-10-27T10:00:00',
    decisao: null,
    analisadaEm: null,
    analisadaPor: null,
  };
  const denuncia: Denuncia = {
    id: 11, tipoConteudo: 'AVALIACAO', recursoId: 8, motivo: 'OUTRO', descricao: null,
    status: 'ABERTA', criadaEm: '2026-10-27T09:00:00', analisadaEm: null, analisadaPor: null,
    autorId: 3, autorNome: 'Pessoa denunciante', trechoConteudo: 'Relato denunciado',
  };
  const historico: HistoricoAvaliacao = {
    avaliacaoId: 8, notaAnterior: 2, notaAnuncioAnterior: 3, depoimentoAnterior: 'Versão anterior',
    editorNome: 'Cliente', registradaEm: '2026-10-27T10:00:00',
  };

  beforeEach(async () => {
    api = jasmine.createSpyObj<ModeracaoApiService>(
      'ModeracaoApiService',
      ['acesso', 'listar', 'decidir', 'listarContestacoes', 'decidirContestacao', 'historicoAvaliacao'],
    );
    api.acesso.and.returnValue(of(true));
    api.listar.and.returnValue(of({ itens: [denuncia], pagina: 0, tamanho: 20, totalItens: 1, totalPaginas: 1 }));
    api.listarContestacoes.and.returnValue(of({ itens: [contestacao], pagina: 0, tamanho: 20, totalItens: 1, totalPaginas: 1 }));
    api.decidirContestacao.and.returnValue(of({ ...contestacao, status: 'OCULTADA' }));
    api.historicoAvaliacao.and.returnValue(of(historico));

    await TestBed.configureTestingModule({
      imports: [DenunciasComponent],
      providers: [provideRouter([]), { provide: ModeracaoApiService, useValue: api }],
    }).compileComponents();

    fixture = TestBed.createComponent(DenunciasComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('apresenta a justificativa privada e pede uma fundamentação para a decisão', () => {
    const pagina = fixture.nativeElement as HTMLElement;
    expect(pagina.textContent).toContain('Contestações de avaliações');
    expect(pagina.textContent).toContain(contestacao.justificativa);
    expect(pagina.textContent).toContain('Ocultar avaliação e notas');
    expect(api.listarContestacoes).toHaveBeenCalledWith('ABERTA', 0);
  });

  it('envia a decisão fundamentada para a contestação selecionada', () => {
    const pagina = fixture.nativeElement as HTMLElement;
    const campo = pagina.querySelector<HTMLTextAreaElement>('#decisao-contestacao-5')!;
    campo.value = 'A descrição contém afirmações que não correspondem ao acordo confirmado.';
    campo.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    const botao = [...pagina.querySelectorAll('button')]
      .find((item) => item.textContent?.includes('Ocultar avaliação e notas'))!;
    botao.click();
    fixture.detectChanges();
    expect(api.decidirContestacao).toHaveBeenCalledWith(
      5,
      'OCULTAR',
      'A descrição contém afirmações que não correspondem ao acordo confirmado.',
    );
  });

  it('permite à moderação consultar a versão anterior de uma avaliação denunciada', () => {
    const pagina = fixture.nativeElement as HTMLElement;
    const botao = [...pagina.querySelectorAll('button')]
      .find((item) => item.textContent?.includes('Consultar versões anteriores'))!;
    botao.click();
    fixture.detectChanges();
    expect(api.historicoAvaliacao).toHaveBeenCalledWith(8);
    expect(pagina.textContent).toContain('Versão anterior, editada por Cliente');
    expect(pagina.textContent).toContain('Versão anterior');
  });
});
