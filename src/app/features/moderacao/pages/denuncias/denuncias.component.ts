import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { Denuncia, StatusDenuncia } from '../../models/moderacao.model';
import { ModeracaoApiService } from '../../data-access/moderacao-api.service';

@Component({
  selector: 'app-denuncias',
  standalone: true,
  imports: [DatePipe, MatButtonModule, RouterLink],
  templateUrl: './denuncias.component.html',
  styleUrl: './denuncias.component.scss',
})
export class DenunciasComponent {
  private readonly api = inject(ModeracaoApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly denuncias = signal<readonly Denuncia[]>([]);
  protected carregando = true;
  protected podeModerar = false;
  protected erro = '';
  protected status: StatusDenuncia = 'ABERTA';
  protected processandoId: number | null = null;
  protected readonly pagina = signal(0);
  protected readonly totalPaginas = signal(0);

  constructor() {
    this.api.acesso().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (acesso) => {
        this.podeModerar = acesso;
        if (acesso) this.carregar();
        else this.carregando = false;
      },
      error: () => {
        this.podeModerar = false;
        this.carregando = false;
        this.erro = 'Não foi possível verificar o acesso à moderação.';
      },
    });
  }

  protected carregar(): void {
    this.carregando = true;
    this.erro = '';
    this.api.listar(this.status, this.pagina()).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.carregando = false),
    ).subscribe({
      next: (resultado) => {
        this.denuncias.set(resultado.itens);
        this.pagina.set(resultado.pagina);
        this.totalPaginas.set(resultado.totalPaginas);
      },
      error: () => this.erro = 'Não foi possível carregar as denúncias.',
    });
  }

  protected filtrar(status: StatusDenuncia): void {
    this.status = status;
    this.pagina.set(0);
    this.carregar();
  }

  protected mudarPagina(pagina: number): void {
    if (this.carregando || pagina < 0 || pagina >= this.totalPaginas()) return;
    this.pagina.set(pagina);
    this.carregar();
  }

  protected decidir(denuncia: Denuncia, acao: 'OCULTAR' | 'DESCARTAR' | 'REEXIBIR'): void {
    if (this.processandoId !== null) return;
    this.processandoId = denuncia.id;
    this.api.decidir(denuncia.id, acao).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.processandoId = null),
    ).subscribe({
      next: () => this.carregar(),
      error: () => this.erro = 'Não foi possível concluir a análise. Tente novamente.',
    });
  }
}
