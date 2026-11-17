import { SharedModule } from '../../../../shared/shared.module';
import { TestBed } from '@angular/core/testing';
import { BannerDestaquesComponent } from './banner-destaques.component';

describe('BannerDestaquesComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [BannerDestaquesComponent], imports: [SharedModule],
    }).compileComponents();
  });

  it('oferece alvos de 24 por 24 pixels nos controles dos destaques', () => {
    const fixture = TestBed.createComponent(BannerDestaquesComponent);
    fixture.detectChanges();

    const indicadores = fixture.nativeElement.querySelectorAll(
      '.indicadores button',
    ) as NodeListOf<HTMLButtonElement>;

    expect(indicadores.length).toBe(3);
    for (const indicador of indicadores) {
      const estilo = getComputedStyle(indicador);
      expect(estilo.width).toBe('24px');
      expect(estilo.height).toBe('24px');
    }
  });
});
