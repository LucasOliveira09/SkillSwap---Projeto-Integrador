import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { SessaoService } from '../../../conta/data-access/sessao.service';
import { DenunciarComponent } from './denunciar.component';

describe('DenunciarComponent', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [DenunciarComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pede entrada antes de abrir o formulário para visitantes', () => {
    const fixture = TestBed.createComponent(DenunciarComponent);
    fixture.componentRef.setInput('tipoConteudo', 'PUBLICACAO');
    fixture.componentRef.setInput('recursoId', 42);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Entrar para denunciar');
    expect(http.match('/api/denuncias')).toHaveSize(0);
  });

  it('envia denúncia autenticada e confirma somente após resposta da API', () => {
    TestBed.inject(SessaoService).entrar('cliente@teste.invalid', 'senha').subscribe();
    http.expectOne('/api/sessoes').flush({ token: 'token-de-teste' });

    const fixture = TestBed.createComponent(DenunciarComponent);
    fixture.componentRef.setInput('tipoConteudo', 'AVALIACAO');
    fixture.componentRef.setInput('recursoId', 8);
    fixture.detectChanges();
    const instancia = fixture.componentInstance as unknown as {
      aberto: boolean;
      motivo: string;
      descricao: string;
      enviar(): void;
    };
    instancia.aberto = true;
    instancia.motivo = 'FRAUDE';
    instancia.descricao = 'Detalhes que ajudam a análise';
    instancia.enviar();

    const request = http.expectOne('/api/denuncias');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      tipoConteudo: 'AVALIACAO', recursoId: 8, motivo: 'FRAUDE', descricao: 'Detalhes que ajudam a análise',
    });
    request.flush({ id: 14, status: 'ABERTA' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Denúncia enviada para análise.');
  });
});
