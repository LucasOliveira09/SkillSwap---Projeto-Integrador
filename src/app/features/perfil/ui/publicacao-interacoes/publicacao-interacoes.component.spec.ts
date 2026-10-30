import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { appConfig } from '../../../../app.config';
import { SessaoService } from '../../../conta/data-access/sessao.service';
import { PublicacaoInteracoesComponent } from './publicacao-interacoes.component';

describe('PublicacaoInteracoesComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [...appConfig.providers, provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('carrega comentários públicos apenas quando o painel é aberto', () => {
    const fixture = TestBed.createComponent(PublicacaoInteracoesComponent);
    fixture.componentRef.setInput('publicacao', {
      id: 42, profissionalId: 12, profissionalNome: 'Ana', anuncioId: null, anuncioTitulo: null,
      legenda: '', fotos: [], criadoEm: '2026-10-30T09:00:00', totalCurtidas: 0, curtidaPeloUsuario: false,
    });
    fixture.detectChanges();
    expect(http.match('/api/publicacoes/42/comentarios')).toHaveSize(0);

    (fixture.nativeElement.querySelector('.alternar-comentarios') as HTMLButtonElement).click();
    const leitura = http.expectOne('/api/publicacoes/42/comentarios?pagina=0&tamanho=10');
    expect(leitura.request.method).toBe('GET');
    leitura.flush({ itens: [], pagina: 0, tamanho: 10, totalItens: 0, totalPaginas: 0 });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Ainda não há comentários');
  });

  it('publica uma resposta autorizada na raiz carregada após confirmar sessão', () => {
    TestBed.inject(SessaoService).entrar('cliente@teste.invalid', 'senha').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'token-de-teste' });

    const fixture = TestBed.createComponent(PublicacaoInteracoesComponent);
    fixture.componentRef.setInput('publicacao', {
      id: 42, profissionalId: 12, profissionalNome: 'Ana', anuncioId: null, anuncioTitulo: null,
      legenda: '', fotos: [], criadoEm: '2026-10-30T09:00:00', totalCurtidas: 1, curtidaPeloUsuario: true,
    });
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.alternar-comentarios') as HTMLButtonElement).click();
    http.expectOne('/api/publicacoes/42/comentarios?pagina=0&tamanho=10').flush({
      itens: [{ id: 7, publicacaoId: 42, respostaAId: null, autorNome: 'Profissional', conteudo: 'Como faço orçamento?',
        criadoEm: '2026-10-30T09:00:00', respostas: [], podeResponder: true, podeDenunciar: true }],
      pagina: 0, tamanho: 10, totalItens: 1, totalPaginas: 1,
    });
    fixture.detectChanges();

    (fixture.nativeElement.querySelector('.acoes-comentario button') as HTMLButtonElement).click();
    fixture.detectChanges();
    const componente = fixture.componentInstance as unknown as { texto: string };
    componente.texto = 'Consigo enviar pelo chat do anúncio.';
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    const envio = http.expectOne('/api/publicacoes/42/comentarios');
    expect(envio.request.method).toBe('POST');
    expect(envio.request.body).toEqual({ conteudo: 'Consigo enviar pelo chat do anúncio.', respostaAId: 7 });
    envio.flush({ id: 8, publicacaoId: 42, respostaAId: 7, autorNome: 'Cliente',
      conteudo: 'Consigo enviar pelo chat do anúncio.', criadoEm: '2026-10-30T10:00:00',
      respostas: [], podeResponder: false, podeDenunciar: false });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Consigo enviar pelo chat do anúncio.');
  });
});
