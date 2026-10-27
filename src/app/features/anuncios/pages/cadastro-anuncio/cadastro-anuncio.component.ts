import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { CurrencyPipe } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { AnunciosApiService } from '../../data-access/anuncios-api.service';
import { DadosAnuncio } from '../../models/anuncio.model';
import { CategoriaApi } from '../../models/anuncio-api.model';
import { validarAnuncio } from '../../models/anuncio.validation';
import { IconeComponent } from '../../../../shared/ui/icone/icone.component';
import { SessaoService } from '../../../conta/data-access/sessao.service';

@Component({
  selector: 'app-cadastro-anuncio',
  standalone: true,
  imports: [
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    CurrencyPipe,
    IconeComponent,
  ],
  templateUrl: './cadastro-anuncio.component.html',
  styleUrl: './cadastro-anuncio.component.scss',
})
export class CadastroAnuncioComponent {
  protected readonly sessao = inject(SessaoService);
  private readonly anuncios = inject(AnunciosApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly categorias = signal<readonly CategoriaApi[]>([]);
  protected dados = this.novoFormulario();
  protected mensagem = '';
  protected erro = '';
  protected erroCategorias = false;
  protected carregandoCategorias = true;
  protected salvando = false;
  protected readonly anuncioId = signal<number | null>(null);
  protected readonly carregandoAnuncio = signal(false);
  protected readonly enviandoImagem = signal(false);
  protected readonly erroUpload = signal<string | null>(null);

  constructor() {
    this.carregarCategorias();
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((parametros) => {
      const id = parametros.get('id');
      if (!id) return;
      const numero = Number(id);
      if (!Number.isSafeInteger(numero) || numero < 1) {
        this.erro = 'Anúncio inválido.';
        return;
      }
      if (!this.sessao.autenticado()) {
        void this.router.navigate(['/conta'], { queryParams: { retorno: `/anuncios/${numero}/editar` } });
        return;
      }
      this.anuncioId.set(numero);
      this.carregarAnuncio(numero);
    });
  }

  private carregarAnuncio(id: number): void {
    this.carregandoAnuncio.set(true);
    this.erro = '';
    this.anuncios.obterMeu(id).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.carregandoAnuncio.set(false)),
    ).subscribe({
      next: (anuncio) => {
        this.dados = {
          titulo: anuncio.titulo,
          descricao: anuncio.descricao,
          categoria: anuncio.categoria,
          cidade: anuncio.cidade,
          modalidade: anuncio.modalidade,
          preco: anuncio.preco,
          habilidadeDesejada: anuncio.habilidadeDesejada || '',
          imagem: anuncio.imagem,
        };
      },
      error: () => this.erro = 'Não foi possível carregar este anúncio. Confira se ele pertence à sua conta.',
    });
  }

  protected carregarCategorias(): void {
    this.erroCategorias = false;
    this.carregandoCategorias = true;
    this.anuncios
      .categorias()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => (this.carregandoCategorias = false)),
      )
      .subscribe({
        next: (categorias) => {
          this.categorias.set(categorias);
          if (!categorias.some((c) => c.valor === this.dados.categoria)) {
            this.dados.categoria = categorias[0]?.valor || '';
          }
          this.erroCategorias = !categorias.length;
        },
        error: () => (this.erroCategorias = true),
      });
  }

  protected get erroValidacao(): string | null {
    return validarAnuncio(this.dados, this.categorias());
  }

  protected get categoriaSelecionada(): string {
    return (
      this.categorias().find((categoria) => categoria.valor === this.dados.categoria)?.rotulo ||
      'Selecione uma categoria'
    );
  }

  protected salvar(form: NgForm): void {
    if (!this.sessao.autenticado()) return;
    if (this.salvando || this.carregandoCategorias || this.carregandoAnuncio() || this.erroCategorias || this.mensagem) return;
    if (form.invalid || this.erroValidacao) {
      form.control.markAllAsTouched();
      return;
    }
    this.salvando = true;
    this.erro = '';
    const salvar = this.anuncioId()
      ? this.anuncios.atualizar(this.anuncioId()!, { ...this.dados })
      : this.anuncios.criar({ ...this.dados });
    salvar
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => (this.salvando = false)),
      )
      .subscribe({
        next: (anuncio) => {
          if (this.anuncioId()) {
            void this.router.navigate(['/anuncios/minhas'], { queryParams: { atualizado: anuncio.id } });
          } else {
            this.mensagem = `Anúncio "${anuncio.titulo}" salvo no banco de dados.`;
          }
        },
        error: () =>
          (this.erro =
            'Não foi possível salvar o anúncio. Os campos foram mantidos; revise e tente novamente.'),
      });
  }

  protected selecionarArquivo(event: Event): void {
    const input = event.target as HTMLInputElement;
    const arquivo = input.files?.[0];
    if (!arquivo) return;

    if (arquivo.size > 5 * 1024 * 1024) {
      this.erroUpload.set('A imagem excede o tamanho máximo de 5 MB.');
      input.value = '';
      return;
    }

    this.enviandoImagem.set(true);
    this.erroUpload.set(null);

    this.anuncios
      .uploadImagem(arquivo)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.enviandoImagem.set(false)),
      )
      .subscribe({
        next: (res) => {
          this.dados.imagem = res.url;
        },
        error: (err) => {
          this.erroUpload.set(
            err?.error?.mensagem || 'Falha ao enviar a imagem. Tente novamente.',
          );
        },
      });
  }

  protected removerImagem(): void {
    this.dados.imagem = undefined;
    this.erroUpload.set(null);
  }

  protected limpar(form: NgForm): void {
    if (this.salvando) return;
    this.dados = this.novoFormulario();
    form.resetForm(this.dados);
    this.mensagem = '';
    this.erro = '';
    this.erroUpload.set(null);
  }

  private novoFormulario(): DadosAnuncio {
    return {
      titulo: '',
      descricao: '',
      cidade: '',
      categoria: this.categorias()[0]?.valor || '',
      modalidade: 'venda',
      preco: null,
      habilidadeDesejada: '',
      imagem: undefined,
    };
  }
}
