import { Router } from '@angular/router';
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

  function responderInteracoes(anuncioId: number): void {
    http.expectOne(`/api/anuncios/${anuncioId}/comentarios?pagina=0&tamanho=30`).flush({
      itens: [], pagina: 0, tamanho: 30, totalItens: 0, totalPaginas: 0,
    });
    http.expectOne(`/api/anuncios/${anuncioId}/avaliacoes`).flush([]);
  }

  it('carrega anúncio e exibe galeria, preço e informações do anunciante', async () => {
    const harness = await RouterTestingHarness.create('/anuncios/1');
    expect(harness.routeNativeElement?.textContent).toContain('Carregando informações');

    http.expectOne('/api/anuncios/1').flush(mockAnuncio);
    harness.detectChanges();
    responderInteracoes(1);

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
    expect(titulos.map((titulo) => titulo.tagName)).toEqual(['H1', 'H2', 'H3', 'H2', 'H2', 'H2', 'H2']);
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

  it('substitui por estado sem foto quando a imagem da galeria falha', async () => {
    const harness = await RouterTestingHarness.create('/anuncios/1');
    http.expectOne('/api/anuncios/1').flush(mockAnuncio);
    harness.detectChanges();
    responderInteracoes(1);

    const imagem = harness.routeNativeElement?.querySelector('.imagem-destaque') as HTMLImageElement;
    imagem.dispatchEvent(new Event('error'));
    harness.detectChanges();

    expect(harness.routeNativeElement?.querySelector('.imagem-destaque')).toBeNull();
    expect(harness.routeNativeElement?.querySelector('.sem-imagem')?.textContent).toContain('indisponível');
    expect(harness.routeNativeElement?.querySelector('.miniaturas-barra')).toBeNull();
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
    responderInteracoes(2);

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

  it('publica comentário autenticado e o acrescenta à lista persistida', async () => {
    sessao.entrar('usuario@teste.invalid', '123456').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'mock-jwt-token' });
    const harness = await RouterTestingHarness.create('/anuncios/1');
    http.expectOne('/api/anuncios/1').flush(mockAnuncio);
    harness.detectChanges();
    responderInteracoes(1);

    const componente = harness.routeDebugElement!.componentInstance as {
      comentarioTexto: string;
      enviarComentario(): void;
    };
    componente.comentarioTexto = 'Atende durante a semana?';
    componente.enviarComentario();
    harness.detectChanges();

    const request = http.expectOne('/api/anuncios/1/comentarios');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ conteudo: 'Atende durante a semana?' });
    request.flush({ id: 7, anuncioId: 1, respostaAId: null, autorNome: 'Visitante',
      conteudo: 'Atende durante a semana?', criadoEm: '2026-10-30T13:00:00', podeResponder: false, podeDenunciar: true });
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('Atende durante a semana?');
    expect(componente.comentarioTexto).toBe('');
  });

  it('carrega a próxima página pública de comentários sem substituir a primeira', async () => {
    const harness = await RouterTestingHarness.create('/anuncios/1');
    http.expectOne('/api/anuncios/1').flush(mockAnuncio);
    harness.detectChanges();
    http.expectOne('/api/anuncios/1/comentarios?pagina=0&tamanho=30').flush({
      itens: [{ id: 1, anuncioId: 1, respostaAId: null, autorNome: 'Ana', conteudo: 'Primeira pergunta',
        criadoEm: '2026-10-29T10:00:00', podeResponder: false, podeDenunciar: false }],
      pagina: 0, tamanho: 30, totalItens: 31, totalPaginas: 2,
    });
    http.expectOne('/api/anuncios/1/avaliacoes').flush([]);
    harness.detectChanges();

    (harness.routeNativeElement?.querySelector('.interacoes-anuncio button') as HTMLButtonElement).click();
    harness.detectChanges();
    http.expectOne('/api/anuncios/1/comentarios?pagina=1&tamanho=30').flush({
      itens: [{ id: 31, anuncioId: 1, respostaAId: null, autorNome: 'Bruno', conteudo: 'Última pergunta',
        criadoEm: '2026-10-30T10:00:00', podeResponder: false, podeDenunciar: false }],
      pagina: 1, tamanho: 30, totalItens: 31, totalPaginas: 2,
    });
    harness.detectChanges();

    expect(harness.routeNativeElement?.textContent).toContain('Primeira pergunta');
    expect(harness.routeNativeElement?.textContent).toContain('Última pergunta');
  });

  it('abre uma conversa contextualizada quando o usuário autenticado entra em contato', async () => {
    // Autentica via login mockado
    sessao.entrar('usuario@teste.invalid', '123456').subscribe();
    const reqLogin = http.expectOne('/api/sessoes');
    reqLogin.flush({ token: 'mock-jwt-token' });

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/anuncios/1', DetalheAnuncioComponent);
    http.expectOne('/api/anuncios/1').flush(mockAnuncio);
    harness.detectChanges();
    responderInteracoes(1);

    const navegar = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const botao = harness.routeNativeElement?.querySelector('.mensagens-card button') as HTMLButtonElement;
    expect(botao.disabled).toBeFalse();
    botao.click();
    botao.click();
    harness.detectChanges();

    const reqMsg = http.expectOne('/api/conversas');
    expect(reqMsg.request.body).toEqual({ destinatarioId: 10, anuncioId: 1 });
    reqMsg.flush({ id: 50 });
    harness.detectChanges();

    expect(navegar).toHaveBeenCalledWith(['/mensagens'], { queryParams: { conversa: 'chat-50', anuncioId: 1 } });
  });
});
