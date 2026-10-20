import { Component, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AnunciosDemoService } from '../../data-access/anuncios-demo.service';
import { CategoriaAnuncio, CATEGORIAS_ANUNCIO } from '../../models/anuncio.model';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CurrencyPipe, RouterLink, MatButtonModule, MatCardModule],
  templateUrl: './catalogo.component.html',
  styleUrl: './catalogo.component.scss',
})
export class CatalogoComponent {
  protected readonly anuncios = inject(AnunciosDemoService).anuncios;

  protected categoria(valor: CategoriaAnuncio): string {
    return CATEGORIAS_ANUNCIO.find((categoria) => categoria.valor === valor)!.rotulo;
  }
}
