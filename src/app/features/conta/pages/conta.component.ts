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
import { NegociacaoApiService } from '../../negociacao/data-access/negociacao-api.service';
import { Acordo, Proposta } from '../../negociacao/models/negociacao.model';

const ROTA_DETALHE_ANUNCIO = /^\/anuncios\/[A-Za-z0-9_-]+$/;
const ROTA_DETALHE_ACORDO = /^\/acordos\/\d+$/;

export function destinoPosLogin(destino: string | null): string {
  if (
    destino === '/anuncios/novo' ||
    destino === '/meu-perfil' ||
    (destino && ROTA_DETALHE_ANUNCIO.test(destino)) ||
    (destino && ROTA_DETALHE_ACORDO.test(destino))
  ) {
    return destino;
  }
  return '/anuncios';
}

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
  private readonly api = inject(NegociacaoApiService);

  protected readonly registrar = signal(false);
  protected nome = '';
  protected email = '';
  protected senha = '';
  protected enviando = false;
  protected erro = '';

  protected readonly propostasRecebidas = signal<readonly Proposta[]>([]);
  protected readonly propostasEnviadas = signal<readonly Proposta[]>([]);
  protected readonly acordos = signal<readonly Acordo[]>([]);
  protected readonly carregandoPropostas = signal(false);
  protected readonly erroPropostas = signal('');
  protected readonly erroAcordos = signal('');

  constructor() {
    if (this.sessao.autenticado()) {
      this.carregarPropostas();
    }
  }

  protected carregarPropostas(): void {
    if (!this.sessao.autenticado()) return;
    this.erroPropostas.set('');
    this.erroAcordos.set('');
    this.carregandoPropostas.set(true);
    this.api
      .propostas('RECEBIDAS')
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.carregandoPropostas.set(false)),
      )
      .subscribe({
        next: (pagina) => this.propostasRecebidas.set(pagina.itens),
        error: () => this.erroPropostas.set('Não foi possível carregar suas propostas. Tente novamente.'),
      });

    this.api
      .propostas('ENVIADAS')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (pagina) => this.propostasEnviadas.set(pagina.itens),
        error: () => this.erroPropostas.set('Não foi possível carregar suas propostas. Tente novamente.'),
      });

    this.api.acordos().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (pagina) => this.acordos.set(pagina.itens),
      error: () => this.erroAcordos.set('Não foi possível carregar seus acordos. Tente novamente.'),
    });
  }

  protected aceitarProposta(id: number): void {
    this.api
      .atualizarProposta(id, 'ACEITA')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.carregarPropostas(),
      });
  }

  protected recusarProposta(id: number): void {
    this.api
      .atualizarProposta(id, 'RECUSADA')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.carregarPropostas(),
      });
  }

  protected cancelarProposta(id: number): void {
    this.api
      .atualizarProposta(id, 'CANCELADA')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.carregarPropostas(),
        error: () => this.erroPropostas.set('Não foi possível cancelar esta proposta. Atualize a lista e tente novamente.'),
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
          void this.router.navigateByUrl(destinoPosLogin(retorno));
        },
        error: () => {
          this.senha = '';
          this.erro = 'Não foi possível entrar. Confira os dados ou tente novamente.';
        },
      });
  }
}
