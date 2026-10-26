import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { finalize } from 'rxjs';
import { SessaoService } from '../data-access/sessao.service';
import { AnunciosApiService } from '../../anuncios/data-access/anuncios-api.service';

@Component({
  selector: 'app-conta',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    DatePipe,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './conta.component.html',
  styleUrl: './conta.component.scss',
})
export class ContaComponent {
  protected readonly sessao = inject(SessaoService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly api = inject(AnunciosApiService);

  protected readonly registrar = signal(false);
  protected nome = '';
  protected email = '';
  protected senha = '';
  protected enviando = false;
  protected erro = '';

  protected readonly propostasRecebidas = signal<readonly any[]>([]);
  protected readonly propostasEnviadas = signal<readonly any[]>([]);
  protected readonly carregandoPropostas = signal(false);

  constructor() {
    if (this.sessao.autenticado()) {
      this.carregarPropostas();
    }
  }

  protected carregarPropostas(): void {
    if (!this.sessao.autenticado()) return;
    this.carregandoPropostas.set(true);
    this.api
      .propostasRecebidas()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.carregandoPropostas.set(false)),
      )
      .subscribe({
        next: (propostas) => this.propostasRecebidas.set(propostas),
        error: () => {},
      });

    this.api
      .propostasEnviadas()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (propostas) => this.propostasEnviadas.set(propostas),
        error: () => {},
      });
  }

  protected aceitarProposta(id: number): void {
    this.api
      .aceitarProposta(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.carregarPropostas(),
      });
  }

  protected recusarProposta(id: number): void {
    this.api
      .recusarProposta(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.carregarPropostas(),
      });
  }
  protected enviar(form: NgForm): void {
    if (this.enviando || form.invalid) {
      form.control.markAllAsTouched();
      return;
    }
    this.enviando = true;
    this.erro = '';
    const pedido = this.registrar()
      ? this.sessao.registrar(this.nome, this.email, this.senha)
      : this.sessao.entrar(this.email, this.senha);
    pedido
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => (this.enviando = false)),
      )
      .subscribe({
        next: () => {
          this.senha = '';
          const retorno = this.route.snapshot.queryParamMap.get('retorno');
          void this.router.navigateByUrl(retorno === '/anuncios/novo' ? retorno : '/anuncios');
        },
        error: () => {
          this.senha = '';
          this.erro = 'Não foi possível entrar. Confira os dados ou tente novamente.';
        },
      });
  }
}
