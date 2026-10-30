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
      capaUrl: null,
      perfilPublico: true,
      especialidades: 'Elétrica residencial',
      criadoEm: '2026-10-01T10:00:00',
      mediaNotas: null,
      quantidadeAvaliacoes: 0,
    });
    http.expectOne('/api/uploads/uso').flush({ usoBytes: 4096, limiteBytes: 104857600, limitePorArquivoBytes: 5242880 });
    http.expectOne((request) => request.url === '/api/perfis/12/anuncios').flush({
      itens: [], pagina: 0, tamanho: 6, totalItens: 0, totalPaginas: 0,
    });
    http.expectOne((request) => request.url === '/api/publicacoes').flush({
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
    componente.dados.disponivelTrabalhos = true;
    componente.salvar({ invalid: false, control: { markAllAsTouched: () => undefined } });

    const requisicao = http.expectOne('/api/perfil');
    expect(requisicao.request.method).toBe('PATCH');
    expect(requisicao.request.body.nome).toBe('Ana Lima');
    expect(requisicao.request.body.tituloProfissional).toBe('Eletricista autônoma');
    expect(requisicao.request.body.perfilPublico).toBeTrue();
    expect(requisicao.request.body.disponivelTrabalhos).toBeTrue();
    requisicao.flush({
      id: 12,
      nome: 'Ana Lima',
      tituloProfissional: 'Eletricista autônoma',
      bio: 'Experiência com instalações residenciais.',
      cidade: 'Florianópolis',
      fotoUrl: null,
      capaUrl: null,
      especialidades: 'Elétrica residencial',
      criadoEm: '2026-10-01T10:00:00',
      mediaNotas: null,
      quantidadeAvaliacoes: 0,
    });
    await harness.fixture.whenStable();
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('Perfil salvo e visível publicamente.');
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
      id: 12, nome: 'Ana Profissional', tituloProfissional: null, bio: null, cidade: null, fotoUrl: null, capaUrl: null,
      especialidades: null, criadoEm: '2026-10-01T10:00:00', mediaNotas: null, quantidadeAvaliacoes: 0,
    });
    http.expectOne('/api/uploads/uso').flush({ usoBytes: 0, limiteBytes: 104857600, limitePorArquivoBytes: 5242880 });
    http.expectOne((request) => request.urlWithParams === '/api/perfis/12/anuncios?pagina=0&tamanho=6')
      .flush({ itens: [], pagina: 0, tamanho: 6, totalItens: 0, totalPaginas: 0 });
    http.expectOne((request) => request.urlWithParams === '/api/publicacoes?pagina=0&tamanho=12').flush({
      itens: [{
        id: 9, profissionalId: 12, profissionalNome: 'Ana Profissional', anuncioId: null,
        anuncioTitulo: null, legenda: 'Instalação concluída', fotos: ['/api/uploads/foto.png'],
        criadoEm: '2026-10-02T10:00:00',
        totalCurtidas: 0, curtidaPeloUsuario: false,
      }],
      pagina: 0, tamanho: 12, totalItens: 1, totalPaginas: 1,
    });
    harness.detectChanges();

    const fotoPortfolio = harness.routeNativeElement!.querySelector('.publicacoes-proprias .miniaturas img');
    expect(fotoPortfolio?.getAttribute('alt')).toBe('Foto 1 do trabalho de Ana Profissional');

    const publicacao = {
      id: 9, profissionalId: 12, profissionalNome: 'Ana Profissional', anuncioId: null,
      anuncioTitulo: null, legenda: 'Instalação concluída', fotos: ['/api/uploads/foto.png'],
      criadoEm: '2026-10-02T10:00:00',
        totalCurtidas: 0, curtidaPeloUsuario: false,
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
        totalCurtidas: 0, curtidaPeloUsuario: false,
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
      capaUrl: null,
      especialidades: null,
      criadoEm: '2026-10-01T10:00:00',
      mediaNotas: null,
      quantidadeAvaliacoes: 0,
    });
    http.expectOne('/api/uploads/uso').flush({ usoBytes: 0, limiteBytes: 104857600, limitePorArquivoBytes: 5242880 });
    http.expectOne((request) => request.url === '/api/perfis/12/anuncios').flush({
      itens: [], pagina: 0, tamanho: 6, totalItens: 0, totalPaginas: 0,
    });
    http.expectOne((request) => request.urlWithParams === '/api/publicacoes?pagina=0&tamanho=12').flush({
      itens: [], pagina: 0, tamanho: 12, totalItens: 0, totalPaginas: 0,
    });
    harness.detectChanges();

    const componente = harness.routeDebugElement!.componentInstance as unknown as {
      arquivoFoto: File | null;
      selecionarFoto(evento: Event): void;
      selecionarCapa(evento: Event): void;
      enviarFoto(): void;
      enviarCapa(): void;
      removerFoto(): void;
      removerCapa(): void;
      perfil(): { fotoUrl: string | null } | null;
    };
    const arquivo = new File(['imagem'], 'perfil.png', { type: 'image/png' });
    const campo = { files: [arquivo], value: 'perfil.png' } as unknown as HTMLInputElement;
    componente.selecionarFoto({ target: campo } as unknown as Event);
    expect(componente.arquivoFoto).toBe(arquivo);
    componente.enviarFoto();
    harness.detectChanges();

    expect(harness.routeNativeElement!.querySelector('.foto-editor [role="status"]')?.textContent)
      .toContain('Enviando foto profissional...');

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
      capaUrl: null,
      especialidades: null,
      criadoEm: '2026-10-01T10:00:00',
      mediaNotas: null,
      quantidadeAvaliacoes: 0,
    });
    http.expectOne('/api/uploads/uso').flush({ usoBytes: 1024, limiteBytes: 104857600, limitePorArquivoBytes: 5242880 });
    harness.detectChanges();

    expect(componente.perfil()?.fotoUrl).toBe('/api/uploads/0123456789abcdef0123456789abcdef.png');
    expect(harness.routeNativeElement?.textContent).toContain('Foto do perfil atualizada.');

    componente.removerFoto();
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector('.foto-editor [role="status"]')?.textContent)
      .toContain('Removendo foto profissional...');
    const removerFoto = http.expectOne('/api/perfil/foto');
    expect(removerFoto.request.method).toBe('DELETE');
    removerFoto.flush(null);
    harness.detectChanges();

    const arquivoCapa = new File(['capa'], 'capa.png', { type: 'image/png' });
    const campoCapa = { files: [arquivoCapa], value: 'capa.png' } as unknown as HTMLInputElement;
    componente.selecionarCapa({ target: campoCapa } as unknown as Event);
    componente.enviarCapa();
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector('.capa-editor [role="status"]')?.textContent)
      .toContain('Enviando capa profissional...');

    const enviarCapa = http.expectOne('/api/perfil/capa');
    expect(enviarCapa.request.method).toBe('PUT');
    enviarCapa.flush({
      id: 12, nome: 'Ana Profissional', tituloProfissional: null, bio: null, cidade: null,
      fotoUrl: null, capaUrl: '/api/uploads/capa.png', especialidades: null,
      criadoEm: '2026-10-01T10:00:00', mediaNotas: null, quantidadeAvaliacoes: 0,
    });
    http.expectOne('/api/uploads/uso').flush({ usoBytes: 2048, limiteBytes: 104857600, limitePorArquivoBytes: 5242880 });
    harness.detectChanges();

    componente.removerCapa();
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector('.capa-editor [role="status"]')?.textContent)
      .toContain('Removendo capa profissional...');
    const removerCapa = http.expectOne('/api/perfil/capa');
    expect(removerCapa.request.method).toBe('DELETE');
    removerCapa.flush(null);
  });

  it('retenta uma publicação sem reenviar as fotos já aceitas pelo servidor', async () => {
    TestBed.inject(SessaoService).entrar('ana@teste.invalid', 'senha').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'token-de-teste' });
    const harness = await RouterTestingHarness.create('/meu-perfil');
    http.expectOne('/api/perfil').flush({
      id: 12, nome: 'Ana Profissional', tituloProfissional: null, bio: null, cidade: null,
      fotoUrl: null, capaUrl: null, especialidades: null, criadoEm: '2026-10-01T10:00:00',
      mediaNotas: null, quantidadeAvaliacoes: 0,
    });
    http.expectOne('/api/uploads/uso').flush({ usoBytes: 0, limiteBytes: 104857600, limitePorArquivoBytes: 5242880 });
    http.expectOne((request) => request.url === '/api/perfis/12/anuncios').flush({
      itens: [], pagina: 0, tamanho: 6, totalItens: 0, totalPaginas: 0,
    });
    http.expectOne((request) => request.urlWithParams === '/api/publicacoes?pagina=0&tamanho=12').flush({
      itens: [], pagina: 0, tamanho: 12, totalItens: 0, totalPaginas: 0,
    });
    harness.detectChanges();

    const raiz = harness.routeNativeElement!;
    const entrada = raiz.querySelector<HTMLInputElement>('input[type="file"][multiple]')!;
    const primeiraFoto = new File(['primeira'], 'primeira.png', { type: 'image/png' });
    const segundaFoto = new File(['segunda'], 'segunda.jpg', { type: 'image/jpeg' });
    const arquivoLista = [primeiraFoto, segundaFoto];
    Object.defineProperty(entrada, 'files', { configurable: true, value: arquivoLista });
    entrada.dispatchEvent(new Event('change', { bubbles: true }));
    harness.detectChanges();
    expect(raiz.querySelectorAll('.previas-portfolio img')).toHaveSize(2);

    const publicar = () => Array.from(raiz.querySelectorAll<HTMLButtonElement>('.acoes-portfolio button'))
      .find((botao) => botao.textContent?.includes('Publicar no portfólio'))!;
    publicar().click();
    harness.detectChanges();
    expect(raiz.querySelector('.portfolio-editor [role="status"]')?.textContent)
      .toContain('Enviando fotos para o portfólio...');
    const primeiroEnvio = http.expectOne('/api/uploads');
    expect((primeiroEnvio.request.body as FormData).get('file')).toBe(primeiraFoto);
    primeiroEnvio.flush({ url: '/api/uploads/primeira.png' });
    const segundoEnvioFalho = http.expectOne('/api/uploads');
    expect((segundoEnvioFalho.request.body as FormData).get('file')).toBe(segundaFoto);
    segundoEnvioFalho.flush({}, { status: 503, statusText: 'Service Unavailable' });
    http.expectOne('/api/uploads/uso').flush({ usoBytes: 7, limiteBytes: 104857600, limitePorArquivoBytes: 5242880 });
    harness.detectChanges();
    expect(raiz.textContent).toContain('Não foi possível publicar as fotos');
    expect(raiz.querySelectorAll('.previas-portfolio img')).toHaveSize(2);

    publicar().click();
    const retentativa = http.expectOne('/api/uploads');
    expect((retentativa.request.body as FormData).get('file')).toBe(segundaFoto);
    retentativa.flush({ url: '/api/uploads/segunda.jpg' });
    const criar = http.expectOne('/api/publicacoes');
    expect(criar.request.method).toBe('POST');
    expect(criar.request.body).toEqual({
      legenda: '', fotos: ['/api/uploads/primeira.png', '/api/uploads/segunda.jpg'],
    });
    criar.flush({
      id: 20, profissionalId: 12, profissionalNome: 'Ana Profissional', anuncioId: null,
      anuncioTitulo: null, legenda: '', fotos: ['/api/uploads/primeira.png', '/api/uploads/segunda.jpg'],
      criadoEm: '2026-10-28T10:00:00',
      totalCurtidas: 0, curtidaPeloUsuario: false,
    });
    http.expectOne((request) => request.urlWithParams === '/api/publicacoes?pagina=0&tamanho=12').flush({
      itens: [{
        id: 20, profissionalId: 12, profissionalNome: 'Ana Profissional', anuncioId: null,
        anuncioTitulo: null, legenda: '', fotos: ['/api/uploads/primeira.png', '/api/uploads/segunda.jpg'],
        criadoEm: '2026-10-28T10:00:00',
        totalCurtidas: 0, curtidaPeloUsuario: false,
      }],
      pagina: 0, tamanho: 12, totalItens: 1, totalPaginas: 1,
    });
    http.expectOne('/api/uploads/uso').flush({ usoBytes: 14, limiteBytes: 104857600, limitePorArquivoBytes: 5242880 });
    await harness.fixture.whenStable();
    harness.detectChanges();

    expect(raiz.textContent).toContain('Publicação adicionada ao seu portfólio.');
    expect(raiz.querySelectorAll('.previas-portfolio img')).toHaveSize(0);
    expect(raiz.textContent).toContain('Suas publicações');
  });
});
