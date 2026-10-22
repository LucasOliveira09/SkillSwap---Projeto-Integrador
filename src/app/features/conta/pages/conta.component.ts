import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { finalize } from 'rxjs';
import { SessaoService } from '../data-access/sessao.service';

@Component({
  selector: 'app-conta',
  standalone: true,
  imports: [FormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './conta.component.html',
  styleUrl: './conta.component.scss',
})
export class ContaComponent {
  protected readonly sessao = inject(SessaoService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly registrar = signal(false);
  protected nome = '';
  protected email = '';
  protected senha = '';
  protected enviando = false;
  protected erro = '';
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
