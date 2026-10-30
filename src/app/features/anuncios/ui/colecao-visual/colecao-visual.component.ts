import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconeComponent } from '../../../../shared/ui/icone/icone.component';
import { ItemColecao } from '../vitrine.model';

@Component({
  selector: 'app-colecao-visual',
  standalone: true,
  imports: [RouterLink, IconeComponent],
  templateUrl: './colecao-visual.component.html',
  styleUrl: './colecao-visual.component.scss',
})
export class ColecaoVisualComponent {
  readonly titulo = input.required<string>();
  readonly itens = input.required<readonly ItemColecao[]>();
  readonly formato = input<'circulo' | 'banner' | 'servico'>('circulo');
  protected rolar(linha: HTMLElement, direcao: number): void {
    linha.scrollBy({
      left: direcao * linha.clientWidth * 0.8,
      behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches
        ? 'instant'
        : 'smooth',
    });
  }
}
