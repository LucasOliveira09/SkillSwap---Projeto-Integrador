import { Component, inject } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { AnunciosDemoService } from '../../data-access/anuncios-demo.service';
import { CATEGORIAS_ANUNCIO, DadosAnuncio } from '../../models/anuncio.model';
import { validarAnuncio } from '../../models/anuncio.validation';
import { IconeComponent } from '../../../../shared/ui/icone/icone.component';

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
  private readonly anuncios = inject(AnunciosDemoService);
  protected readonly categorias = CATEGORIAS_ANUNCIO;
  protected dados = this.novoFormulario();
  protected mensagem = '';

  protected get erroValidacao(): string | null {
    return validarAnuncio(this.dados);
  }

  protected get categoriaSelecionada(): string {
    return this.categorias.find((categoria) => categoria.valor === this.dados.categoria)!.rotulo;
  }

  protected salvar(form: NgForm): void {
    if (form.invalid || this.erroValidacao) {
      form.control.markAllAsTouched();
      return;
    }
    const anuncio = this.anuncios.criar(this.dados);
    this.mensagem = `Simulação: "${anuncio.titulo}" adicionado ao catálogo desta sessão. Não houve publicação real.`;
  }

  protected limpar(form: NgForm): void {
    this.dados = this.novoFormulario();
    form.resetForm(this.dados);
    this.mensagem = '';
  }

  private novoFormulario(): DadosAnuncio {
    return {
      titulo: '',
      descricao: '',
      cidade: '',
      categoria: 'manutencao',
      modalidade: 'venda',
      preco: null,
      habilidadeDesejada: '',
    };
  }
}
