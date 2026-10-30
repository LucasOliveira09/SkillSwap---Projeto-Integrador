import { DOCUMENT, DatePipe } from '@angular/common';
import { Component, DestroyRef, ElementRef, Injector, afterNextRender, computed, effect, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize, forkJoin, interval } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { SessaoService } from '../../../conta/data-access/sessao.service';
import { PerfilApiService } from '../../../perfil/data-access/perfil-api.service';
import { PerfilProfissional } from '../../../perfil/models/perfil.model';
import { Pagina } from '../../models/negociacao.model';
import { Conversa, MensagemConversa, NovaPropostaConversa, PropostaMensagem } from '../../models/conversa.model';
import { ConversasApiService } from '../../data-access/conversas-api.service';
import { IndicadorMensagensService } from '../../data-access/indicador-mensagens.service';
import { NegociacaoApiService } from '../../data-access/negociacao-api.service';
import { AnunciosApiService } from '../../../anuncios/data-access/anuncios-api.service';
import { AnuncioApi } from '../../../anuncios/models/anuncio-api.model';

@Component({
  selector: 'app-caixa-mensagens',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './caixa-mensagens.component.html',
  styleUrl: './caixa-mensagens.component.scss',
})
export class CaixaMensagensComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly perfilApi = inject(PerfilApiService);
  private readonly api = inject(ConversasApiService);
  private readonly negociacaoApi = inject(NegociacaoApiService);
  private readonly anunciosApi = inject(AnunciosApiService);
  private readonly indicador = inject(IndicadorMensagensService);
  private readonly leituras = new Map<number, number>();
  private atualizandoLista = false;
  private readonly destroyRef = inject(DestroyRef);

  protected readonly carregando = signal(true);
  protected readonly carregandoMais = signal(false);
  protected readonly enviando = signal(false);
  protected readonly erro = signal('');
  protected readonly sucesso = signal('');
  protected readonly perfil = signal<PerfilProfissional | null>(null);
  protected readonly conversas = signal<readonly Conversa[]>([]);
  protected readonly paginaConversas = signal(0);
  protected readonly totalPaginasConversas = signal(0);
  protected readonly conversaSelecionada = signal<Conversa | null>(null);
  protected readonly mensagens = signal<readonly MensagemConversa[]>([]);
  protected readonly paginaMensagens = signal(0);
  protected readonly totalPaginasMensagens = signal(0);
  protected readonly carregandoMensagens = signal(false);
  protected texto = '';
  protected readonly filtro = signal('');
  protected readonly visiveis = computed(() => {
    const busca = this.filtro().trim().toLocaleLowerCase('pt-BR');
    return this.conversas().filter(acordo =>
      `${this.interlocutor(acordo)} ${acordo.anuncioTitulo ?? ''}`.toLocaleLowerCase('pt-BR').includes(busca));
  });
  protected readonly erroSincronizacao = signal('');
  protected readonly anuncioContextoInicial = signal<number | null>(null);
  protected readonly mostrandoProposta = signal(false);
  protected readonly anunciosDoInterlocutor = signal<readonly AnuncioApi[]>([]);
  protected readonly carregandoAnunciosProposta = signal(false);
  protected readonly enviandoProposta = signal(false);
  protected readonly respondendoProposta = signal<number | null>(null);
  protected proposta: NovaPropostaConversa = {
    anuncioId: 0, servico: '', escopo: '', formaPagamento: 'A_VISTA', valor: null,
    parcelas: null, duracao: '', validadeEm: '',
  };
  private readonly document = inject(DOCUMENT);
  private readonly injector = inject(Injector);
  private readonly historico = viewChild<ElementRef<HTMLDivElement>>('historico');
  private readonly rascunhos = new Map<number, string>();
  private sincronizando = false;
  private rolarAoAbrir = true;
  private readonly sessao = inject(SessaoService);
  private readonly acompanharSessao = effect(() => {
    if (!this.sessao.autenticado()) {
      const id = this.conversaSelecionada()?.id;
      void this.router.navigate(['/conta'], { queryParams: { retorno: id ? `/mensagens?conversa=chat-${id}` : '/mensagens' } });
    }
  });
  private geracaoSelecao = 0;

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (!this.carregando()) this.sincronizarRota();
    });
    interval(8000).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.document.visibilityState === 'visible') { this.atualizarEntrada(); this.atualizarConversa(); }
    });
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erro.set('');
    forkJoin({ perfil: this.perfilApi.meuPerfil(), conversas: this.api.listar(0) })
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.carregando.set(false)))
      .subscribe({
        next: ({ perfil, conversas }) => {
          this.perfil.set(perfil);
          this.conversas.set(conversas.itens);
          this.paginaConversas.set(conversas.pagina);
          this.totalPaginasConversas.set(conversas.totalPaginas);
          this.sincronizarRota();
        },
        error: () => this.erro.set('Não foi possível carregar suas conversas. Tente novamente.'),
      });
  }

  protected carregarMaisConversas(): void {
    if (this.carregandoMais() || this.paginaConversas() + 1 >= this.totalPaginasConversas()) return;
    const proximaPagina = this.paginaConversas() + 1;
    this.carregandoMais.set(true);
    this.api.listar(proximaPagina)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.carregandoMais.set(false)))
      .subscribe({
        next: (pagina) => {
          this.conversas.update((atuais) => [...new Map(
            [...atuais, ...pagina.itens].map(item => [item.id, item]),
          ).values()]);
          this.paginaConversas.set(pagina.pagina);
          this.totalPaginasConversas.set(pagina.totalPaginas);
        },
        error: () => this.erro.set('Não foi possível carregar mais conversas.'),
      });
  }

  protected selecionar(acordo: Conversa, atualizarUrl = true): void {
    if (atualizarUrl) {
      void this.router.navigate([], { relativeTo: this.route, queryParams: { conversa: `chat-${acordo.id}`, acordo: null, proposta: null, anuncioId: null }, queryParamsHandling: 'merge' });
      return;
    }
    const anterior = this.conversaSelecionada();
    if (anterior) this.rascunhos.set(anterior.id, this.texto);
    this.texto = this.rascunhos.get(acordo.id) ?? '';
    this.rolarAoAbrir = true;
    const geracao = ++this.geracaoSelecao;
    this.conversaSelecionada.set(acordo);
    this.mensagens.set([]);
    this.erro.set('');
    this.sucesso.set('');
    this.carregandoMensagens.set(true);
    this.api.mensagens(acordo.id, 0)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (primeiraPagina) => {
          if (geracao !== this.geracaoSelecao) return;
          if (primeiraPagina.totalPaginas <= 1) {
            this.aplicarMensagens(primeiraPagina);
            this.carregandoMensagens.set(false);
            return;
          }
          const paginaAtual = Math.max(0, primeiraPagina.totalPaginas - 1);
          this.api.mensagens(acordo.id, paginaAtual)
            .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => {
              if (geracao === this.geracaoSelecao) this.carregandoMensagens.set(false);
            }))
            .subscribe({
              next: (pagina) => {
                if (geracao === this.geracaoSelecao) this.aplicarMensagens(pagina);
              },
              error: () => {
                if (geracao === this.geracaoSelecao) this.erro.set('Não foi possível carregar as mensagens desta conversa.');
              },
            });
        },
        error: () => {
          if (geracao === this.geracaoSelecao) {
            this.carregandoMensagens.set(false);
            this.erro.set('Não foi possível carregar as mensagens desta conversa.');
          }
        },
      });
  }

  protected voltarLista(): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: { conversa: null, acordo: null, proposta: null, anuncioId: null }, queryParamsHandling: 'merge' });
  }

  private sincronizarRota(): void {
    const legado = Number(this.route.snapshot.queryParamMap.get('acordo'));
    if (Number.isSafeInteger(legado) && legado > 0) {
      const geracao = ++this.geracaoSelecao;
      this.negociacaoApi.acordo(legado).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: acordo => this.resolverPropostaLegada(acordo.propostaId, geracao),
        error: () => this.erro.set('Conversa indisponível ou sem permissão de acesso.'),
      });
      return;
    }
    const valorConversa = this.route.snapshot.queryParamMap.get('conversa');
    const propostaLegada = Number(this.route.snapshot.queryParamMap.get('proposta') ??
      (valorConversa && /^\d+$/.test(valorConversa) ? valorConversa : NaN));
    if (Number.isSafeInteger(propostaLegada) && propostaLegada > 0 && (!valorConversa || !valorConversa.startsWith('chat-'))) {
      this.resolverPropostaLegada(propostaLegada, ++this.geracaoSelecao);
      return;
    }
    const textoId = valorConversa?.replace(/^chat-/, '') ?? '';
    const id = Number(textoId);
    const anuncioId = Number(this.route.snapshot.queryParamMap.get('anuncioId'));
    this.anuncioContextoInicial.set(Number.isSafeInteger(anuncioId) && anuncioId > 0 ? anuncioId : null);
    if (id === this.conversaSelecionada()?.id) return;
    if (!Number.isSafeInteger(id) || id <= 0) {
      const anterior = this.conversaSelecionada();
      if (anterior) this.rascunhos.set(anterior.id, this.texto);
      ++this.geracaoSelecao;
      this.conversaSelecionada.set(null);
      this.mensagens.set([]);
      this.carregandoMensagens.set(false);
      return;
    }
    const acordo = this.conversas().find(item => item.id === id);
    if (acordo) { this.selecionar(acordo, false); return; }
    const anterior = this.conversaSelecionada();
    if (anterior) this.rascunhos.set(anterior.id, this.texto);
    this.conversaSelecionada.set(null);
    this.mensagens.set([]);
    this.carregandoMensagens.set(true);
    const geracao = ++this.geracaoSelecao;
    this.api.conversa(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: item => {
        if (geracao !== this.geracaoSelecao) return;
        this.conversas.update(atuais => [item, ...atuais.filter(a => a.id !== item.id)]);
        this.selecionar(item, false);
      },
      error: () => { if (geracao === this.geracaoSelecao) { this.carregandoMensagens.set(false); this.erro.set('Conversa indisponível ou sem permissão de acesso.'); } },
    });
  }

  private resolverPropostaLegada(id: number, geracao: number): void {
    this.api.conversaDaProposta(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: conversa => { if (geracao === this.geracaoSelecao) void this.router.navigate([], {
        relativeTo: this.route,
        queryParams: { proposta: null, acordo: null, conversa: `chat-${conversa.id}` },
        queryParamsHandling: 'merge', replaceUrl: true,
      }); },
      error: () => { if (geracao === this.geracaoSelecao) this.erro.set('Conversa indisponível ou sem permissão de acesso.'); },
    });
  }

  protected atualizarEntrada(): void {
    if (this.carregando() || this.atualizandoLista) return;
    this.atualizandoLista = true;
    this.api.listar(0).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.atualizandoLista = false)).subscribe({
      next: pagina => {
        this.conversas.update(atuais => [...new Map([...pagina.itens, ...atuais.filter(a => !pagina.itens.some(n => n.id === a.id))].map(a => [a.id, a])).values()]);
        this.totalPaginasConversas.set(pagina.totalPaginas);
        const selecionada = this.conversaSelecionada();
        const atualizada = selecionada && pagina.itens.find(a => a.id === selecionada.id);
        if (atualizada) this.conversaSelecionada.set(atualizada);
      },
      error: () => this.erroSincronizacao.set('Não foi possível atualizar a caixa de entrada. Tentaremos novamente.'),
    });
  }

  protected rotuloConversa(conversa: Conversa): string {
    if (conversa.acordoStatus === 'CONCLUIDO') return 'Concluído';
    if (conversa.acordoStatus === 'CANCELADO' || conversa.status === 'CANCELADA') return 'Cancelado';
    if (conversa.status === 'RECUSADA') return 'Proposta recusada';
    return conversa.acordoId ? 'Em andamento' : 'Primeiro contato';
  }

  private marcarLeitura(pagina: Pagina<MensagemConversa>): void {
    const conversa = this.conversaSelecionada();
    const ultima = pagina.itens.at(-1);
    if (!conversa || !ultima || pagina.pagina + 1 < pagina.totalPaginas ||
        this.document.visibilityState !== 'visible' || this.leituras.get(conversa.id) === ultima.id) return;
    this.api.ler(conversa.id, ultima.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.leituras.set(conversa.id, ultima.id);
        this.conversas.update(itens => itens.map(a => a.id === conversa.id ? { ...a, naoLidas: 0 } : a));
        this.indicador.atualizar();
      },
      error: () => this.erroSincronizacao.set('Não foi possível confirmar a leitura. Tentaremos novamente.'),
    });
  }

  protected atualizarConversa(): void {
    const acordo = this.conversaSelecionada();
    if (!acordo || this.sincronizando || this.enviando() || this.carregandoMensagens() ||
      this.paginaMensagens() < this.totalPaginasMensagens() - 1) return;
    const geracao = this.geracaoSelecao;
    this.sincronizando = true;
    this.api.mensagens(acordo.id, this.paginaMensagens()).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.sincronizando = false),
    ).subscribe({
      next: pagina => {
        if (geracao !== this.geracaoSelecao) return;
        this.erroSincronizacao.set('');
        if (pagina.totalPaginas > this.totalPaginasMensagens()) {
          this.carregarPagina(acordo.id, pagina.totalPaginas - 1, geracao);
        } else this.aplicarMensagens(pagina);
      },
      error: () => { if (geracao === this.geracaoSelecao) this.erroSincronizacao.set('A atualização automática falhou. Tentaremos novamente.'); },
    });
  }

  private carregarPagina(id: number, pagina: number, geracao: number): void {
    this.carregandoMensagens.set(true);
    this.api.mensagens(id, pagina).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => {
      if (geracao === this.geracaoSelecao) this.carregandoMensagens.set(false);
    })).subscribe({
      next: resposta => { if (geracao === this.geracaoSelecao) this.aplicarMensagens(resposta); },
      error: () => { if (geracao === this.geracaoSelecao) this.erroSincronizacao.set('Não foi possível atualizar as mensagens.'); },
    });
  }

  protected carregarMensagensRecentes(): void {
    const acordo = this.conversaSelecionada();
    if (acordo && !this.carregandoMensagens()) this.selecionar(acordo, false);
  }

  protected aoEnter(evento: KeyboardEvent): void {
    if (!evento.shiftKey && !evento.isComposing && window.matchMedia('(pointer: fine)').matches) {
      evento.preventDefault();
      this.enviar();
    }
  }

  protected carregarMensagensAnteriores(): void {
    const acordo = this.conversaSelecionada();
    const pagina = this.paginaMensagens() - 1;
    if (!acordo || pagina < 0 || this.carregandoMensagens()) return;
    const geracao = this.geracaoSelecao;
    this.carregandoMensagens.set(true);
    this.api.mensagens(acordo.id, pagina)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => {
        if (geracao === this.geracaoSelecao) this.carregandoMensagens.set(false);
      }))
      .subscribe({
        next: (resposta) => {
          if (geracao === this.geracaoSelecao) this.aplicarMensagens(resposta);
        },
        error: () => {
          if (geracao === this.geracaoSelecao) this.erro.set('Não foi possível carregar mensagens anteriores.');
        },
      });
  }

  protected enviar(): void {
    const acordo = this.conversaSelecionada();
    const conteudo = this.texto.trim();
    if (!acordo || !acordo.podeEnviar || !conteudo || conteudo.length > 2000 || this.enviando()) return;
    const geracao = this.geracaoSelecao;
    this.enviando.set(true);
    this.erro.set('');
    this.sucesso.set('');
    this.api.enviarMensagem(acordo.id, conteudo, this.anuncioContextoInicial() ?? acordo.anuncioId ?? undefined)
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.enviando.set(false)))
      .subscribe({
        next: () => {
          this.rascunhos.delete(acordo.id);
          if (geracao !== this.geracaoSelecao) return;
          this.texto = '';
          this.anuncioContextoInicial.set(null);
          void this.router.navigate([], { relativeTo: this.route, queryParams: { anuncioId: null }, queryParamsHandling: 'merge', replaceUrl: true });
          this.selecionar(acordo, false);
          this.sucesso.set('Mensagem enviada.');
        },
        error: () => { if (geracao === this.geracaoSelecao) this.erro.set('Não foi possível enviar a mensagem. Tente novamente.'); },
      });
  }

  protected abrirFormularioProposta(conversa: Conversa): void {
    this.proposta = {
      anuncioId: this.anuncioContextoInicial() ?? conversa.anuncioId ?? 0, servico: '', escopo: '',
      formaPagamento: 'A_VISTA',
      valor: null, parcelas: null, duracao: '', validadeEm: '',
    };
    this.anunciosDoInterlocutor.set([]);
    this.carregandoAnunciosProposta.set(true);
    this.mostrandoProposta.set(true);
    this.anunciosApi.listarDoUsuario(conversa.outroParticipanteId, 0, 50).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.carregandoAnunciosProposta.set(false)),
    ).subscribe({
      next: pagina => {
        this.anunciosDoInterlocutor.set(pagina.itens);
        if (!pagina.itens.some(anuncio => anuncio.id === this.proposta.anuncioId)) {
          this.proposta.anuncioId = pagina.itens[0]?.id ?? 0;
        }
        this.ajustarFormaPagamentoDaOferta();
      },
      error: () => this.erro.set('Não foi possível carregar os anúncios disponíveis para a proposta.'),
    });
  }

  protected ajustarFormaPagamentoDaOferta(): void {
    const anuncio = this.anunciosDoInterlocutor().find(item => item.id === Number(this.proposta.anuncioId));
    this.proposta.formaPagamento = anuncio?.modalidade === 'troca' ? 'TROCA' : 'A_VISTA';
    this.proposta.valor = null;
    this.proposta.parcelas = null;
  }

  protected ofertaDaProposta(): AnuncioApi | null {
    return this.anunciosDoInterlocutor().find(item => item.id === Number(this.proposta.anuncioId)) ?? null;
  }

  protected enviarProposta(): void {
    const conversa = this.conversaSelecionada();
    if (!conversa || !this.proposta.anuncioId || this.enviandoProposta()) return;
    const troca = this.ofertaDaProposta()?.modalidade === 'troca';
    const proposta: NovaPropostaConversa = {
      anuncioId: Number(this.proposta.anuncioId),
      servico: this.proposta.servico.trim(),
      escopo: this.proposta.escopo.trim(),
      formaPagamento: troca ? 'TROCA' : this.proposta.formaPagamento,
      valor: troca ? null : this.proposta.valor,
      parcelas: troca || this.proposta.formaPagamento !== 'PARCELADO' ? null : this.proposta.parcelas,
      duracao: this.proposta.duracao?.trim() || null,
      validadeEm: this.proposta.validadeEm || null,
    };
    if (!proposta.servico || !proposta.escopo || (!troca && (!proposta.valor || proposta.valor <= 0)) ||
      (!troca && proposta.formaPagamento === 'PARCELADO' && (!proposta.parcelas || proposta.parcelas < 2 || proposta.parcelas > 12))) {
      this.erro.set('Preencha o serviço, o escopo e os termos de pagamento válidos.');
      return;
    }
    this.enviandoProposta.set(true);
    this.erro.set('');
    this.api.criarProposta(conversa.id, proposta).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.enviandoProposta.set(false)),
    ).subscribe({
      next: () => {
        this.mostrandoProposta.set(false);
        this.selecionar(conversa, false);
        this.sucesso.set('Proposta enviada no chat.');
        this.atualizarEntrada();
      },
      error: () => this.erro.set('Não foi possível enviar a proposta. Confira os termos e tente novamente.'),
    });
  }

  protected propostaExpirada(proposta: PropostaMensagem): boolean {
    if (!proposta.validadeEm) return false;
    const hoje = new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Sao_Paulo' }).format(new Date());
    return proposta.validadeEm < hoje;
  }

  protected responderProposta(proposta: PropostaMensagem, status: 'ACEITA' | 'RECUSADA' | 'CANCELADA'): void {
    const conversa = this.conversaSelecionada();
    if (!conversa || this.respondendoProposta() !== null) return;
    this.respondendoProposta.set(proposta.id);
    this.negociacaoApi.atualizarProposta(proposta.id, status).pipe(
      takeUntilDestroyed(this.destroyRef), finalize(() => this.respondendoProposta.set(null)),
    ).subscribe({
      next: () => {
        this.selecionar(conversa, false);
        this.atualizarEntrada();
        this.sucesso.set(status === 'ACEITA' ? 'Proposta aceita. O acordo está disponível nesta conversa.' :
          status === 'RECUSADA' ? 'Proposta recusada.' : 'Proposta cancelada.');
      },
      error: () => this.erro.set('Não foi possível atualizar a proposta. Ela pode já ter sido respondida.'),
    });
  }

  protected termosPagamento(proposta: PropostaMensagem): string {
    if (proposta.formaPagamento === 'TROCA') return 'Troca sem complemento em dinheiro';
    const valor = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(proposta.valor ?? 0);
    return proposta.formaPagamento === 'PARCELADO'
      ? `${valor} em ${proposta.parcelas} parcelas`
      : `${valor} à vista`;
  }

  protected interlocutor(acordo: Conversa): string {
    return acordo.outroParticipanteNome;
  }

  protected iniciais(nome: string): string {
    return nome.trim().split(/\s+/).slice(0, 2).map((parte) => parte.charAt(0)).join('').toLocaleUpperCase('pt-BR');
  }

  protected carregarMensagemAnteriorDisponivel(): boolean {
    return this.paginaMensagens() > 0;
  }

  private aplicarMensagens(pagina: Pagina<MensagemConversa>): void {
    const elemento = this.historico()?.nativeElement;
    const rolar = this.rolarAoAbrir || (elemento && elemento.scrollHeight - elemento.scrollTop - elemento.clientHeight < 100);
    this.rolarAoAbrir = false;
    this.mensagens.set(pagina.itens);
    if (rolar) afterNextRender(() => {
      const historico = this.historico()?.nativeElement;
      if (historico) historico.scrollTop = historico.scrollHeight;
    }, { injector: this.injector });
    this.paginaMensagens.set(pagina.pagina);
    this.totalPaginasMensagens.set(pagina.totalPaginas);
    this.marcarLeitura(pagina);
  }
}
