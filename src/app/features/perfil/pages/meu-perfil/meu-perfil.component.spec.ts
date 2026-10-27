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
      tituloProfissional: 'Eletricista residencial',
      bio: 'Experiência com instalações residenciais.',
      cidade: 'Florianópolis',
      fotoUrl: null,
      especialidades: 'Elétrica residencial',
      criadoEm: '2026-10-01T10:00:00',
      mediaNotas: null,
      quantidadeAvaliacoes: 0,
    });
    http.expectOne('/api/uploads/uso').flush({ usoBytes: 4096, limiteBytes: 104857600, limitePorArquivoBytes: 5242880 });
    http.expectOne((request) => request.url === '/api/perfis/12/anuncios').flush({
      itens: [], pagina: 0, tamanho: 6, totalItens: 0, totalPaginas: 0,
    });
    http.expectOne((request) => request.url === '/api/perfis/12/publicacoes').flush({
      itens: [], pagina: 0, tamanho: 12, totalItens: 0, totalPaginas: 0,
    });
    harness.detectChanges();

    const raiz = harness.routeNativeElement!;
    const componente = harness.routeDebugElement!.componentInstance as unknown as {
      dados: AtualizacaoPerfil;
      salvar(form: { invalid: boolean; control: { markAllAsTouched(): void } }): void;
    };
    componente.dados.nome = 'Ana Lima';
    componente.dados.tituloProfissional = 'Eletricista autônoma';
    componente.salvar({ invalid: false, control: { markAllAsTouched: () => undefined } });

    const requisicao = http.expectOne('/api/perfil');
    expect(requisicao.request.method).toBe('PATCH');
    expect(requisicao.request.body.nome).toBe('Ana Lima');
    expect(requisicao.request.body.tituloProfissional).toBe('Eletricista autônoma');
    requisicao.flush({
      id: 12,
      nome: 'Ana Lima',
      tituloProfissional: 'Eletricista autônoma',
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

  it('edita e persiste a legenda de uma publicação existente', async () => {
    TestBed.inject(SessaoService).entrar('ana@teste.invalid', 'senha').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'token-de-teste' });
    const harness = await RouterTestingHarness.create('/meu-perfil');
    http.expectOne('/api/perfil').flush({
      id: 12, nome: 'Ana Profissional', tituloProfissional: null, bio: null, cidade: null, fotoUrl: null,
      especialidades: null, criadoEm: '2026-10-01T10:00:00', mediaNotas: null, quantidadeAvaliacoes: 0,
    });
    http.expectOne('/api/uploads/uso').flush({ usoBytes: 0, limiteBytes: 104857600, limitePorArquivoBytes: 5242880 });
    http.expectOne((request) => request.urlWithParams === '/api/perfis/12/anuncios?pagina=0&tamanho=6')
      .flush({ itens: [], pagina: 0, tamanho: 6, totalItens: 0, totalPaginas: 0 });
    http.expectOne((request) => request.urlWithParams === '/api/perfis/12/publicacoes?pagina=0&tamanho=12').flush({
      itens: [{
        id: 9, profissionalId: 12, profissionalNome: 'Ana Profissional', anuncioId: null,
        anuncioTitulo: null, legenda: 'Instalação concluída', fotos: ['/api/uploads/foto.png'],
        criadoEm: '2026-10-02T10:00:00',
      }],
      pagina: 0, tamanho: 12, totalItens: 1, totalPaginas: 1,
    });
    harness.detectChanges();

    const publicacao = {
      id: 9, profissionalId: 12, profissionalNome: 'Ana Profissional', anuncioId: null,
      anuncioTitulo: null, legenda: 'Instalação concluída', fotos: ['/api/uploads/foto.png'],
      criadoEm: '2026-10-02T10:00:00',
    };
    const componente = harness.routeDebugElement!.componentInstance as unknown as {
      iniciarEdicaoPublicacao(item: typeof publicacao): void;
      salvarEdicaoPublicacao(item: typeof publicacao): void;
      legendaEmEdicao: string;
      publicacoes(): readonly typeof publicacao[];
      editandoPublicacaoId(): number | null;
    };
    expect(componente.publicacoes()).toHaveSize(1);
    componente.iniciarEdicaoPublicacao(publicacao);
    expect(componente.editandoPublicacaoId()).toBe(9);
    harness.detectChanges();
    componente.legendaEmEdicao = 'Resultado revisado';
    componente.salvarEdicaoPublicacao(publicacao);

    const atualizar = http.expectOne('/api/publicacoes/9');
    expect(atualizar.request.method).toBe('PATCH');
    expect(atualizar.request.body).toEqual({ legenda: 'Resultado revisado' });
    atualizar.flush({
      id: 9, profissionalId: 12, profissionalNome: 'Ana Profissional', anuncioId: null,
      anuncioTitulo: null, legenda: 'Resultado revisado', fotos: ['/api/uploads/foto.png'],
      criadoEm: '2026-10-02T10:00:00',
    });
    await harness.fixture.whenStable();
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Resultado revisado');
    expect(harness.routeNativeElement?.textContent).toContain('Legenda da publicação atualizada.');
  });

  it('envia a foto selecionada e atualiza a prévia persistida pelo servidor', async () => {
    TestBed.inject(SessaoService).entrar('ana@teste.invalid', 'senha').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'token-de-teste' });
    const harness = await RouterTestingHarness.create('/meu-perfil');
    http.expectOne('/api/perfil').flush({
      id: 12,
      nome: 'Ana Profissional',
      tituloProfissional: null,
      bio: null,
      cidade: null,
      fotoUrl: null,
      especialidades: null,
      criadoEm: '2026-10-01T10:00:00',
      mediaNotas: null,
      quantidadeAvaliacoes: 0,
    });
    http.expectOne('/api/uploads/uso').flush({ usoBytes: 0, limiteBytes: 104857600, limitePorArquivoBytes: 5242880 });
    http.expectOne((request) => request.url === '/api/perfis/12/anuncios').flush({
      itens: [], pagina: 0, tamanho: 6, totalItens: 0, totalPaginas: 0,
    });
    http.expectOne((request) => request.url === '/api/perfis/12/publicacoes').flush({
      itens: [], pagina: 0, tamanho: 12, totalItens: 0, totalPaginas: 0,
    });
    harness.detectChanges();

    const componente = harness.routeDebugElement!.componentInstance as unknown as {
      arquivoFoto: File | null;
      selecionarFoto(evento: Event): void;
      enviarFoto(): void;
      perfil(): { fotoUrl: string | null } | null;
    };
    const arquivo = new File(['imagem'], 'perfil.png', { type: 'image/png' });
    const campo = { files: [arquivo], value: 'perfil.png' } as unknown as HTMLInputElement;
    componente.selecionarFoto({ target: campo } as unknown as Event);
    expect(componente.arquivoFoto).toBe(arquivo);
    componente.enviarFoto();

    const requisicao = http.expectOne('/api/perfil/foto');
    expect(requisicao.request.method).toBe('PUT');
    expect(requisicao.request.body instanceof FormData).toBeTrue();
    requisicao.flush({
      id: 12,
      nome: 'Ana Profissional',
      tituloProfissional: null,
      bio: null,
      cidade: null,
      fotoUrl: '/api/uploads/0123456789abcdef0123456789abcdef.png',
      especialidades: null,
      criadoEm: '2026-10-01T10:00:00',
      mediaNotas: null,
      quantidadeAvaliacoes: 0,
    });
    http.expectOne('/api/uploads/uso').flush({ usoBytes: 1024, limiteBytes: 104857600, limitePorArquivoBytes: 5242880 });
    harness.detectChanges();

    expect(componente.perfil()?.fotoUrl).toBe('/api/uploads/0123456789abcdef0123456789abcdef.png');
    expect(harness.routeNativeElement?.textContent).toContain('Foto do perfil atualizada.');
  });
});
