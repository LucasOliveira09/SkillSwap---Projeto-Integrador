import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { SharedModule } from '../../../../shared/shared.module';
import { PainelComponent } from './painel.component';

describe('PainelComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [PainelComponent],
      imports: [SharedModule],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('exibe os KPIs calculados pela API e oculta a seção ADMIN para usuário comum', () => {
    const fixture = TestBed.createComponent(PainelComponent);
    http.expectOne('/api/painel').flush({
      anunciosAtivos: 2, anunciosPausados: 1, propostasRecebidasPendentes: 3, propostasEnviadasPendentes: 0,
      acordosEmAndamento: 1, acordosConcluidos: 4, taxaAceitacao: 66.7, notaMedia: 4.5, avaliacoesRecebidas: 2,
    });
    http.expectOne('/api/moderacao/acesso').flush(false);
    fixture.detectChanges();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    const kpis = (fixture.nativeElement as HTMLElement).querySelectorAll('.kpi');
    expect(kpis.length).toBe(5);
    expect(texto).toContain('Anúncios ativos');
    expect(texto).toContain('66,7%');
    expect(texto).toContain('4,5');
    expect(texto).not.toContain('Plataforma');
  });

  it('carrega os indicadores da plataforma para o perfil ADMIN', () => {
    const fixture = TestBed.createComponent(PainelComponent);
    http.expectOne('/api/painel').flush({
      anunciosAtivos: 0, anunciosPausados: 0, propostasRecebidasPendentes: 0, propostasEnviadasPendentes: 0,
      acordosEmAndamento: 0, acordosConcluidos: 0, taxaAceitacao: null, notaMedia: null, avaliacoesRecebidas: 0,
    });
    http.expectOne('/api/moderacao/acesso').flush(true);
    http.expectOne('/api/painel/plataforma').flush({
      usuarios: 10, anunciosAtivos: 7, propostasPorStatus: { PENDENTE: 2, ACEITA: 3 },
      acordosPorStatus: { CONCLUIDO: 3 }, taxaConclusaoAcordos: 100, mediaAvaliacoes: 4.8,
      anunciosAtivosPorCategoria: { AULAS: 5, MANUTENCAO: 2 },
    });
    fixture.detectChanges();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Plataforma');
    expect(texto).toContain('Aulas e instrução');
    expect(texto).toContain('—');
  });
});
