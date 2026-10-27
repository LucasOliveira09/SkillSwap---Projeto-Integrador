import { TestBed } from '@angular/core/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { Router } from '@angular/router';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { appConfig } from '../../../../app.config';
import { PerfilPublicoComponent } from './perfil-publico.component';

describe('PerfilPublicoComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [...appConfig.providers, provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('mostra dados públicos, reputação com amostra e ofertas ativas', async () => {
    const harness = await RouterTestingHarness.create('/profissionais/12');
    expect(harness.routeNativeElement?.textContent).toContain('Carregando perfil profissional');

    http.expectOne('/api/perfis/12').flush({
      id: 12,
      nome: 'Ana Profissional',
      bio: 'Atendo residências em Florianópolis.',
      cidade: 'Florianópolis',
      fotoUrl: null,
      especialidades: 'Elétrica residencial',
      criadoEm: '2026-10-01T10:00:00',
      mediaNotas: 4.8,
      quantidadeAvaliacoes: 5,
    });
    const ofertas = http.expectOne((request) => request.url === '/api/perfis/12/anuncios');
    expect(ofertas.request.params.get('pagina')).toBe('0');
    expect(ofertas.request.params.get('tamanho')).toBe('6');
    ofertas.flush({
      itens: [
        {
          id: 81,
          titulo: 'Instalação de luminária',
          descricao: 'Instalação residencial',
          categoria: 'MANUTENCAO',
          cidade: 'Florianópolis',
          modalidade: 'VENDA',
          preco: 120,
          habilidadeDesejada: '',
          status: 'ATIVO',
          usuarioId: 12,
          usuarioNome: 'Ana Profissional',
          criadoEm: '2026-10-01T10:00:00',
          categoriaRotulo: 'Manutenção',
          imagem: null,
          unidade: '/visita',
          autor: 'Ana Profissional',
          avatar: null,
          estrelas: null,
          exemplo: false,
        },
      ],
      pagina: 0,
      tamanho: 6,
      totalItens: 1,
      totalPaginas: 1,
    });
    const publicacoes = http.expectOne((request) => request.url === '/api/perfis/12/publicacoes');
    expect(publicacoes.request.params.get('tamanho')).toBe('12');
    publicacoes.flush({
      itens: [{
        id: 4,
        profissionalId: 12,
        profissionalNome: 'Ana Profissional',
        anuncioId: null,
        anuncioTitulo: null,
        legenda: 'Instalação concluída',
        fotos: ['/api/uploads/foto.png'],
        criadoEm: '2026-10-02T10:00:00',
      }],
      pagina: 0,
      tamanho: 12,
      totalItens: 1,
      totalPaginas: 1,
    });
    http.expectOne('/api/usuarios/12/avaliacoes').flush([{
      id: 9,
      acordoId: 2,
      avaliadorId: 18,
      avaliadorNome: 'Bruno Cliente',
      avaliadoId: 12,
      avaliadoNome: 'Ana Profissional',
      nota: 5,
      notaAnuncio: 4,
      depoimento: 'Serviço pontual e bem executado.',
      criadoEm: '2026-10-03T10:00:00',
      respostaProfissional: 'Agradeço a confiança.',
      respondidaEm: '2026-10-04T10:00:00',
    }]);
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent || '';
    expect(texto).toContain('Ana Profissional');
    expect(texto).toContain('Atendo residências em Florianópolis.');
    expect(texto).toContain('4,8');
    expect(texto).toContain('5 avaliações');
    expect(texto).toContain('Instalação de luminária');
    expect(texto).toContain('Trabalhos e portfólio');
    expect(texto).toContain('Instalação concluída');
    expect(texto).toContain('Avaliações após trabalhos concluídos');
    expect(texto).toContain('Bruno Cliente');
    expect(texto).toContain('Serviço pontual e bem executado.');
    expect(texto).toContain('Agradeço a confiança.');
    expect(texto).toContain('Serviço:');
    expect(texto).not.toContain('ana@');
  });

  it('trata perfil ausente sem inventar dados', async () => {
    const harness = await RouterTestingHarness.create('/profissionais/999');
    http.expectOne('/api/perfis/999').flush({}, { status: 404, statusText: 'Not Found' });
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Perfil não encontrado');
  });

  it('navega pelas páginas do portfólio usando a página solicitada pela API', async () => {
    const harness = await RouterTestingHarness.create('/profissionais/12');
    const perfil = {
      id: 12, nome: 'Ana Profissional', bio: null, cidade: 'Florianópolis', fotoUrl: null,
      especialidades: 'Eletricista', criadoEm: '2026-10-01T10:00:00', mediaNotas: null,
      quantidadeAvaliacoes: 0,
    };
    http.expectOne('/api/perfis/12').flush(perfil);
    http.expectOne((request) => request.url === '/api/perfis/12/anuncios').flush({
      itens: [], pagina: 0, tamanho: 6, totalItens: 0, totalPaginas: 0,
    });
    const primeira = http.expectOne((request) => request.url === '/api/perfis/12/publicacoes');
    expect(primeira.request.params.get('pagina')).toBe('0');
    primeira.flush({
      itens: [{ id: 1, profissionalId: 12, profissionalNome: 'Ana Profissional', anuncioId: null,
        anuncioTitulo: null, legenda: 'Primeira página', fotos: ['/api/uploads/1.png'], criadoEm: '2026-10-02T10:00:00' }],
      pagina: 0, tamanho: 12, totalItens: 13, totalPaginas: 2,
    });
    http.expectOne('/api/usuarios/12/avaliacoes').flush([]);
    harness.detectChanges();

    Array.from(harness.routeNativeElement!.querySelectorAll('button'))
      .find((button) => button.textContent?.includes('Próximas'))?.click();
    await harness.fixture.whenStable();
    const perfilAtualizado = http.expectOne('/api/perfis/12');
    perfilAtualizado.flush(perfil);
    http.expectOne((request) => request.url === '/api/perfis/12/anuncios').flush({
      itens: [], pagina: 0, tamanho: 6, totalItens: 0, totalPaginas: 0,
    });
    const segunda = http.expectOne((request) => request.url === '/api/perfis/12/publicacoes');
    expect(segunda.request.params.get('pagina')).toBe('1');
    segunda.flush({
      itens: [{ id: 13, profissionalId: 12, profissionalNome: 'Ana Profissional', anuncioId: null,
        anuncioTitulo: null, legenda: 'Décima terceira publicação', fotos: ['/api/uploads/13.png'], criadoEm: '2026-10-14T10:00:00' }],
      pagina: 1, tamanho: 12, totalItens: 13, totalPaginas: 2,
    });
    http.expectOne('/api/usuarios/12/avaliacoes').flush([]);
    await harness.fixture.whenStable();
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Décima terceira publicação');
    expect(harness.routeNativeElement?.textContent).toContain('13 publicações');
    expect(TestBed.inject(Router).url).toContain('portfolioPagina=1');
  });

  it('pagina todas as ofertas do profissional e mostra a quantidade total', async () => {
    const harness = await RouterTestingHarness.create('/profissionais/12');
    const perfil = {
      id: 12, nome: 'Ana Profissional', bio: null, cidade: 'Florianópolis', fotoUrl: null,
      especialidades: 'Eletricista', criadoEm: '2026-10-01T10:00:00', mediaNotas: null,
      quantidadeAvaliacoes: 0,
    };
    const oferta = {
      titulo: 'Manutenção residencial', descricao: 'Atendimento local', categoria: 'MANUTENCAO',
      categoriaRotulo: 'Manutenção', cidade: 'Florianópolis', modalidade: 'VENDA', preco: 80,
      habilidadeDesejada: '', status: 'ATIVO', usuarioId: 12, criadoEm: '2026-10-01T10:00:00',
      imagem: null, unidade: '/visita', autor: 'Ana Profissional', avatar: null, estrelas: null, exemplo: false,
    };
    http.expectOne('/api/perfis/12').flush(perfil);
    const primeira = http.expectOne((request) => request.url === '/api/perfis/12/anuncios');
    expect(primeira.request.params.get('pagina')).toBe('0');
    primeira.flush({ itens: Array.from({ length: 6 }, (_, i) => ({ ...oferta, id: 81 + i })), pagina: 0, tamanho: 6, totalItens: 7, totalPaginas: 2 });
    http.expectOne((request) => request.url === '/api/perfis/12/publicacoes').flush({
      itens: [], pagina: 0, tamanho: 12, totalItens: 0, totalPaginas: 0,
    });
    http.expectOne('/api/usuarios/12/avaliacoes').flush([]);
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('7 ofertas');

    const proxima = Array.from(harness.routeNativeElement!.querySelectorAll('.paginacao-ofertas button'))
      .find((button) => button.textContent?.includes('Próximas')) as HTMLButtonElement;
    proxima.click();
    await harness.fixture.whenStable();
    http.expectOne('/api/perfis/12').flush(perfil);
    const segunda = http.expectOne((request) => request.url === '/api/perfis/12/anuncios');
    expect(segunda.request.params.get('pagina')).toBe('1');
    segunda.flush({ itens: [{ ...oferta, id: 87 }], pagina: 1, tamanho: 6, totalItens: 7, totalPaginas: 2 });
    http.expectOne((request) => request.url === '/api/perfis/12/publicacoes').flush({
      itens: [], pagina: 0, tamanho: 12, totalItens: 0, totalPaginas: 0,
    });
    http.expectOne('/api/usuarios/12/avaliacoes').flush([]);
    await harness.fixture.whenStable();
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Página 2 de 2');
    expect(harness.routeNativeElement?.textContent).toContain('Manutenção residencial');
    expect(TestBed.inject(Router).url).toContain('ofertasPagina=1');
  });
});
