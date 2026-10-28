import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
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
import { NotificacoesApiService } from '../data-access/notificacoes-api.service';
import { Notificacao } from '../models/notificacao.model';

const ROTA_DETALHE_ANUNCIO = /^\/anuncios\/[A-Za-z0-9_-]+$/;
const ROTA_EDICAO_ANUNCIO = /^\/anuncios\/\d+\/editar$/;
const ROTA_DETALHE_ACORDO = /^\/acordos\/\d+$/;

export function destinoPosLogin(destino: string | null): string {
  if (
    destino === '/anuncios/novo' ||
    destino === '/anuncios/minhas' ||
    destino === '/meu-perfil' ||
    (destino && ROTA_EDICAO_ANUNCIO.test(destino)) ||
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
  private readonly notificacoesApi = inject(NotificacoesApiService);

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
  protected readonly acaoPropostaEmAndamento = signal<{
    id: number;
    status: 'ACEITA' | 'RECUSADA' | 'CANCELADA';
  } | null>(null);
  protected readonly erroAcaoProposta = signal('');
  protected readonly erroAcordos = signal('');
  protected readonly notificacoes = signal<readonly Notificacao[]>([]);
  protected readonly carregandoNotificacoes = signal(false);
  protected readonly erroNotificacoes = signal('');
  protected readonly erroLeituraNotificacao = signal('');
  protected readonly naoLidas = computed(() => this.notificacoes().filter((notificacao) => !notificacao.lida).length);

  constructor() {
    if (this.sessao.autenticado()) {
      this.carregarPropostas();
      this.carregarNotificacoes();
    }
  }

  protected carregarNotificacoes(): void {
    if (!this.sessao.autenticado()) return;
    this.carregandoNotificacoes.set(true);
    this.erroNotificacoes.set('');
    this.notificacoesApi.listar().pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.carregandoNotificacoes.set(false)),
    ).subscribe({
      next: (pagina) => this.notificacoes.set(pagina.itens),
      error: () => this.erroNotificacoes.set('Não foi possível carregar as notificações. Tente novamente.'),
    });
  }

  protected abrirNotificacao(event: MouseEvent, notificacao: Notificacao): void {
    if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    if (notificacao.lida) {
      void this.router.navigateByUrl(notificacao.recursoUrl);
      return;
    }
    this.erroLeituraNotificacao.set('');
    this.notificacoesApi.atualizarLeitura(notificacao.id, true).pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: (atualizada) => this.notificacoes.update((itens) =>
        itens.map((item) => item.id === atualizada.id ? atualizada : item)),
      error: () => {
        this.erroLeituraNotificacao.set('Não foi possível atualizar esta notificação.');
      },
      complete: () => void this.router.navigateByUrl(notificacao.recursoUrl),
    });
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
    this.atualizarStatusProposta(id, 'ACEITA');
  }

  protected recusarProposta(id: number): void {
    this.atualizarStatusProposta(id, 'RECUSADA');
  }

  private atualizarStatusProposta(id: number, status: 'ACEITA' | 'RECUSADA' | 'CANCELADA'): void {
    if (this.acaoPropostaEmAndamento()) return;
    this.erroAcaoProposta.set('');
    this.acaoPropostaEmAndamento.set({ id, status });
    this.api
      .atualizarProposta(id, status)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.acaoPropostaEmAndamento.set(null)),
      )
      .subscribe({
        next: () => this.carregarPropostas(),
        error: () => {
          const verbo = status === 'ACEITA' ? 'aceitar' : status === 'RECUSADA' ? 'recusar' : 'cancelar';
          this.erroAcaoProposta.set(
            `Não foi possível ${verbo} esta proposta. Atualize a lista e tente novamente.`,
          );
        },
      });
  }

  protected cancelarProposta(id: number): void {
    this.atualizarStatusProposta(id, 'CANCELADA');
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
