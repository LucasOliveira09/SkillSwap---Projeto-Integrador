import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { CurrencyPipe } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';
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
  private readonly destroyRef = inject(DestroyRef);
  protected readonly categorias = signal<readonly CategoriaApi[]>([]);
  protected dados = this.novoFormulario();
  protected mensagem = '';
  protected erro = '';
  protected erroCategorias = false;
  protected carregandoCategorias = true;
  protected salvando = false;

  constructor() {
    this.carregarCategorias();
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
    if (this.salvando || this.carregandoCategorias || this.erroCategorias || this.mensagem) return;
    if (form.invalid || this.erroValidacao) {
      form.control.markAllAsTouched();
      return;
    }
    this.salvando = true;
    this.erro = '';
    this.anuncios
      .criar({ ...this.dados })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => (this.salvando = false)),
      )
      .subscribe({
        next: (anuncio) => (this.mensagem = `Anúncio "${anuncio.titulo}" salvo no banco de dados.`),
        error: () =>
          (this.erro =
            'Não foi possível salvar o anúncio. Os campos foram mantidos; revise e tente novamente.'),
      });
  }

  protected limpar(form: NgForm): void {
    if (this.salvando) return;
    this.dados = this.novoFormulario();
    form.resetForm(this.dados);
    this.mensagem = '';
    this.erro = '';
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
    };
  }
}
