import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { catchError, map, of, startWith, switchMap } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { AnunciosApiService } from '../../data-access/anuncios-api.service';
import { AnuncioApi } from '../../models/anuncio-api.model';
import { DiaSemana, PeriodoDia } from '../../models/anuncio.model';
import { SessaoService } from '../../../conta/data-access/sessao.service';
import { IconeComponent } from '../../../../shared/ui/icone/icone.component';

interface EstadoDetalhe {
  carregando: boolean;
  erro: boolean;
  anuncio: AnuncioApi | null;
}

const estadoInicial: EstadoDetalhe = {
  carregando: true,
  erro: false,
  anuncio: null,
};

@Component({
  selector: 'app-detalhe-anuncio',
  standalone: true,
  imports: [
    RouterLink,
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    IconeComponent,
  ],
  templateUrl: './detalhe-anuncio.component.html',
  styleUrl: './detalhe-anuncio.component.scss',
})
export class DetalheAnuncioComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(AnunciosApiService);
  protected readonly sessao = inject(SessaoService);

  protected readonly fotoSelecionada = signal<string | null>(null);
  mensagemTexto = '';
  protected readonly enviandoMensagem = signal(false);
  protected readonly feedbackSucesso = signal<string | null>(null);
  protected readonly feedbackErro = signal<string | null>(null);

  protected readonly estado = toSignal(
    this.route.paramMap.pipe(
      switchMap((params) => {
        const id = params.get('id');
        if (!id) {
          return of({ carregando: false, erro: true, anuncio: null });
        }
        return this.api.obter(id).pipe(
          map((anuncio) => {
            this.fotoSelecionada.set(anuncio.imagem);
            return { carregando: false, erro: false, anuncio };
          }),
          catchError(() => of({ carregando: false, erro: true, anuncio: null })),
          startWith(estadoInicial),
        );
      }),
    ),
    { initialValue: estadoInicial },
  );

  protected readonly anuncio = computed(() => this.estado().anuncio);
  protected readonly fotoPrincipal = computed(() => {
    return this.fotoSelecionada() || this.anuncio()?.imagem || null;
  });

  protected readonly linhasDescricao = computed(() => {
    const desc = this.anuncio()?.descricao;
    if (!desc) return [];
    return desc.split('\n').map((l) => l.trim()).filter((l) => l.length > 0);
  });

  protected selecionarFoto(url: string | null): void {
    this.fotoSelecionada.set(url);
  }

  protected estrelas(quantidade: number | null): string {
    if (quantidade === null) return '';
    return '★'.repeat(quantidade) + '☆'.repeat(5 - quantidade);
  }

  protected rotuloDia(dia: DiaSemana): string {
    return ({ SEGUNDA: 'Segunda-feira', TERCA: 'Terça-feira', QUARTA: 'Quarta-feira', QUINTA: 'Quinta-feira', SEXTA: 'Sexta-feira', SABADO: 'Sábado', DOMINGO: 'Domingo' })[dia];
  }

  protected rotuloPeriodo(periodo: PeriodoDia): string {
    return ({ MANHA: 'Manhã', TARDE: 'Tarde', NOITE: 'Noite' })[periodo];
  }

  protected rolarParaMensagens(): void {
    const el = document.getElementById('secao-contato');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      const textarea = el.querySelector('textarea');
      if (textarea) {
        setTimeout(() => textarea.focus(), 300);
      }
    }
  }

  protected enviarMensagem(): void {
    const anuncioAtual = this.anuncio();
    if (!anuncioAtual) return;

    if (!this.sessao.autenticado()) {
      void this.router.navigate(['/conta'], {
        queryParams: { retorno: `/anuncios/${anuncioAtual.id}` },
      });
      return;
    }

    const texto = this.mensagemTexto.trim();
    if (!texto) {
      this.feedbackErro.set('Por favor, digite sua mensagem antes de enviar.');
      return;
    }

    this.enviandoMensagem.set(true);
    this.feedbackSucesso.set(null);
    this.feedbackErro.set(null);

    this.api.enviarProposta(anuncioAtual.id, texto).subscribe({
      next: () => {
        this.enviandoMensagem.set(false);
        this.feedbackSucesso.set(
          'Mensagem e proposta enviadas com sucesso! O anunciante recebeu seu contato.',
        );
        this.mensagemTexto = '';
      },
      error: (err) => {
        this.enviandoMensagem.set(false);
        const msg =
          err?.error?.mensagem ||
          'Não foi possível enviar a mensagem. Verifique se você já possui uma proposta ativa.';
        this.feedbackErro.set(msg);
      },
    });
  }
}
