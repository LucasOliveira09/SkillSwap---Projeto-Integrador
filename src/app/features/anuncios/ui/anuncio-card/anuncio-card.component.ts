import { Component, computed, input, signal } from '@angular/core';
import { CartaoAnuncio } from '../vitrine.model';

@Component({
  standalone: false,
  selector: 'app-anuncio-card',
  templateUrl: './anuncio-card.component.html',
  styleUrl: './anuncio-card.component.scss',
})
export class AnuncioCardComponent {
  readonly anuncio = input.required<CartaoAnuncio>();
  private readonly imagemQueFalhou = signal<string | null>(null);
  protected readonly imagemDisponivel = computed(() => {
    const imagem = this.anuncio().imagem;
    return imagem && imagem !== this.imagemQueFalhou() ? imagem : null;
  });

  protected marcarImagemIndisponivel(): void {
    this.imagemQueFalhou.set(this.imagemDisponivel());
  }

  protected estrelas(quantidade: number): string {
    return '★'.repeat(quantidade) + '☆'.repeat(5 - quantidade);
  }
}
