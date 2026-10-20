import { Component, input } from '@angular/core';

const CAMINHOS = {
  menu: 'M4 6h16M4 12h16M4 18h16',
  busca: 'M21 21l-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
  usuario: 'M20 21v-2a7 7 0 0 0-14 0v2M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
  carrinho: 'M2 3h3l3 13h11l3-9H6M9 21h.01M18 21h.01',
  esquerda: 'M15 5l-7 7 7 7',
  direita: 'M9 5l7 7-7 7',
  fechar: 'M6 6l12 12M6 18L18 6',
  troca: 'M3 7h17l-4-4M21 17H4l4 4',
  relogio: 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0M12 6v6l4 2',
  pessoas:
    'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0M19 8a3 3 0 0 1 0 6M22 21v-2a4 4 0 0 0-3-3.8',
  local: 'M20 10c0 6-8 12-8 12S4 16 4 10a8 8 0 1 1 16 0M15 10a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
  estrela: 'M12 3l2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-3-5.6 3 1.1-6.2L3 9.6l6.2-.9z',
  correto: 'M20 6L9 17l-5-5',
  email: 'M3 5h18v14H3zM3 5l9 8 9-8',
  ajuda: 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0M9.1 9a3 3 0 0 1 5.8 1c0 2-3 2-3 4M12 18h.01',
} as const;

@Component({
  selector: 'app-icone',
  standalone: true,
  templateUrl: './icone.component.html',
  styleUrl: './icone.component.scss',
})
export class IconeComponent {
  readonly nome = input.required<keyof typeof CAMINHOS>();
  protected readonly caminhos = CAMINHOS;
}
