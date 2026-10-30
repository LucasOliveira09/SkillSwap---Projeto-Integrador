import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { CadastroAnuncioComponent } from './cadastro-anuncio.component';
import { SessaoService } from '../../../conta/data-access/sessao.service';
import { signal } from '@angular/core';

@Component({ standalone: true, template: '' })
class PaginaDeGestaoTesteComponent {}

describe('Edição de anúncio', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CadastroAnuncioComponent],
      providers: [
        provideRouter([{ path: 'anuncios/minhas', component: PaginaDeGestaoTesteComponent }]),
        provideHttpClient(), provideHttpClientTesting(),
        { provide: SessaoService, useValue: { autenticado: signal(true) } },
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: '42' })) } },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('carrega anúncio próprio e persiste a edição com PUT', async () => {
    const fixture = TestBed.createComponent(CadastroAnuncioComponent);
    fixture.detectChanges();
    http.expectOne('/api/categorias').flush([{ valor: 'aulas', rotulo: 'Aulas e instrução' }]);
    http.expectOne('/api/anuncios/meus/42').flush({
      id: 42, titulo: 'Aula de piano', descricao: 'Para iniciantes', categoria: 'aulas',
      cidade: 'Florianópolis', modalidade: 'venda', preco: 80, habilidadeDesejada: '', imagem: null,
    });
    fixture.detectChanges();
    await fixture.whenStable();

    const titulo = fixture.nativeElement.querySelector('[name="titulo"]') as HTMLInputElement;
    expect(titulo.value).toBe('Aula de piano');
    titulo.value = 'Aula de piano para iniciantes';
    titulo.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('button[type="submit"]') as HTMLButtonElement).click();

    const req = http.expectOne('/api/anuncios/42');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.titulo).toBe('Aula de piano para iniciantes');
    req.flush({
      id: 42, titulo: req.request.body.titulo, categoria: 'AULAS', modalidade: 'VENDA',
      preco: 80, status: 'ATIVO',
    });
  });
});
