import { TestBed } from '@angular/core/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { appConfig } from '../../../../app.config';
import { SessaoService } from '../../../conta/data-access/sessao.service';
import { DetalheAnuncioComponent } from './detalhe-anuncio.component';

describe('Detalhe do Anúncio (estilo Mercado Livre)', () => {
  let http: HttpTestingController;
  let sessao: SessaoService;

  const mockAnuncio = {
    id: 1,
    titulo: 'Aula de Violão Avançada',
    descricao: 'Técnicas de dedilhado e harmonia.\nPresencial ou online.',
    categoria: 'aulas',
    categoriaRotulo: 'Aulas e Cursos',
    cidade: 'Ourinhos / SP',
    modalidade: 'venda' as const,
    preco: 120,
    unidade: '/hora',
    habilidadeDesejada: '',
    status: 'ATIVO',
    usuarioId: 10,
    autor: 'Professor Carlos',
    avatar: 'images/legado/avatar-lucas.jpg',
    imagem: 'images/legado/aula-futebol.jpg',
    estrelas: 5,
    mediaAvaliacoesAnuncio: 4.7,
    quantidadeAvaliacoesAnuncio: 2,
    exemplo: false,
    criadoEm: '2026-10-26T12:00:00',
    disponibilidades: [{ diaSemana: 'SABADO' as const, periodo: 'TARDE' as const }],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [...appConfig.providers, provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    sessao = TestBed.inject(SessaoService);
  });

  afterEach(() => http.verify());

  it('carrega anúncio e exibe galeria, preço e informações do anunciante', async () => {
    const harness = await RouterTestingHarness.create('/anuncios/1');
    expect(harness.routeNativeElement?.textContent).toContain('Carregando informações');

    http.expectOne('/api/anuncios/1').flush(mockAnuncio);
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent || '';
    expect(texto).toContain('Aula de Violão Avançada');
    expect(texto).toContain('Professor Carlos');
    expect(texto).toContain('Ourinhos / SP');
    expect(texto).toContain('120,00');
    expect(texto).toContain('Disponibilidade semanal informada');
    expect(texto).toContain('Sábado · Tarde');
    expect(texto).toContain('confirme os horários na conversa');

    const titulos = Array.from(
      harness.routeNativeElement?.querySelectorAll('h1, h2, h3') ?? [],
    );
    expect(titulos[0]?.tagName).toBe('H1');
    expect(titulos[0]?.textContent?.trim()).toBe('Aula de Violão Avançada');
    expect(titulos.map((titulo) => titulo.tagName)).toEqual(['H1', 'H2', 'H3', 'H2', 'H2']);
    expect(harness.routeNativeElement?.querySelectorAll('h1').length).toBe(1);

    const reputacao = harness.routeNativeElement?.querySelector('.reputacao-anuncio');
    expect(reputacao?.hasAttribute('aria-label')).toBeFalse();
    expect(reputacao?.textContent).toContain('4,7');
    expect(reputacao?.textContent).toContain('2 avaliações do serviço');
    expect(reputacao?.querySelector('.sr-only')?.textContent).toContain('trabalhos concluídos');

    const img = harness.routeNativeElement?.querySelector('.imagem-destaque') as HTMLImageElement;
    expect(img).toBeTruthy();
    expect(img.src).toContain('aula-futebol.jpg');
  });

  it('exibe modalidade de troca e habilidade desejada quando for troca', async () => {
    const harness = await RouterTestingHarness.create('/anuncios/2');
    http.expectOne('/api/anuncios/2').flush({
      ...mockAnuncio,
      id: 2,
      modalidade: 'troca',
      preco: null,
      habilidadeDesejada: 'Aulas de Francês',
    });
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent || '';
    expect(texto).toContain('Disponível para troca');
    expect(texto).toContain('Aulas de Francês');
  });

  it('exibe estado de erro caso anúncio não seja encontrado', async () => {
    const harness = await RouterTestingHarness.create('/anuncios/999');
    http.expectOne('/api/anuncios/999').flush({}, { status: 404, statusText: 'Not Found' });
    harness.detectChanges();

    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toContain(
      'Anúncio não encontrado',
    );
  });

  it('permite enviar mensagem quando usuário estiver autenticado', async () => {
    // Autentica via login mockado
    sessao.entrar('usuario@teste.invalid', '123456').subscribe();
    const reqLogin = http.expectOne('/api/sessoes');
    reqLogin.flush({ token: 'mock-jwt-token' });

    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl('/anuncios/1', DetalheAnuncioComponent);
    http.expectOne('/api/anuncios/1').flush(mockAnuncio);
    harness.detectChanges();

    component.mensagemTexto = 'Olá, gostaria de saber se atende aos sábados!';
    harness.detectChanges();

    const botao = harness.routeNativeElement?.querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(botao.disabled).toBeFalse();
    botao.click();
    harness.detectChanges();

    const reqMsg = http.expectOne('/api/anuncios/1/propostas');
    expect(reqMsg.request.body.mensagemInicial).toBe('Olá, gostaria de saber se atende aos sábados!');
    reqMsg.flush({ id: 50, mensagemInicial: 'Olá, gostaria de saber se atende aos sábados!', status: 'PENDENTE' });
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('Mensagem e proposta enviadas com sucesso');
  });
});
