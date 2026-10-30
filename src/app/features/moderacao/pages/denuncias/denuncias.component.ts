import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { Denuncia, HistoricoAvaliacao, StatusDenuncia } from '../../models/moderacao.model';
import { AcaoContestacaoAvaliacao, ContestacaoAvaliacao, StatusContestacaoAvaliacao } from '../../../avaliacao/models/contestacao.model';
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
  protected readonly contestacoes = signal<readonly ContestacaoAvaliacao[]>([]);
  protected readonly carregandoContestacoes = signal(true);
  protected readonly erroContestacoes = signal('');
  protected readonly paginaContestacoes = signal(0);
  protected readonly totalPaginasContestacoes = signal(0);
  protected statusContestacoes: StatusContestacaoAvaliacao = 'ABERTA';
  protected readonly processandoContestacaoId = signal<number | null>(null);
  protected decisoesContestacoes: Record<number, string> = {};
  protected readonly historicosAvaliacao = signal<Readonly<Record<number, HistoricoAvaliacao | null>>>({});
  protected readonly carregandoHistoricoId = signal<number | null>(null);

  constructor() {
    this.api.acesso().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (acesso) => {
        this.podeModerar = acesso;
        if (acesso) {
          this.carregar();
          this.carregarContestacoes();
        }
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

  protected carregarContestacoes(): void {
    this.carregandoContestacoes.set(true);
    this.erroContestacoes.set('');
    this.api.listarContestacoes(this.statusContestacoes, this.paginaContestacoes()).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.carregandoContestacoes.set(false)),
    ).subscribe({
      next: (resultado) => {
        this.contestacoes.set(resultado.itens);
        this.paginaContestacoes.set(resultado.pagina);
        this.totalPaginasContestacoes.set(resultado.totalPaginas);
      },
      error: () => this.erroContestacoes.set('Não foi possível carregar as contestações.'),
    });
  }

  protected filtrarContestacoes(status: StatusContestacaoAvaliacao): void {
    this.statusContestacoes = status;
    this.paginaContestacoes.set(0);
    this.carregarContestacoes();
  }

  protected mudarPaginaContestacoes(pagina: number): void {
    if (this.carregandoContestacoes() || pagina < 0 || pagina >= this.totalPaginasContestacoes()) return;
    this.paginaContestacoes.set(pagina);
    this.carregarContestacoes();
  }

  protected atualizarDecisaoContestacao(id: number, decisao: string): void {
    this.decisoesContestacoes = { ...this.decisoesContestacoes, [id]: decisao };
  }

  protected decidirContestacao(contestacao: ContestacaoAvaliacao, acao: AcaoContestacaoAvaliacao): void {
    const decisao = (this.decisoesContestacoes[contestacao.id] ?? '').trim();
    if (decisao.length < 20 || decisao.length > 1000 || this.processandoContestacaoId() !== null) {
      this.erroContestacoes.set('A decisão deve explicar o motivo em pelo menos 20 caracteres.');
      return;
    }
    this.processandoContestacaoId.set(contestacao.id);
    this.erroContestacoes.set('');
    this.api.decidirContestacao(contestacao.id, acao, decisao).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.processandoContestacaoId.set(null)),
    ).subscribe({
      next: () => this.carregarContestacoes(),
      error: () => this.erroContestacoes.set('Não foi possível salvar a decisão. Atualize a fila e tente novamente.'),
    });
  }

  protected carregarHistoricoAvaliacao(avaliacaoId: number): void {
    if (this.carregandoHistoricoId() !== null || this.historicosAvaliacao()[avaliacaoId]) return;
    this.carregandoHistoricoId.set(avaliacaoId);
    this.api.historicoAvaliacao(avaliacaoId).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.carregandoHistoricoId.set(null)),
    ).subscribe({
      next: (historico) => this.historicosAvaliacao.update((atuais) => ({ ...atuais, [avaliacaoId]: historico })),
      error: () => this.historicosAvaliacao.update((atuais) => ({ ...atuais, [avaliacaoId]: null })),
    });
  }
}
