import { Component, signal } from '@angular/core';
import { IconeComponent } from '../../../../shared/ui/icone/icone.component';

@Component({
  selector: 'app-banner-destaques',
  standalone: true,
  imports: [IconeComponent],
  templateUrl: './banner-destaques.component.html',
  styleUrl: './banner-destaques.component.scss',
})
export class BannerDestaquesComponent {
  protected readonly atual = signal(0);
  protected readonly destaques = [
    {
      chamada: 'Sua habilidade vale muito\ntroque, colabore, cresça',
      titulo: 'SKILL\nSWAP',
      descricao: 'Aqui, cada talento tem valor\ntroque e aproveite.',
    },
    {
      chamada: 'O que você faz bem\npode abrir novas portas',
      titulo: 'SEU\nTALENTO',
      descricao: 'Divulgue serviços e aulas.\nConecte-se com novas possibilidades.',
    },
    {
      chamada: 'Aprenda algo novo\ncompartilhe o que sabe',
      titulo: 'TROQUE\nIDEIAS',
      descricao: 'Habilidades que se encontram.\nPessoas que crescem juntas.',
    },
  ];
  protected mover(direcao: number): void {
    this.atual.update((valor) => (valor + direcao + this.destaques.length) % this.destaques.length);
  }
}
