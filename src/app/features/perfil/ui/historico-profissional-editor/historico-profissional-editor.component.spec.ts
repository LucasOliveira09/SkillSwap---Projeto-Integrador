import { ComponentFixture, TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { of } from 'rxjs';
import { PerfilApiService } from '../../data-access/perfil-api.service';
import { HistoricoProfissionalEditorComponent } from './historico-profissional-editor.component';

describe('HistoricoProfissionalEditorComponent', () => {
  let fixture: ComponentFixture<HistoricoProfissionalEditorComponent>;
  let api: jasmine.SpyObj<PerfilApiService>;

  beforeEach(() => {
    registerLocaleData(localePt);
    api = jasmine.createSpyObj<PerfilApiService>('PerfilApiService', [
      'minhasExperiencias', 'minhasCertificacoes', 'excluirExperiencia', 'excluirCertificacao',
    ]);
    api.minhasExperiencias.and.returnValue(of([
      { id: 5, cargo: 'Designer', organizacao: 'Estúdio', cidade: null,
        dataInicio: '2022-01-01', dataFim: null, descricao: null },
    ]));
    api.minhasCertificacoes.and.returnValue(of([
      { id: 8, nome: 'UX', emissor: 'Escola', dataEmissao: '2024-05-01', dataValidade: null, urlCredencial: null },
    ]));
    api.excluirExperiencia.and.returnValue(of(void 0));
    api.excluirCertificacao.and.returnValue(of(void 0));
    TestBed.configureTestingModule({ imports: [HistoricoProfissionalEditorComponent], providers: [
      { provide: PerfilApiService, useValue: api },
    ] });
    fixture = TestBed.createComponent(HistoricoProfissionalEditorComponent);
    fixture.detectChanges();
  });

  it('carrega e apresenta dados declarados pela pessoa', () => {
    expect(api.minhasExperiencias).toHaveBeenCalled();
    expect(api.minhasCertificacoes).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Designer');
    expect(fixture.nativeElement.textContent).toContain('UX');
    expect(fixture.nativeElement.textContent).toContain('não verificadas pelo SkillSwap');
  });

  it('remove experiência somente após confirmação da API', () => {
    const remover = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button'))
      .find(botao => botao.textContent?.includes('Remover'))!;
    remover.click();
    fixture.detectChanges();
    expect(api.excluirExperiencia).toHaveBeenCalledWith(5);
    expect(fixture.nativeElement.textContent).toContain('Você ainda não adicionou experiências.');
    expect(fixture.nativeElement.textContent).toContain('Experiência removida.');
  });
});
