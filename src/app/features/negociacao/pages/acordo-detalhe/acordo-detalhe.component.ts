import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin, finalize } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { SessaoService } from '../../../conta/data-access/sessao.service';
import { PerfilApiService } from '../../../perfil/data-access/perfil-api.service';
import { PerfilProfissional } from '../../../perfil/models/perfil.model';
import { NegociacaoApiService } from '../../data-access/negociacao-api.service';
import { Acordo, Avaliacao, Mensagem } from '../../models/negociacao.model';

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
  protected readonly erro = signal('');
  protected readonly sucesso = signal('');
  protected readonly acordo = signal<Acordo | null>(null);
  protected readonly perfil = signal<PerfilProfissional | null>(null);
  protected readonly mensagens = signal<readonly Mensagem[]>([]);
  protected readonly avaliacoes = signal<readonly Avaliacao[]>([]);
  protected readonly mensagem = signal('');
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

  protected readonly jaAvaliou = computed(() => {
    const perfil = this.perfil();
    return perfil ? this.avaliacoes().some((avaliacao) => avaliacao.avaliadorId === perfil.id) : false;
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
      mensagens: this.api.mensagens(this.acordoId),
      avaliacoes: this.api.avaliacoes(this.acordoId),
      perfil: this.perfilApi.meuPerfil(),
    }).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.carregando.set(false)),
    ).subscribe({
      next: (resposta) => {
        this.acordo.set(resposta.acordo);
        this.mensagens.set(resposta.mensagens.itens);
        this.avaliacoes.set(resposta.avaliacoes);
        this.perfil.set(resposta.perfil);
      },
      error: () => this.erro.set('Não foi possível carregar este acordo. Confira se você participa dele e tente novamente.'),
    });
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
        this.mensagens.update((atuais) => [...atuais, mensagem]);
        this.mensagem.set('');
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
        this.sucesso.set('Acordo concluído. Você já pode avaliar sua experiência.');
      },
      error: () => this.erro.set('Não foi possível concluir o acordo. Atualize a página e tente novamente.'),
    });
  }

  protected avaliar(): void {
    if (!this.nota || !this.notaAnuncio || this.enviando() || this.acordo()?.status !== 'CONCLUIDO' || this.jaAvaliou()) return;
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
}
