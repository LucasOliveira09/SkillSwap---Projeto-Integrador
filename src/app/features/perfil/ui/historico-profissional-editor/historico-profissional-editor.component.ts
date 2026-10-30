import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { FormsModule, NgForm } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { PerfilApiService } from '../../data-access/perfil-api.service';
import { CertificacaoProfissional, ExperienciaProfissional } from '../../models/perfil.model';

type ExperienciaForm = Omit<ExperienciaProfissional, 'id'>;
type CertificacaoForm = Omit<CertificacaoProfissional, 'id'>;

const experienciaVazia: ExperienciaForm = {
  cargo: '', organizacao: '', cidade: '', dataInicio: '', dataFim: null, descricao: '',
};
const certificacaoVazia: CertificacaoForm = {
  nome: '', emissor: '', dataEmissao: '', dataValidade: null, urlCredencial: '',
};

@Component({
  selector: 'app-historico-profissional-editor',
  standalone: true,
  imports: [FormsModule, DatePipe, MatButtonModule],
  templateUrl: './historico-profissional-editor.component.html',
  styleUrl: './historico-profissional-editor.component.scss',
})
export class HistoricoProfissionalEditorComponent {
  private readonly api = inject(PerfilApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly experiencias = signal<readonly ExperienciaProfissional[]>([]);
  protected readonly certificacoes = signal<readonly CertificacaoProfissional[]>([]);
  protected carregando = true;
  protected erro = '';
  protected mensagem = '';
  protected salvandoExperiencia = false;
  protected salvandoCertificacao = false;
  protected removendoExperienciaId: number | null = null;
  protected removendoCertificacaoId: number | null = null;
  protected readonly editandoExperienciaId = signal<number | null>(null);
  protected readonly editandoCertificacaoId = signal<number | null>(null);
  protected experiencia: ExperienciaForm = { ...experienciaVazia };
  protected certificacao: CertificacaoForm = { ...certificacaoVazia };

  constructor() { this.carregar(); }

  protected carregar(): void {
    this.carregando = true;
    this.erro = '';
    forkJoin({
      experiencias: this.api.minhasExperiencias().pipe(catchError(() => of(null))),
      certificacoes: this.api.minhasCertificacoes().pipe(catchError(() => of(null))),
    }).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.carregando = false))
      .subscribe(({ experiencias, certificacoes }) => {
        if (experiencias === null || certificacoes === null) {
          this.erro = 'Não foi possível carregar sua trajetória. Tente novamente.';
          return;
        }
        this.experiencias.set(experiencias);
        this.certificacoes.set(certificacoes);
      });
  }

  protected salvarExperiencia(form: NgForm): void {
    if (form.invalid || this.salvandoExperiencia) { form.control.markAllAsTouched(); return; }
    if (this.experiencia.dataFim && this.experiencia.dataFim < this.experiencia.dataInicio) {
      this.erro = 'A data final deve ser igual ou posterior à data inicial.';
      return;
    }
    this.salvandoExperiencia = true;
    this.erro = '';
    this.mensagem = '';
    const dados: ExperienciaForm = {
      ...this.experiencia,
      cargo: this.experiencia.cargo.trim(), organizacao: this.experiencia.organizacao.trim(),
      cidade: this.experiencia.cidade?.trim() || null, dataFim: this.experiencia.dataFim || null,
      descricao: this.experiencia.descricao?.trim() || null,
    };
    const id = this.editandoExperienciaId();
    const requisicao = id ? this.api.atualizarExperiencia(id, dados) : this.api.criarExperiencia(dados);
    requisicao.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.salvandoExperiencia = false))
      .subscribe({
        next: item => {
          this.experiencias.update(itens => [item, ...itens.filter(experiencia => experiencia.id !== item.id)]
            .sort((a, b) => b.dataInicio.localeCompare(a.dataInicio)));
          this.limparExperiencia();
          this.mensagem = 'Experiência salva.';
        },
        error: () => this.erro = 'Não foi possível salvar a experiência. Confira as datas e tente novamente.',
      });
  }

  protected editarExperiencia(item: ExperienciaProfissional): void {
    this.editandoExperienciaId.set(item.id);
    this.experiencia = { ...item, cidade: item.cidade ?? '', dataFim: item.dataFim, descricao: item.descricao ?? '' };
    this.erro = '';
    this.mensagem = '';
  }

  protected limparExperiencia(): void {
    this.experiencia = { ...experienciaVazia };
    this.editandoExperienciaId.set(null);
  }

  protected excluirExperiencia(item: ExperienciaProfissional): void {
    if (this.removendoExperienciaId !== null) return;
    this.removendoExperienciaId = item.id;
    this.erro = '';
    this.api.excluirExperiencia(item.id).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.removendoExperienciaId = null))
      .subscribe({
        next: () => {
          this.experiencias.update(itens => itens.filter(experiencia => experiencia.id !== item.id));
          if (this.editandoExperienciaId() === item.id) this.limparExperiencia();
          this.mensagem = 'Experiência removida.';
        },
        error: () => this.erro = 'Não foi possível remover a experiência.',
      });
  }

  protected salvarCertificacao(form: NgForm): void {
    if (form.invalid || this.salvandoCertificacao) { form.control.markAllAsTouched(); return; }
    if (this.certificacao.dataValidade && this.certificacao.dataValidade < this.certificacao.dataEmissao) {
      this.erro = 'A validade deve ser igual ou posterior à emissão.';
      return;
    }
    this.salvandoCertificacao = true;
    this.erro = '';
    this.mensagem = '';
    const dados: CertificacaoForm = {
      ...this.certificacao, nome: this.certificacao.nome.trim(), emissor: this.certificacao.emissor.trim(),
      dataValidade: this.certificacao.dataValidade || null, urlCredencial: this.certificacao.urlCredencial?.trim() || null,
    };
    const id = this.editandoCertificacaoId();
    const requisicao = id ? this.api.atualizarCertificacao(id, dados) : this.api.criarCertificacao(dados);
    requisicao.pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.salvandoCertificacao = false))
      .subscribe({
        next: item => {
          this.certificacoes.update(itens => [item, ...itens.filter(certificacao => certificacao.id !== item.id)]
            .sort((a, b) => b.dataEmissao.localeCompare(a.dataEmissao)));
          this.limparCertificacao();
          this.mensagem = 'Certificação salva.';
        },
        error: () => this.erro = 'Não foi possível salvar a certificação. Confira os dados e use um link HTTPS.',
      });
  }

  protected editarCertificacao(item: CertificacaoProfissional): void {
    this.editandoCertificacaoId.set(item.id);
    this.certificacao = { ...item, dataValidade: item.dataValidade, urlCredencial: item.urlCredencial ?? '' };
    this.erro = '';
    this.mensagem = '';
  }

  protected limparCertificacao(): void {
    this.certificacao = { ...certificacaoVazia };
    this.editandoCertificacaoId.set(null);
  }

  protected excluirCertificacao(item: CertificacaoProfissional): void {
    if (this.removendoCertificacaoId !== null) return;
    this.removendoCertificacaoId = item.id;
    this.erro = '';
    this.api.excluirCertificacao(item.id).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.removendoCertificacaoId = null))
      .subscribe({
        next: () => {
          this.certificacoes.update(itens => itens.filter(certificacao => certificacao.id !== item.id));
          if (this.editandoCertificacaoId() === item.id) this.limparCertificacao();
          this.mensagem = 'Certificação removida.';
        },
        error: () => this.erro = 'Não foi possível remover a certificação.',
      });
  }
}
