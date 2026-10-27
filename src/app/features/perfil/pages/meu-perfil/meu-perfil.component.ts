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
  protected readonly previaFoto = signal<string | null>(null);
  protected dados: AtualizacaoPerfil = { ...formularioVazio };
  protected arquivoFoto: File | null = null;
  protected carregando = false;
  protected salvando = false;
  protected enviandoFoto = false;
  protected erro = '';
  protected sucesso = '';

  private readonly urlsTemporarias = new Set<string>();

  constructor() {
    this.destroyRef.onDestroy(() => {
      for (const url of this.urlsTemporarias) URL.revokeObjectURL(url);
    });
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

  protected enviarFoto(): void {
    if (!this.arquivoFoto || this.enviandoFoto) return;

    this.enviandoFoto = true;
    this.erro = '';
    this.sucesso = '';
    this.api
      .atualizarFoto(this.arquivoFoto)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => (this.enviandoFoto = false)),
      )
      .subscribe({
        next: (perfil) => {
          this.perfil.set(perfil);
          this.arquivoFoto = null;
          this.limparPrevia();
          this.sucesso = 'Foto do perfil atualizada.';
        },
        error: () => (this.erro = 'Não foi possível enviar a foto. Tente novamente.'),
      });
  }

  protected removerFoto(): void {
    if (this.enviandoFoto || !this.perfil()?.fotoUrl) return;

    this.enviandoFoto = true;
    this.erro = '';
    this.sucesso = '';
    this.api
      .removerFoto()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => (this.enviandoFoto = false)),
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

  private preencher(perfil: PerfilProfissional): void {
    this.perfil.set(perfil);
    this.dados = {
      nome: perfil.nome,
      bio: perfil.bio ?? '',
      cidade: perfil.cidade ?? '',
      especialidades: perfil.especialidades ?? '',
    };
  }

  private limparPrevia(): void {
    const atual = this.previaFoto();
    if (atual?.startsWith('blob:')) {
      URL.revokeObjectURL(atual);
      this.urlsTemporarias.delete(atual);
    }
    this.previaFoto.set(null);
  }
}
