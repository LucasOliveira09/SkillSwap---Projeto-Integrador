import { registerLocaleData } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import localePt from '@angular/common/locales/pt';
import { provideRouter } from '@angular/router';
import { AnuncioCardComponent } from './anuncio-card.component';
import { CartaoAnuncio } from '../vitrine.model';

registerLocaleData(localePt);

describe('AnuncioCardComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnuncioCardComponent],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('inclui no nome acessível os rótulos visíveis no link da imagem', () => {
    const fixture = TestBed.createComponent(AnuncioCardComponent);
    const anuncio: CartaoAnuncio = {
      id: '1',
      titulo: 'Aula de violão',
      categoria: 'Aulas e instrução',
      filtros: [],
      preco: 80,
      unidade: 'hora',
      trocaPor: '',
      detalhes: [],
      autor: 'Professora Ana',
      autorId: 7,
      estrelas: null,
      quando: 'Hoje',
      cidade: 'Florianópolis / SC',
      exemplo: false,
    };
    fixture.componentRef.setInput('anuncio', anuncio);
    fixture.detectChanges();

    const link = fixture.nativeElement.querySelector('.foto-link') as HTMLAnchorElement;
    expect(fixture.nativeElement.querySelector('.avatar-link')?.getAttribute('href')).toBe('/profissionais/7');
    expect(fixture.nativeElement.querySelector('.avatar-link')?.getAttribute('aria-label')).toBe('Ver perfil de Professora Ana');
    expect(link.hasAttribute('aria-label')).toBeFalse();
    expect(link.textContent).toContain(anuncio.categoria);
    expect(link.textContent).toContain('Seu anúncio');
    expect(link.querySelector('.sr-only')?.textContent?.trim()).toBe('Ver detalhes de Aula de violão');

    fixture.componentRef.setInput('anuncio', {
      ...anuncio,
      categoria: 'Esportes',
      exemplo: true,
      imagem: 'data:image/gif;base64,R0lGODlhAQABAAD/ACwAAAAAAQABAAACADs=',
    });
    fixture.detectChanges();

    expect(link.hasAttribute('aria-label')).toBeFalse();
    expect(fixture.nativeElement.querySelector('.avatar-link')).toBeNull();
    expect(link.querySelector('.sr-only')).toBeNull();
    expect(link.querySelector('img')?.alt).toBe(anuncio.titulo);
    expect(link.textContent).toContain('Esportes');
    expect(link.textContent).toContain('Exemplo');
  });

  it('mostra um estado sem foto quando a imagem do anúncio não carrega', () => {
    const fixture = TestBed.createComponent(AnuncioCardComponent);
    fixture.componentRef.setInput('anuncio', {
      id: '1', titulo: 'Aula de violão', categoria: 'Aulas e instrução', filtros: [],
      preco: 80, unidade: 'hora', trocaPor: '', detalhes: [], autor: 'Ana', autorId: 7,
      estrelas: null, quando: 'Hoje', cidade: 'Florianópolis / SC', exemplo: false,
      imagem: '/api/uploads/foto-ausente.png',
    });
    fixture.detectChanges();

    const imagem = fixture.nativeElement.querySelector('.foto img') as HTMLImageElement;
    imagem.dispatchEvent(new Event('error'));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.foto img')).toBeNull();
    expect(fixture.nativeElement.querySelector('.sem-foto')?.textContent).toContain('Imagem indisponível');
  });
});
