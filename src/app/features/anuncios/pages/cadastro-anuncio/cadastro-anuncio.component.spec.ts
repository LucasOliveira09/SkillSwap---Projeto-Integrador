import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CadastroAnuncioComponent } from './cadastro-anuncio.component';
import { signal } from '@angular/core';
import { SessaoService } from '../../../conta/data-access/sessao.service';

describe('Cadastro persistente', () => {
  let fixture: ComponentFixture<CadastroAnuncioComponent>;
  let http: HttpTestingController;
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CadastroAnuncioComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SessaoService, useValue: { autenticado: signal(true) } },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CadastroAnuncioComponent);
    fixture.detectChanges();
    http.expectOne('/api/categorias').flush([{ valor: 'aulas', rotulo: 'Aulas e instrução' }]);
    fixture.detectChanges();
    await fixture.whenStable();
  });
  afterEach(() => http.verify());

  async function preencher() {
    for (const [nome, valor] of Object.entries({
      titulo: 'Aulas de violão',
      descricao: 'Música para iniciantes',
      cidade: 'Florianópolis',
      preco: '90',
    })) {
      const campo = fixture.nativeElement.querySelector(`[name="${nome}"]`) as HTMLInputElement;
      campo.value = valor;
      campo.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      await fixture.whenStable();
    }
  }

  it('impede envio vazio sem acionar a API', () => {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    http.expectNone('/api/anuncios');
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBeTrue();
  });

  it('confirma somente após resposta da API e impede envio duplicado enquanto salva', async () => {
    await preencher();
    const botao = fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement;
    botao.click();
    fixture.detectChanges();
    expect(botao.disabled).toBeTrue();
    expect(fixture.nativeElement.querySelector('[role="status"]')).toBeNull();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    const req = http.expectOne('/api/anuncios');
    req.flush({ ...req.request.body, id: 17, exemplo: false });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="status"]').textContent).toContain('salvo');
    fixture.nativeElement.querySelector('[data-testid="limpar"]').click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('[name="titulo"]').value).toBe('');
    expect(fixture.nativeElement.querySelector('[role="status"]')).toBeNull();
  });

  it('mantém os campos e não confirma sucesso quando o backend falha', async () => {
    await preencher();
    fixture.nativeElement.querySelector('button[type="submit"]').click();
    http.expectOne('/api/anuncios').flush({}, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('salvar');
    expect(fixture.nativeElement.querySelector('[name="titulo"]').value).toBe('Aulas de violão');
    expect(fixture.nativeElement.querySelector('[role="status"]')).toBeNull();
  });
});
