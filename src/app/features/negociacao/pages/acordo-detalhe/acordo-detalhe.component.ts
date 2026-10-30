import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, finalize, switchMap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ContestacaoAvaliacao } from '../../../avaliacao/models/contestacao.model';
import { SessaoService } from '../../../conta/data-access/sessao.service';
import { PerfilApiService } from '../../../perfil/data-access/perfil-api.service';
import { PerfilProfissional } from '../../../perfil/models/perfil.model';
import { NegociacaoApiService } from '../../data-access/negociacao-api.service';
import { Acordo, Avaliacao, EventoAcordo, Mensagem, TipoEventoAcordo } from '../../models/negociacao.model';

@Component({
  selector: 'app-acordo-detalhe',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './acordo-detalhe.component.html',
  styleUrl: './acordo-detalhe.component.scss',
})
export class AcordoDetalheComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly sessao = inject(SessaoService);
  private readonly perfilApi = inject(PerfilApiService);
  private readonly api = inject(NegociacaoApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly carregando = signal(true);
  protected readonly enviando = signal(false);
  protected readonly confirmandoConclusao = signal(false);
  protected readonly confirmandoCancelamento = signal(false);
  protected readonly erro = signal('');
  protected readonly sucesso = signal('');
  protected readonly acordo = signal<Acordo | null>(null);
  protected readonly perfil = signal<PerfilProfissional | null>(null);
  protected readonly mensagens = signal<readonly Mensagem[]>([]);
  protected readonly carregandoMensagens = signal(false);
  protected readonly paginaMensagens = signal(0);
  protected readonly totalPaginasMensagens = signal(0);
  protected readonly avaliacoes = signal<readonly Avaliacao[]>([]);
  protected readonly contestacoes = signal<Readonly<Record<number, ContestacaoAvaliacao | undefined>>>({});
  protected readonly eventos = signal<readonly EventoAcordo[]>([]);
  protected readonly mensagem = signal('');
  protected respostas: Record<number, string> = {};
  protected readonly respondendoAvaliacao = signal<number | null>(null);
  protected readonly editandoAvaliacao = signal<number | null>(null);
  protected readonly salvandoEdicaoAvaliacao = signal<number | null>(null);
  protected notaEdicao = 0;
  protected notaAnuncioEdicao = 0;
  protected depoimentoEdicao = '';
  protected readonly contestandoAvaliacao = signal<number | null>(null);
  protected justificativas: Record<number, string> = {};
  protected nota = 0;
  protected notaAnuncio = 0;
  protected depoimento = '';
  private acordoId = 0;

  protected readonly interlocutor = computed(() => {
    const acordo = this.acordo();
    const perfil = this.perfil();
    if (!acordo || !perfil) return '';
    return acordo.anuncianteId === perfil.id ? acordo.proponenteNome : acordo.anuncianteNome;
  });

  protected readonly minhaConclusaoConfirmada = computed(() => {
    const acordo = this.acordo();
    const perfil = this.perfil();
    if (!acordo || !perfil) return false;
    return acordo.anuncianteId === perfil.id
      ? acordo.conclusaoConfirmadaAnunciante
      : acordo.conclusaoConfirmadaProponente;
  });

  protected readonly outraConclusaoConfirmada = computed(() => {
    const acordo = this.acordo();
    const perfil = this.perfil();
    if (!acordo || !perfil) return false;
    return acordo.anuncianteId === perfil.id
      ? acordo.conclusaoConfirmadaProponente
      : acordo.conclusaoConfirmadaAnunciante;
  });

  protected readonly jaAvaliou = computed(() => {
    const perfil = this.perfil();
    return perfil ? this.avaliacoes().some((avaliacao) => avaliacao.avaliadorId === perfil.id) : false;
  });

  protected readonly podeAvaliar = computed(() => {
    const acordo = this.acordo();
    const perfil = this.perfil();
    return Boolean(acordo && perfil && acordo.proponenteId === perfil.id);
  });

  constructor() {
    if (!this.sessao.autenticado()) {
      const id = this.route.snapshot.paramMap.get('id');
      void this.router.navigate(['/conta'], { queryParams: { retorno: `/acordos/${id ?? ''}` } });
      return;
    }
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isSafeInteger(id) || id <= 0) {
      this.erro.set('Acordo não encontrado.');
      this.carregando.set(false);
      return;
    }
    this.acordoId = id;
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erro.set('');
    forkJoin({
      acordo: this.api.acordo(this.acordoId),
      mensagens: this.api.mensagens(this.acordoId, 0),
      avaliacoes: this.api.avaliacoes(this.acordoId),
      eventos: this.api.eventos(this.acordoId),
      perfil: this.perfilApi.meuPerfil(),
    }).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.carregando.set(false)),
    ).subscribe({
      next: (resposta) => {
        this.acordo.set(resposta.acordo);
        this.aplicarPaginaMensagens(resposta.mensagens);
        this.avaliacoes.set(resposta.avaliacoes);
        this.eventos.set(resposta.eventos);
        this.perfil.set(resposta.perfil);
        this.carregarContestacoesDoProfissional(resposta.avaliacoes, resposta.perfil.id);
        if (resposta.mensagens.totalPaginas > 1) {
          this.carregarMensagens(resposta.mensagens.totalPaginas - 1);
        }
      },
      error: () => this.erro.set('Não foi possível carregar este acordo. Confira se você participa dele e tente novamente.'),
    });
  }

  protected descricaoEvento(tipo: TipoEventoAcordo): string {
    switch (tipo) {
      case 'ACORDO_INICIADO': return 'Acordo iniciado após a proposta ser aceita.';
      case 'CONCLUSAO_CONFIRMADA': return 'Confirmou que o trabalho foi concluído.';
      case 'ACORDO_CONCLUIDO': return 'As duas partes confirmaram a conclusão.';
      case 'ACORDO_CANCELADO': return 'Acordo cancelado.';
    }
  }

  protected carregarMensagens(pagina: number): void {
    if (pagina < 0 || pagina >= this.totalPaginasMensagens() || this.carregandoMensagens()) return;
    this.carregandoMensagens.set(true);
    this.erro.set('');
    this.api.mensagens(this.acordoId, pagina).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.carregandoMensagens.set(false)),
    ).subscribe({
      next: (resposta) => this.aplicarPaginaMensagens(resposta),
      error: () => this.erro.set('Não foi possível carregar esta página da conversa. Tente novamente.'),
    });
  }

  private carregarMensagensMaisRecentes(): void {
    this.carregandoMensagens.set(true);
    this.api.mensagens(this.acordoId, 0).pipe(
      switchMap((primeiraPagina) => this.api.mensagens(
        this.acordoId,
        Math.max(0, primeiraPagina.totalPaginas - 1),
      )),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.carregandoMensagens.set(false)),
    ).subscribe({
      next: (resposta) => {
        this.aplicarPaginaMensagens(resposta);
        this.sucesso.set('Mensagem enviada.');
      },
      error: () => this.erro.set('A mensagem foi enviada, mas não foi possível atualizar a conversa.'),
    });
  }

  private aplicarPaginaMensagens(resposta: { itens: readonly Mensagem[]; pagina: number; totalPaginas: number }): void {
    this.mensagens.set(resposta.itens);
    this.paginaMensagens.set(resposta.pagina);
    this.totalPaginasMensagens.set(resposta.totalPaginas);
  }

  protected enviarMensagem(): void {
    const conteudo = this.mensagem().trim();
    if (!conteudo || conteudo.length > 2000 || this.enviando()) return;
    this.enviando.set(true);
    this.erro.set('');
    this.sucesso.set('');
    this.api.enviarMensagem(this.acordoId, conteudo).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.enviando.set(false)),
    ).subscribe({
      next: (mensagem) => {
        this.mensagem.set('');
        if (this.totalPaginasMensagens() === 0) {
          this.mensagens.set([mensagem]);
        }
        this.carregarMensagensMaisRecentes();
      },
      error: () => this.erro.set('Não foi possível enviar a mensagem. Seu texto continua disponível para nova tentativa.'),
    });
  }

  protected concluirAcordo(): void {
    if (this.enviando() || this.acordo()?.status !== 'EM_ANDAMENTO') return;
    this.enviando.set(true);
    this.erro.set('');
    this.api.atualizarAcordo(this.acordoId, 'CONCLUIDO').pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.enviando.set(false)),
    ).subscribe({
      next: (acordo) => {
        this.acordo.set(acordo);
        this.confirmandoConclusao.set(false);
        this.sucesso.set(acordo.status === 'CONCLUIDO'
          ? 'A conclusão foi confirmada pelas duas pessoas. Você já pode avaliar sua experiência.'
          : 'Sua confirmação foi registrada. O acordo será concluído quando a outra pessoa também confirmar.');
      },
      error: () => this.erro.set('Não foi possível concluir o acordo. Atualize a página e tente novamente.'),
    });
  }

  protected cancelarAcordo(): void {
    if (this.enviando() || this.acordo()?.status !== 'EM_ANDAMENTO') return;
    this.enviando.set(true);
    this.erro.set('');
    this.sucesso.set('');
    this.api.atualizarAcordo(this.acordoId, 'CANCELADO').pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.enviando.set(false)),
    ).subscribe({
      next: () => {
        this.confirmandoCancelamento.set(false);
        this.sucesso.set('Acordo cancelado.');
        this.carregar();
      },
      error: () => this.erro.set('Não foi possível cancelar o acordo. Atualize os dados e tente novamente.'),
    });
  }

  protected avaliar(): void {
    if (!this.nota || !this.notaAnuncio || this.enviando() || this.acordo()?.status !== 'CONCLUIDO' || !this.podeAvaliar() || this.jaAvaliou()) return;
    this.enviando.set(true);
    this.erro.set('');
    this.sucesso.set('');
    this.api.avaliar(this.acordoId, this.nota, this.notaAnuncio, this.depoimento.trim()).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.enviando.set(false)),
    ).subscribe({
      next: (avaliacao) => {
        this.avaliacoes.update((atuais) => [...atuais, avaliacao]);
        this.sucesso.set('Avaliação enviada e vinculada ao acordo concluído.');
      },
      error: () => this.erro.set('Não foi possível enviar a avaliação. Verifique se este acordo já foi avaliado.'),
    });
  }

  protected responderAvaliacao(avaliacao: Avaliacao): void {
    const resposta = (this.respostas[avaliacao.id] ?? '').trim();
    if (!resposta || resposta.length > 1000 || this.respondendoAvaliacao() !== null
        || avaliacao.respostaProfissional || this.perfil()?.id !== avaliacao.avaliadoId
        || this.acordo()?.anuncianteId !== this.perfil()?.id) return;
    this.respondendoAvaliacao.set(avaliacao.id);
    this.erro.set('');
    this.sucesso.set('');
    this.api.responderAvaliacao(this.acordoId, avaliacao.id, resposta).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.respondendoAvaliacao.set(null)),
    ).subscribe({
      next: (atualizada) => {
        this.avaliacoes.update((atuais) => atuais.map((item) => item.id === atualizada.id ? atualizada : item));
        this.respostas[avaliacao.id] = '';
        this.sucesso.set('Sua resposta foi publicada no perfil profissional.');
      },
      error: () => this.erro.set('Não foi possível publicar a resposta. Atualize o acordo e tente novamente.'),
    });
  }

  protected podeEditarAvaliacao(avaliacao: Avaliacao): boolean {
    const perfil = this.perfil();
    const acordo = this.acordo();
    if (!perfil || !acordo || perfil.id !== avaliacao.avaliadorId || acordo.proponenteId !== perfil.id
        || acordo.status !== 'CONCLUIDO' || avaliacao.edicaoRealizada || avaliacao.respostaProfissional) return false;
    return Date.now() <= new Date(avaliacao.criadoEm).getTime() + 24 * 60 * 60 * 1000;
  }

  protected iniciarEdicaoAvaliacao(avaliacao: Avaliacao): void {
    if (!this.podeEditarAvaliacao(avaliacao)) return;
    this.notaEdicao = avaliacao.nota;
    this.notaAnuncioEdicao = avaliacao.notaAnuncio ?? 0;
    this.depoimentoEdicao = avaliacao.depoimento ?? '';
    this.editandoAvaliacao.set(avaliacao.id);
    this.erro.set('');
  }

  protected salvarEdicaoAvaliacao(avaliacao: Avaliacao): void {
    if (!this.podeEditarAvaliacao(avaliacao) || !this.notaEdicao || !this.notaAnuncioEdicao
        || this.salvandoEdicaoAvaliacao() !== null) return;
    this.salvandoEdicaoAvaliacao.set(avaliacao.id);
    this.erro.set('');
    this.api.editarAvaliacao(this.acordoId, avaliacao.id, this.notaEdicao,
      this.notaAnuncioEdicao, this.depoimentoEdicao.trim()).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.salvandoEdicaoAvaliacao.set(null)),
    ).subscribe({
      next: (atualizada) => {
        this.avaliacoes.update((atuais) => atuais.map((item) => item.id === atualizada.id ? atualizada : item));
        this.editandoAvaliacao.set(null);
        this.sucesso.set('Avaliação atualizada. A versão anterior foi preservada para auditoria.');
      },
      error: () => this.erro.set('Não foi possível editar. O prazo pode ter terminado ou a avaliação pode ter recebido resposta, denúncia ou contestação.'),
    });
  }

  protected atualizarResposta(avaliacaoId: number, resposta: string): void {
    this.respostas = { ...this.respostas, [avaliacaoId]: resposta };
  }

  protected atualizarJustificativa(avaliacaoId: number, justificativa: string): void {
    this.justificativas = { ...this.justificativas, [avaliacaoId]: justificativa };
  }

  protected contestarAvaliacao(avaliacao: Avaliacao): void {
    const justificativa = (this.justificativas[avaliacao.id] ?? '').trim();
    if (justificativa.length < 20 || justificativa.length > 1000
        || this.contestandoAvaliacao() !== null || this.perfil()?.id !== avaliacao.avaliadoId) return;
    this.contestandoAvaliacao.set(avaliacao.id);
    this.erro.set('');
    this.sucesso.set('');
    this.api.contestarAvaliacao(avaliacao.id, justificativa).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.contestandoAvaliacao.set(null)),
    ).subscribe({
      next: (contestacao) => {
        this.contestacoes.update((atuais) => ({ ...atuais, [avaliacao.id]: contestacao }));
        this.justificativas[avaliacao.id] = '';
        this.sucesso.set('Contestação enviada em caráter privado para análise. A avaliação segue visível enquanto aguarda decisão.');
      },
      error: () => this.erro.set('Não foi possível contestar esta avaliação. Confira o prazo e atualize o acordo.'),
    });
  }

  private carregarContestacoesDoProfissional(avaliacoes: readonly Avaliacao[], perfilId: number): void {
    this.contestacoes.set({});
    for (const avaliacao of avaliacoes) {
      if (avaliacao.avaliadoId !== perfilId) continue;
      this.api.buscarContestacao(avaliacao.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: (contestacoes) => {
          if (contestacoes.length) {
            this.contestacoes.update((atuais) => ({ ...atuais, [avaliacao.id]: contestacoes[0] }));
          }
        },
      });
    }
  }
}
