import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { AnunciosApiService } from '../../data-access/anuncios-api.service';
import { AnuncioApi, PaginaAnuncios } from '../../models/anuncio-api.model';
import { SessaoService } from '../../../conta/data-access/sessao.service';

@Component({
  selector: 'app-minhas-anuncios',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, MatButtonModule],
  templateUrl: './minhas-anuncios.component.html',
  styleUrl: './minhas-anuncios.component.scss',
})
export class MinhasAnunciosComponent {
  private readonly api = inject(AnunciosApiService);
  private readonly sessao = inject(SessaoService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly pagina = signal<PaginaAnuncios | null>(null);
  protected readonly carregando = signal(true);
  protected readonly erro = signal('');
  protected readonly sucesso = signal('');
  protected readonly alterandoId = signal<number | null>(null);

  constructor() {
    if (!this.sessao.autenticado()) {
      void this.router.navigate(['/conta'], { queryParams: { retorno: '/anuncios/minhas' } });
      return;
    }
    this.carregar();
  }

  protected carregar(numero = 0): void {
    this.carregando.set(true);
    this.erro.set('');
    this.api.listarMeus(numero).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.carregando.set(false)),
    ).subscribe({
      next: (resultado) => this.pagina.set(resultado),
      error: () => this.erro.set('Não foi possível carregar seus anúncios. Tente novamente.'),
    });
  }

  protected alternarStatus(anuncio: AnuncioApi): void {
    if (this.alterandoId() !== null) return;
    const status = anuncio.status === 'ATIVO' ? 'INATIVO' : 'ATIVO';
    this.alterandoId.set(anuncio.id);
    this.erro.set('');
    this.sucesso.set('');
    this.api.atualizarStatus(anuncio.id, status).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.alterandoId.set(null)),
    ).subscribe({
      next: (atualizado) => {
        this.pagina.update((pagina) => pagina ? {
          ...pagina,
          itens: pagina.itens.map((item) => item.id === atualizado.id ? atualizado : item),
        } : pagina);
        this.sucesso.set(status === 'ATIVO' ? 'Anúncio reativado.' : 'Anúncio pausado.');
      },
      error: () => this.erro.set('Não foi possível alterar o anúncio. Tente novamente.'),
    });
  }

  protected mudarPagina(pagina: number): void {
    const atual = this.pagina();
    if (!atual || pagina < 0 || pagina >= atual.totalPaginas || this.carregando()) return;
    this.carregar(pagina);
  }
}
