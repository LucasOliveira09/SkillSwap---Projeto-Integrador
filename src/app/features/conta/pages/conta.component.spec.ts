import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RouterTestingHarness } from '@angular/router/testing';
import { appConfig } from '../../../app.config';
import { SessaoService } from '../data-access/sessao.service';
import { destinoPosLogin } from './conta.component';

describe('ações sobre propostas', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [...appConfig.providers, provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('informa falha ao aceitar e impede ações repetidas enquanto a resposta está pendente', async () => {
    const harness = await abrirContaComProposta(http);
    const aceitar = harness.routeNativeElement!.querySelector<HTMLButtonElement>('.proposta-acoes button')!;
    const recusar = harness.routeNativeElement!.querySelectorAll<HTMLButtonElement>('.proposta-acoes button')[1];

    aceitar.click();
    harness.detectChanges();

    const pedido = http.expectOne('/api/propostas/5');
    expect(pedido.request.method).toBe('PATCH');
    expect(pedido.request.body).toEqual({ status: 'ACEITA' });
    expect(aceitar.disabled).toBeTrue();
    expect(recusar.disabled).toBeTrue();
    aceitar.click();
    recusar.click();
    pedido.flush({ mensagem: 'Falha temporária' }, { status: 500, statusText: 'Server Error' });

    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector('[role="alert"]')?.textContent)
      .toContain('Não foi possível aceitar esta proposta. Atualize a lista e tente novamente.');
    expect(aceitar.disabled).toBeFalse();
    http.expectNone('/api/propostas/5');
  });

  it('informa falha ao cancelar e bloqueia cliques repetidos na proposta enviada', async () => {
    const harness = await abrirContaComProposta(http, 'ENVIADAS');
    const cancelar = harness.routeNativeElement!.querySelector<HTMLButtonElement>('.proposta-card button')!;

    cancelar.click();
    harness.detectChanges();

    const pedido = http.expectOne('/api/propostas/6');
    expect(pedido.request.method).toBe('PATCH');
    expect(pedido.request.body).toEqual({ status: 'CANCELADA' });
    expect(cancelar.disabled).toBeTrue();
    expect(cancelar.textContent).toContain('Cancelando...');
    cancelar.click();
    pedido.flush({ mensagem: 'Falha temporária' }, { status: 503, statusText: 'Service Unavailable' });

    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector('[role="alert"]')?.textContent)
      .toContain('Não foi possível cancelar esta proposta. Atualize a lista e tente novamente.');
    expect(cancelar.disabled).toBeFalse();
    http.expectNone('/api/propostas/6');
  });
});

async function abrirContaComProposta(
  http: HttpTestingController,
  direcao: 'RECEBIDAS' | 'ENVIADAS' = 'RECEBIDAS',
): Promise<RouterTestingHarness> {
  TestBed.inject(SessaoService).entrar('anunciante@teste.invalid', 'senha').subscribe();
  http.expectOne('/api/sessoes').flush({ token: 'token-teste' });

  const harness = await RouterTestingHarness.create('/conta');
  const recebidas = http.expectOne((request) =>
    request.url === '/api/propostas' && request.params.get('direcao') === 'RECEBIDAS',
  );
  recebidas.flush({
    itens: direcao === 'RECEBIDAS' ? [{
      id: 5,
      anuncioId: 3,
      anuncioTitulo: 'Aula de violão',
      proponenteId: 2,
      proponenteNome: 'Cliente',
      mensagemInicial: 'Tenho interesse no anúncio.',
      status: 'PENDENTE',
      criadoEm: '2026-10-28T10:00:00',
    }] : [],
    pagina: 0,
    tamanho: 20,
    totalItens: 1,
    totalPaginas: 1,
  });
  http.expectOne((request) =>
    request.url === '/api/propostas' && request.params.get('direcao') === 'ENVIADAS',
  ).flush({
    itens: direcao === 'ENVIADAS' ? [{
      id: 6,
      anuncioId: 3,
      anuncioTitulo: 'Aula de violão',
      proponenteId: 1,
      proponenteNome: 'Anunciante',
      mensagemInicial: 'Tenho interesse neste anúncio.',
      status: 'PENDENTE',
      criadoEm: '2026-10-28T10:00:00',
    }] : [],
    pagina: 0,
    tamanho: 20,
    totalItens: direcao === 'ENVIADAS' ? 1 : 0,
    totalPaginas: direcao === 'ENVIADAS' ? 1 : 0,
  });
  http.expectOne('/api/acordos?pagina=0&tamanho=20')
    .flush({ itens: [], pagina: 0, tamanho: 20, totalItens: 0, totalPaginas: 0 });
  http.expectOne('/api/notificacoes?pagina=0&tamanho=10')
    .flush({ itens: [], pagina: 0, tamanho: 10, totalItens: 0, totalPaginas: 0 });
  harness.detectChanges();
  return harness;
}

describe('destino após login', () => {
  it('retorna ao anúncio solicitado', () => {
    expect(destinoPosLogin('/anuncios/42')).toBe('/anuncios/42');
  });

  it('retorna ao anúncio identificado por código legado', () => {
    expect(destinoPosLogin('/anuncios/exemplo-eletricista')).toBe(
      '/anuncios/exemplo-eletricista',
    );
  });

  it('mantém o retorno ao cadastro de anúncio', () => {
    expect(destinoPosLogin('/anuncios/novo')).toBe('/anuncios/novo');
  });

  it('permite voltar à gestão ou à edição de anúncio depois do login', () => {
    expect(destinoPosLogin('/anuncios/minhas')).toBe('/anuncios/minhas');
    expect(destinoPosLogin('/anuncios/42/editar')).toBe('/anuncios/42/editar');
  });

  it('retorna ao editor de perfil profissional', () => {
    expect(destinoPosLogin('/meu-perfil')).toBe('/meu-perfil');
  });

  it('retorna à conversa interna de um acordo', () => {
    expect(destinoPosLogin('/acordos/15')).toBe('/acordos/15');
  });

  it('usa o catálogo quando o destino não é uma rota interna permitida', () => {
    for (const destino of [null, '', '//externo.example', '/conta', '/anuncios/42?x=1']) {
      expect(destinoPosLogin(destino)).toBe('/anuncios');
    }
  });
});
