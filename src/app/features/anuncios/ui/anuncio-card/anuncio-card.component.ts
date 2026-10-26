import { Component, input } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { IconeComponent } from '../../../../shared/ui/icone/icone.component';
import { CartaoAnuncio } from '../vitrine.model';

@Component({
  selector: 'app-anuncio-card',
  standalone: true,
  imports: [CurrencyPipe, MatCardModule, IconeComponent, RouterLink],
  templateUrl: './anuncio-card.component.html',
  styleUrl: './anuncio-card.component.scss',
})
export class AnuncioCardComponent {
  readonly anuncio = input.required<CartaoAnuncio>();
  protected estrelas(quantidade: number): string {
    return '★'.repeat(quantidade) + '☆'.repeat(5 - quantidade);
  }
}
