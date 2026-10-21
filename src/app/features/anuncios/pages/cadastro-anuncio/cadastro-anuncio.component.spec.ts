import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CadastroAnuncioComponent } from './cadastro-anuncio.component';
import { AnunciosDemoService } from '../../data-access/anuncios-demo.service';

describe('CadastroAnuncioComponent', () => {
  let fixture: ComponentFixture<CadastroAnuncioComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CadastroAnuncioComponent],
      providers: [provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(CadastroAnuncioComponent);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('impede envio vazio, inclusive por submissão direta do formulário', () => {
    const button = fixture.nativeElement.querySelector(
      'button[type="submit"]',
    ) as HTMLButtonElement;
    expect(button.disabled).toBeTrue();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    expect(TestBed.inject(AnunciosDemoService).anuncios()).toEqual([]);
  });

  it('vincula campos, cria a simulação e limpa campos e mensagem', async () => {
    const preencher = async (name: string, value: string) => {
      const field = fixture.nativeElement.querySelector(`[name="${name}"]`) as HTMLInputElement;
      field.value = value;
      field.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      await fixture.whenStable();
    };

    await preencher('titulo', 'Aulas de violão');
    await preencher('descricao', 'Aulas para quem quer começar a tocar.');
    await preencher('cidade', 'Florianópolis');
    await preencher('preco', '90');
    fixture.detectChanges();
    await fixture.whenStable();
    const button = fixture.nativeElement.querySelector(
      'button[type="submit"]',
    ) as HTMLButtonElement;
    expect(button.disabled).toBeFalse();
    button.click();
    fixture.detectChanges();
    expect(TestBed.inject(AnunciosDemoService).anuncios().length).toBe(1);
    expect(fixture.nativeElement.querySelector('[role="status"]').textContent).toContain(
      'Simulação',
    );

    (fixture.nativeElement.querySelector('[data-testid="limpar"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect((fixture.nativeElement.querySelector('[name="titulo"]') as HTMLInputElement).value).toBe(
      '',
    );
    expect(fixture.nativeElement.querySelector('[role="status"]')).toBeNull();
    expect(button.disabled).toBeTrue();
  });
});
