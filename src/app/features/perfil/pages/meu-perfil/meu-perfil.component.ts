import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { SessaoService } from '../../../conta/data-access/sessao.service';
import { AtualizacaoPerfil, PerfilProfissional } from '../../models/perfil.model';
import { PerfilApiService } from '../../data-access/perfil-api.service';

const formularioVazio: AtualizacaoPerfil = {
  nome: '',
  bio: '',
  cidade: '',
  especialidades: '',
};

@Component({
  selector: 'app-meu-perfil',
  standalone: true,
  imports: [FormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './meu-perfil.component.html',
  styleUrl: './meu-perfil.component.scss',
})
export class MeuPerfilComponent {
  private readonly api = inject(PerfilApiService);
  private readonly sessao = inject(SessaoService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly perfil = signal<PerfilProfissional | null>(null);
  protected dados: AtualizacaoPerfil = { ...formularioVazio };
  protected carregando = false;
  protected salvando = false;
  protected erro = '';
  protected sucesso = '';

  constructor() {
    if (!this.sessao.autenticado()) {
      void this.router.navigate(['/conta'], { queryParams: { retorno: '/meu-perfil' } });
      return;
    }
    this.carregar();
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
    if (this.salvando || form.invalid) {
      form.control.markAllAsTouched();
      return;
    }

    this.salvando = true;
    this.erro = '';
    this.sucesso = '';
    const dados = {
      nome: this.dados.nome.trim(),
      bio: this.dados.bio.trim(),
      cidade: this.dados.cidade.trim(),
      especialidades: this.dados.especialidades.trim(),
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
          this.sucesso = 'Perfil salvo.';
        },
        error: () => (this.erro = 'Não foi possível salvar. Seus dados continuam neste formulário.'),
      });
  }

  private preencher(perfil: PerfilProfissional): void {
    this.perfil.set(perfil);
    this.dados = {
      nome: perfil.nome,
      bio: perfil.bio ?? '',
      cidade: perfil.cidade ?? '',
      especialidades: perfil.especialidades ?? '',
    };
  }
}
