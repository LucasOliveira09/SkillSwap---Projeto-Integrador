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

  it('expõe as informações e as modalidades como grupos nomeados', () => {
    const pagina = fixture.nativeElement as HTMLElement;
    const informacoes = pagina.querySelector('.confianca');
    const modalidades = pagina.querySelector('.modos');

    expect(informacoes?.getAttribute('role')).toBe('group');
    expect(informacoes?.getAttribute('aria-label')).toBe('Informações do cadastro');
    expect(modalidades?.getAttribute('role')).toBe('group');
    expect(modalidades?.getAttribute('aria-label')).toBe('Modalidade do anúncio');
  });

  it('dá nome acessível distinto a cada período de cada dia', () => {
    const nomes = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLInputElement>(
        '.grade-disponibilidade input[type="checkbox"]',
      ),
      (campo) => campo.getAttribute('aria-label'),
    );

    expect(nomes).toContain('Segunda-feira, Manhã');
    expect(nomes).toContain('Domingo, Noite');
    expect(new Set(nomes).size).toBe(21);
  });

  it('anuncia o envio de uma foto do anúncio enquanto o servidor processa o upload', () => {
    const entrada = fixture.nativeElement.querySelector('#input-foto') as HTMLInputElement;
    const arquivo = new File(['imagem'], 'servico.png', { type: 'image/png' });
    Object.defineProperty(entrada, 'files', { configurable: true, value: [arquivo] });
    entrada.dispatchEvent(new Event('change', { bubbles: true }));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.upload-bloco [role="status"]')?.textContent)
      .toContain('Enviando foto do anúncio...');

    http.expectOne('/api/uploads').flush({ url: '/api/uploads/servico.png', nomeArquivo: 'servico.png' });
  });

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

  it('salva a disponibilidade semanal declarada junto do anúncio', async () => {
    await preencher();
    const segundaManha = fixture.nativeElement.querySelector('.grade-disponibilidade input') as HTMLInputElement;
    segundaManha.checked = true;
    segundaManha.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    fixture.nativeElement.querySelector('button[type="submit"]').click();
    const req = http.expectOne('/api/anuncios');
    expect(req.request.body.disponibilidades).toEqual([{ diaSemana: 'SEGUNDA', periodo: 'MANHA' }]);
    req.flush({ ...req.request.body, id: 18, exemplo: false });
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
