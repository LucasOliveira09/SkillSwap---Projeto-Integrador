import { DecimalPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { combineLatest, of, Subject } from 'rxjs';
import { catchError, map, startWith, switchMap } from 'rxjs/operators';
import { PerfilApiService } from '../../data-access/perfil-api.service';
import { PaginaPerfisProfissionais } from '../../models/perfil.model';

interface EstadoBusca {
  carregando: boolean;
  erro: boolean;
  pagina: PaginaPerfisProfissionais | null;
}

const estadoCarregando: EstadoBusca = { carregando: true, erro: false, pagina: null };

@Component({
  selector: 'app-busca-profissionais',
  standalone: true,
  imports: [FormsModule, RouterLink, DecimalPipe, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './busca-profissionais.component.html',
  styleUrl: './busca-profissionais.component.scss',
})
export class BuscaProfissionaisComponent {
  private readonly api = inject(PerfilApiService);
  private readonly rota = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly recarregar$ = new Subject<void>();

  protected busca = '';
  protected cidade = '';
  protected notaMinima = '';

  protected readonly estado = toSignal(
    combineLatest([
      this.rota.queryParamMap,
      this.recarregar$.pipe(startWith(undefined)),
    ]).pipe(
      switchMap(([parametros]) => {
        this.busca = parametros.get('busca') ?? '';
        this.cidade = parametros.get('cidade') ?? '';
        this.notaMinima = this.normalizarNotaMinima(parametros.get('notaMinima'));
        const pagina = this.lerPagina(parametros.get('pagina'));
        const nota = this.notaMinima ? Number(this.notaMinima) : undefined;
        return this.api.buscarProfissionais(this.busca, this.cidade, pagina, nota).pipe(
          map((resultado) => ({ carregando: false, erro: false, pagina: resultado })),
          catchError(() => of({ carregando: false, erro: true, pagina: null })),
          startWith(estadoCarregando),
        );
      }),
    ),
    { initialValue: estadoCarregando },
  );

  protected readonly paginaAtual = computed(() => this.estado().pagina?.pagina ?? 0);
  protected readonly totalPaginas = computed(() => this.estado().pagina?.totalPaginas ?? 0);

  protected pesquisar(): void {
    void this.router.navigate([], {
      relativeTo: this.rota,
      queryParams: {
        busca: this.busca.trim() || null,
        cidade: this.cidade.trim() || null,
        notaMinima: this.normalizarNotaMinima(this.notaMinima) || null,
        pagina: null,
      },
    });
  }

  protected mudarPagina(pagina: number): void {
    if (pagina < 0 || pagina >= this.totalPaginas() || this.estado().carregando) return;
    void this.router.navigate([], {
      relativeTo: this.rota,
      queryParams: { pagina },
      queryParamsHandling: 'merge',
    });
  }

  protected recarregar(): void {
    this.recarregar$.next();
  }

  protected inicial(nome: string): string {
    return nome.trim().charAt(0).toLocaleUpperCase('pt-BR');
  }

  private lerPagina(valor: string | null): number {
    const pagina = Number(valor ?? 0);
    return Number.isSafeInteger(pagina) && pagina >= 0 && pagina <= 1_000_000 ? pagina : 0;
  }

  private normalizarNotaMinima(valor: string | null): string {
    return ['4', '4.5', '5'].includes(valor ?? '') ? valor! : '';
  }
}
