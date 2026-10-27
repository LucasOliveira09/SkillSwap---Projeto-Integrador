import { TestBed } from '@angular/core/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { appConfig } from '../../../../app.config';
import { SessaoService } from '../../../conta/data-access/sessao.service';
import { AtualizacaoPerfil } from '../../models/perfil.model';
import { MeuPerfilComponent } from './meu-perfil.component';

describe('MeuPerfilComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [...appConfig.providers, provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('carrega e salva os dados do próprio perfil', async () => {
    TestBed.inject(SessaoService).entrar('ana@teste.invalid', 'senha').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'token-de-teste' });

    const harness = await RouterTestingHarness.create('/meu-perfil');
    http.expectOne('/api/perfil').flush({
      id: 12,
      nome: 'Ana Profissional',
      bio: 'Experiência com instalações residenciais.',
      cidade: 'Florianópolis',
      fotoUrl: null,
      especialidades: 'Elétrica residencial',
      criadoEm: '2026-10-01T10:00:00',
      mediaNotas: null,
      quantidadeAvaliacoes: 0,
    });
    harness.detectChanges();

    const raiz = harness.routeNativeElement!;
    const componente = harness.routeDebugElement!.componentInstance as unknown as {
      dados: AtualizacaoPerfil;
      salvar(form: { invalid: boolean; control: { markAllAsTouched(): void } }): void;
    };
    componente.dados.nome = 'Ana Lima';
    componente.salvar({ invalid: false, control: { markAllAsTouched: () => undefined } });

    const requisicao = http.expectOne('/api/perfil');
    expect(requisicao.request.method).toBe('PATCH');
    expect(requisicao.request.body.nome).toBe('Ana Lima');
    requisicao.flush({
      id: 12,
      nome: 'Ana Lima',
      bio: 'Experiência com instalações residenciais.',
      cidade: 'Florianópolis',
      fotoUrl: null,
      especialidades: 'Elétrica residencial',
      criadoEm: '2026-10-01T10:00:00',
      mediaNotas: null,
      quantidadeAvaliacoes: 0,
    });
    await harness.fixture.whenStable();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('Perfil salvo.');
    expect(componente.dados.nome).toBe('Ana Lima');
  });

  it('encaminha pessoa sem sessão para entrar com destino de retorno', async () => {
    const harness = await RouterTestingHarness.create('/meu-perfil');
    expect(TestBed.inject(SessaoService).autenticado()).toBeFalse();
    expect(harness.routeNativeElement?.textContent).toContain('Entrar');
    expect(TestBed.inject(HttpTestingController).match('/api/perfil')).toHaveSize(0);
  });
});
