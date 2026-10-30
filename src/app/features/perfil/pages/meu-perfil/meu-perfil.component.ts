import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { concatMap, finalize, from, of, switchMap, tap, toArray } from 'rxjs';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { SessaoService } from '../../../conta/data-access/sessao.service';
import { AtualizacaoPerfil, PerfilProfissional, PublicacaoPortfolio, UsoArmazenamento } from '../../models/perfil.model';
import { PerfilApiService } from '../../data-access/perfil-api.service';
import { AnunciosApiService } from '../../../anuncios/data-access/anuncios-api.service';
import { AnuncioApi } from '../../../anuncios/models/anuncio-api.model';

const formularioVazio: AtualizacaoPerfil = {
  nome: '',
  tituloProfissional: '',
  bio: '',
  cidade: '',
  especialidades: '',
  linkedinUrl: '',
  instagramUrl: '',
  siteUrl: '',
  perfilPublico: true,
  disponivelTrabalhos: false,
};

@Component({
  selector: 'app-meu-perfil',
  standalone: true,
  imports: [FormsModule, RouterLink, DatePipe, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatInputModule],
  templateUrl: './meu-perfil.component.html',
  styleUrl: './meu-perfil.component.scss',
})
export class MeuPerfilComponent {
  private readonly api = inject(PerfilApiService);
  private readonly anunciosApi = inject(AnunciosApiService);
  private readonly sessao = inject(SessaoService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly perfil = signal<PerfilProfissional | null>(null);
  protected readonly usoArmazenamento = signal<UsoArmazenamento | null>(null);
  protected erroArmazenamento = '';
  protected readonly previaFoto = signal<string | null>(null);
  protected readonly previaCapa = signal<string | null>(null);
  protected readonly statusOperacaoFoto = signal('');
  protected readonly statusOperacaoCapa = signal('');
  protected dados: AtualizacaoPerfil = { ...formularioVazio };
  protected arquivoFoto: File | null = null;
  protected arquivoCapa: File | null = null;
  protected carregando = false;
  protected salvando = false;
  protected enviandoFoto = false;
  protected enviandoCapa = false;
  protected erro = '';
  protected sucesso = '';
  protected readonly publicacoes = signal<readonly PublicacaoPortfolio[]>([]);
  protected readonly ofertasPortfolio = signal<readonly AnuncioApi[]>([]);
  protected readonly previaFotosPortfolio = signal<readonly { arquivo: File; url: string }[]>([]);
  protected legendaPortfolio = '';
  protected anuncioPortfolioId = '';
  protected carregandoPortfolio = false;
  protected enviandoPortfolio = false;
  protected removendoPublicacaoId: number | null = null;
  protected readonly editandoPublicacaoId = signal<number | null>(null);
  protected legendaEmEdicao = '';
  protected readonly salvandoPublicacaoId = signal<number | null>(null);
  protected readonly paginaPortfolio = signal(0);
  protected readonly totalPaginasPortfolio = signal(0);
  protected readonly totalPublicacoesPortfolio = signal(0);
  protected erroPortfolio = '';
  protected sucessoPortfolio = '';
  private arquivosPortfolio: File[] = [];
  private urlsFotosPortfolio: (string | null)[] = [];

  private readonly urlsTemporarias = new Set<string>();
  private portfolioCarregadoParaId: number | null = null;
  private ofertasCarregadasParaId: number | null = null;

  constructor() {
    this.destroyRef.onDestroy(() => {
      for (const url of this.urlsTemporarias) URL.revokeObjectURL(url);
    });
    if (!this.sessao.autenticado()) {
      void this.router.navigate(['/conta'], { queryParams: { retorno: '/meu-perfil' } });
      return;
    }
    this.carregar();
    this.carregarUsoArmazenamento();
  }

  protected carregar(): void {
    this.carregando = true;
    this.erro = '';
    this.api
      .meuPerfil()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => (this.carregando = false)),
      )
      .subscribe({
        next: (perfil) => this.preencher(perfil),
        error: () => (this.erro = 'Não foi possível carregar seu perfil. Tente novamente.'),
      });
  }

  protected salvar(form: NgForm): void {
    if (this.salvando || this.enviandoFoto || this.enviandoCapa || form.invalid) {
      form.control.markAllAsTouched();
      return;
    }

    this.salvando = true;
    this.erro = '';
    this.sucesso = '';
    const dados = {
      nome: this.dados.nome.trim(),
      tituloProfissional: this.dados.tituloProfissional.trim(),
      bio: this.dados.bio.trim(),
      cidade: this.dados.cidade.trim(),
      especialidades: this.dados.especialidades.trim(),
      linkedinUrl: (this.dados.linkedinUrl ?? '').trim(),
      instagramUrl: (this.dados.instagramUrl ?? '').trim(),
      siteUrl: (this.dados.siteUrl ?? '').trim(),
      perfilPublico: this.dados.perfilPublico !== false,
      disponivelTrabalhos: this.dados.disponivelTrabalhos === true,
    };

    this.api
      .atualizarMeuPerfil(dados)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => (this.salvando = false)),
      )
      .subscribe({
        next: (perfil) => {
          this.preencher(perfil);
          this.sucesso = perfil.perfilPublico !== false
            ? 'Perfil salvo e visível publicamente.'
            : 'Perfil salvo e pausado nas páginas públicas.';
        },
        error: () => (this.erro = 'Não foi possível salvar. Seus dados continuam neste formulário.'),
      });
  }

  protected selecionarFoto(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const arquivo = campo.files?.[0] ?? null;
    campo.value = '';
    this.erro = '';
    this.sucesso = '';

    if (!arquivo) return;
    if (!['image/jpeg', 'image/png', 'image/gif'].includes(arquivo.type)) {
      this.erro = 'Escolha uma imagem JPG, PNG ou GIF.';
      return;
    }
    if (arquivo.size > 5 * 1024 * 1024) {
      this.erro = 'A imagem deve ter no máximo 5 MB.';
      return;
    }

    this.limparPrevia();
    this.arquivoFoto = arquivo;
    const url = URL.createObjectURL(arquivo);
    this.urlsTemporarias.add(url);
    this.previaFoto.set(url);
  }

  protected selecionarCapa(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const arquivo = campo.files?.[0] ?? null;
    campo.value = '';
    if (!arquivo) return;
    if (!['image/jpeg', 'image/png', 'image/gif'].includes(arquivo.type)) {
      this.erro = 'Escolha uma capa em JPG, PNG ou GIF.';
      return;
    }
    if (arquivo.size > 5 * 1024 * 1024) {
      this.erro = 'A capa deve ter no máximo 5 MB.';
      return;
    }

    this.limparPreviaCapa();
    this.arquivoCapa = arquivo;
    const url = URL.createObjectURL(arquivo);
    this.urlsTemporarias.add(url);
    this.previaCapa.set(url);
    this.erro = '';
  }

  protected enviarCapa(): void {
    if (!this.arquivoCapa || this.enviandoCapa || this.salvando || this.enviandoFoto) return;

    this.enviandoCapa = true;
    this.statusOperacaoCapa.set('Enviando capa profissional...');
    this.erro = '';
    this.sucesso = '';
    this.api.atualizarCapa(this.arquivoCapa).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => {
        this.enviandoCapa = false;
        this.statusOperacaoCapa.set('');
      }),
    ).subscribe({
      next: (perfil) => {
        this.perfil.set(perfil);
        this.arquivoCapa = null;
        this.limparPreviaCapa();
        this.sucesso = 'Capa do perfil atualizada.';
        this.carregarUsoArmazenamento();
      },
      error: () => (this.erro = 'Não foi possível enviar a capa. Tente novamente.'),
    });
  }

  protected removerCapa(): void {
    if (this.enviandoCapa || this.enviandoFoto || this.salvando || !this.perfil()?.capaUrl) return;

    this.enviandoCapa = true;
    this.statusOperacaoCapa.set('Removendo capa profissional...');
    this.erro = '';
    this.sucesso = '';
    this.api.removerCapa().pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => {
        this.enviandoCapa = false;
        this.statusOperacaoCapa.set('');
      }),
    ).subscribe({
      next: () => {
        this.perfil.update((perfil) => (perfil ? { ...perfil, capaUrl: null } : null));
        this.arquivoCapa = null;
        this.limparPreviaCapa();
        this.sucesso = 'Capa do perfil removida.';
      },
      error: () => (this.erro = 'Não foi possível remover a capa. Tente novamente.'),
    });
  }

  protected enviarFoto(): void {
    if (!this.arquivoFoto || this.enviandoFoto || this.enviandoCapa || this.salvando) return;

    this.enviandoFoto = true;
    this.statusOperacaoFoto.set('Enviando foto profissional...');
    this.erro = '';
    this.sucesso = '';
    this.api
      .atualizarFoto(this.arquivoFoto)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.enviandoFoto = false;
          this.statusOperacaoFoto.set('');
        }),
      )
      .subscribe({
        next: (perfil) => {
          this.perfil.set(perfil);
          this.arquivoFoto = null;
          this.limparPrevia();
          this.sucesso = 'Foto do perfil atualizada.';
          this.carregarUsoArmazenamento();
        },
        error: () => (this.erro = 'Não foi possível enviar a foto. Tente novamente.'),
      });
  }

  protected removerFoto(): void {
    if (this.enviandoFoto || this.enviandoCapa || this.salvando || !this.perfil()?.fotoUrl) return;

    this.enviandoFoto = true;
    this.statusOperacaoFoto.set('Removendo foto profissional...');
    this.erro = '';
    this.sucesso = '';
    this.api
      .removerFoto()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.enviandoFoto = false;
          this.statusOperacaoFoto.set('');
        }),
      )
      .subscribe({
        next: () => {
          this.perfil.update((perfil) => (perfil ? { ...perfil, fotoUrl: null } : null));
          this.arquivoFoto = null;
          this.limparPrevia();
          this.sucesso = 'Foto do perfil removida.';
        },
        error: () => (this.erro = 'Não foi possível remover a foto. Tente novamente.'),
      });
  }

  protected selecionarFotosPortfolio(evento: Event): void {
    const campo = evento.target as HTMLInputElement;
    const arquivos = Array.from(campo.files ?? []);
    campo.value = '';
    this.erroPortfolio = '';
    this.sucessoPortfolio = '';
    if (!arquivos.length) return;
    if (arquivos.length > 6) {
      this.erroPortfolio = 'Escolha até 6 fotos para cada publicação.';
      return;
    }
    if (arquivos.some((arquivo) => !['image/jpeg', 'image/png', 'image/gif'].includes(arquivo.type))) {
      this.erroPortfolio = 'Use somente imagens JPG, PNG ou GIF.';
      return;
    }
    if (arquivos.some((arquivo) => arquivo.size > 5 * 1024 * 1024)) {
      this.erroPortfolio = 'Cada foto deve ter no máximo 5 MB.';
      return;
    }
    this.limparPreviaPortfolio();
    this.arquivosPortfolio = arquivos;
    this.urlsFotosPortfolio = arquivos.map(() => null);
    this.previaFotosPortfolio.set(arquivos.map((arquivo) => {
      const url = URL.createObjectURL(arquivo);
      this.urlsTemporarias.add(url);
      return { arquivo, url };
    }));
  }

  protected publicarPortfolio(): void {
    if (!this.arquivosPortfolio.length || this.enviandoPortfolio || !this.perfil()) return;
    this.enviandoPortfolio = true;
    this.erroPortfolio = '';
    this.sucessoPortfolio = '';
    from(this.arquivosPortfolio.entries()).pipe(
      concatMap(([indice, arquivo]) => {
        const urlPersistida = this.urlsFotosPortfolio[indice];
        if (urlPersistida) return of({ url: urlPersistida });
        return this.api.enviarFotoPortfolio(arquivo).pipe(
          tap((upload) => this.urlsFotosPortfolio[indice] = upload.url),
        );
      }),
      toArray(),
      switchMap((uploads) => this.api.criarPublicacao(
        this.legendaPortfolio.trim(), uploads.map((upload) => upload.url),
        this.anuncioPortfolioId ? Number(this.anuncioPortfolioId) : undefined,
      )),
      takeUntilDestroyed(this.destroyRef),
      finalize(() => (this.enviandoPortfolio = false)),
    ).subscribe({
      next: () => {
        this.arquivosPortfolio = [];
        this.urlsFotosPortfolio = [];
        this.legendaPortfolio = '';
        this.anuncioPortfolioId = '';
        this.limparPreviaPortfolio();
        this.sucessoPortfolio = 'Publicação adicionada ao seu portfólio.';
        this.carregarPortfolio(this.perfil()!.id, 0, true);
      },
      error: () => {
        this.erroPortfolio = 'Não foi possível publicar as fotos. Confira sua conexão e tente novamente.';
        this.carregarUsoArmazenamento();
      },
      complete: () => this.carregarUsoArmazenamento(),
    });
  }

  protected formatarTamanho(bytes: number): string {
    if (bytes < 1024 * 1024) return `${Math.ceil(bytes / 1024)} KB`;
    return `${(bytes / (1024 * 1024)).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} MB`;
  }

  protected carregarUsoArmazenamento(): void {
    this.api.usoArmazenamento().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (uso) => {
        this.usoArmazenamento.set(uso);
        this.erroArmazenamento = '';
      },
      error: () => this.erroArmazenamento = 'Não foi possível consultar o armazenamento de imagens.',
    });
  }

  protected removerPublicacao(publicacao: PublicacaoPortfolio): void {
    if (this.removendoPublicacaoId !== null) return;
    this.removendoPublicacaoId = publicacao.id;
    this.erroPortfolio = '';
    this.sucessoPortfolio = '';
    this.api.excluirPublicacao(publicacao.id).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => (this.removendoPublicacaoId = null)),
    ).subscribe({
      next: () => {
        this.publicacoes.update((itens) => itens.filter((item) => item.id !== publicacao.id));
        const total = Math.max(0, this.totalPublicacoesPortfolio() - 1);
        this.totalPublicacoesPortfolio.set(total);
        this.totalPaginasPortfolio.set(Math.ceil(total / 12));
        if (!this.publicacoes().length && this.paginaPortfolio() > 0) {
          this.carregarPortfolio(this.perfil()!.id, this.paginaPortfolio() - 1, true);
        }
        this.sucessoPortfolio = 'Publicação removida.';
      },
      error: () => (this.erroPortfolio = 'Não foi possível remover esta publicação.'),
    });
  }

  protected iniciarEdicaoPublicacao(publicacao: PublicacaoPortfolio): void {
    this.editandoPublicacaoId.set(publicacao.id);
    this.legendaEmEdicao = publicacao.legenda;
    this.erroPortfolio = '';
    this.sucessoPortfolio = '';
  }

  protected cancelarEdicaoPublicacao(): void {
    this.editandoPublicacaoId.set(null);
    this.legendaEmEdicao = '';
  }

  protected salvarEdicaoPublicacao(publicacao: PublicacaoPortfolio): void {
    if (this.salvandoPublicacaoId() !== null || this.legendaEmEdicao.length > 500) return;
    this.salvandoPublicacaoId.set(publicacao.id);
    this.erroPortfolio = '';
    this.sucessoPortfolio = '';
    this.api.atualizarLegendaPublicacao(publicacao.id, this.legendaEmEdicao.trim()).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.salvandoPublicacaoId.set(null)),
    ).subscribe({
      next: (atualizada) => {
        this.publicacoes.update((itens) => itens.map((item) => item.id === atualizada.id ? atualizada : item));
        this.cancelarEdicaoPublicacao();
        this.sucessoPortfolio = 'Legenda da publicação atualizada.';
      },
      error: () => this.erroPortfolio = 'Não foi possível atualizar a legenda. Tente novamente.',
    });
  }

  protected mudarPaginaPortfolio(pagina: number): void {
    const perfil = this.perfil();
    if (!perfil || this.carregandoPortfolio || pagina < 0 || pagina >= this.totalPaginasPortfolio()) return;
    this.carregarPortfolio(perfil.id, pagina, true);
  }

  private preencher(perfil: PerfilProfissional): void {
    this.perfil.set(perfil);
    this.dados = {
      nome: perfil.nome,
      tituloProfissional: perfil.tituloProfissional ?? '',
      bio: perfil.bio ?? '',
      cidade: perfil.cidade ?? '',
      especialidades: perfil.especialidades ?? '',
      linkedinUrl: perfil.linkedinUrl ?? '',
      instagramUrl: perfil.instagramUrl ?? '',
      siteUrl: perfil.siteUrl ?? '',
      perfilPublico: perfil.perfilPublico !== false,
      disponivelTrabalhos: perfil.disponivelTrabalhos === true,
    };
    this.carregarPortfolio(perfil.id);
    this.carregarOfertasPortfolio(perfil.id);
  }

  private carregarOfertasPortfolio(id: number): void {
    if (this.ofertasCarregadasParaId === id) return;
    this.ofertasCarregadasParaId = id;
    this.anunciosApi.listarDoUsuario(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (pagina) => this.ofertasPortfolio.set(pagina.itens.filter((anuncio) => !anuncio.exemplo)),
      error: () => {
        this.ofertasCarregadasParaId = null;
        this.ofertasPortfolio.set([]);
      },
    });
  }

  private carregarPortfolio(id: number, pagina = 0, forcar = false): void {
    if (!forcar && this.portfolioCarregadoParaId === id) return;
    if (pagina === 0) this.portfolioCarregadoParaId = id;
    this.erroPortfolio = '';
    this.carregandoPortfolio = true;
    this.api.minhasPublicacoes(pagina).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => (this.carregandoPortfolio = false)),
    ).subscribe({
      next: (resultado) => {
        this.publicacoes.set(resultado.itens);
        this.paginaPortfolio.set(resultado.pagina);
        this.totalPaginasPortfolio.set(resultado.totalPaginas);
        this.totalPublicacoesPortfolio.set(resultado.totalItens);
      },
      error: () => {
        if (pagina === 0) this.portfolioCarregadoParaId = null;
        this.erroPortfolio = 'Não foi possível carregar seu portfólio.';
      },
    });
  }

  private limparPrevia(): void {
    const atual = this.previaFoto();
    if (atual?.startsWith('blob:')) {
      URL.revokeObjectURL(atual);
      this.urlsTemporarias.delete(atual);
    }
    this.previaFoto.set(null);
  }

  private limparPreviaCapa(): void {
    const atual = this.previaCapa();
    if (atual?.startsWith('blob:')) {
      URL.revokeObjectURL(atual);
      this.urlsTemporarias.delete(atual);
    }
    this.previaCapa.set(null);
  }

  private limparPreviaPortfolio(): void {
    for (const { url } of this.previaFotosPortfolio()) {
      URL.revokeObjectURL(url);
      this.urlsTemporarias.delete(url);
    }
    this.previaFotosPortfolio.set([]);
  }
}
