import { TestBed } from '@angular/core/testing';
import { RouterTestingHarness } from '@angular/router/testing';
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
    harness.detectChanges();

    const texto = harness.routeNativeElement?.textContent || '';
    expect(texto).toContain('Ana Profissional');
    expect(texto).toContain('Atendo residências em Florianópolis.');
    expect(texto).toContain('4,8');
    expect(texto).toContain('5 avaliações');
    expect(texto).toContain('Instalação de luminária');
    expect(texto).not.toContain('ana@');
  });

  it('trata perfil ausente sem inventar dados', async () => {
    const harness = await RouterTestingHarness.create('/profissionais/999');
    http.expectOne('/api/perfis/999').flush({}, { status: 404, statusText: 'Not Found' });
    harness.detectChanges();
    expect(harness.routeNativeElement?.textContent).toContain('Perfil não encontrado');
  });
});
