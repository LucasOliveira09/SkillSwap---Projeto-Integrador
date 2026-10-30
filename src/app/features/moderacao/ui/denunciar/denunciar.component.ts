import { Component, input } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { SessaoService } from '../../../conta/data-access/sessao.service';
import { ModeracaoApiService } from '../../data-access/moderacao-api.service';
import { MotivoDenuncia, TipoConteudoDenunciado } from '../../models/moderacao.model';

@Component({
  selector: 'app-denunciar',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './denunciar.component.html',
  styleUrl: './denunciar.component.scss',
})
export class DenunciarComponent {
  readonly tipoConteudo = input.required<TipoConteudoDenunciado>();
  readonly recursoId = input.required<number>();

  protected readonly motivos: readonly { valor: MotivoDenuncia; rotulo: string }[] = [
    { valor: 'SPAM', rotulo: 'Spam ou divulgação repetitiva' },
    { valor: 'ASSEDIO', rotulo: 'Assédio ou discriminação' },
    { valor: 'FRAUDE', rotulo: 'Tentativa de fraude' },
    { valor: 'DADOS_PESSOAIS', rotulo: 'Exposição de dados pessoais' },
    { valor: 'CONTEUDO_INADEQUADO', rotulo: 'Conteúdo impróprio' },
    { valor: 'OUTRO', rotulo: 'Outro motivo' },
  ];
  protected aberto = false;
  protected motivo: MotivoDenuncia = 'OUTRO';
  protected descricao = '';
  protected enviando = false;
  protected enviado = false;
  protected erro = '';

  constructor(
    private readonly api: ModeracaoApiService,
    protected readonly sessao: SessaoService,
    private readonly router: Router,
  ) {}

  protected get retorno(): string {
    return this.router.url;
  }

  protected enviar(): void {
    if (this.enviando || this.enviado) return;
    this.enviando = true;
    this.erro = '';
    this.api.denunciar(this.tipoConteudo(), this.recursoId(), this.motivo, this.descricao.trim()).subscribe({
      next: () => {
        this.enviado = true;
        this.aberto = false;
      },
      error: (erro: unknown) => {
        this.erro = erro instanceof HttpErrorResponse && erro.status === 409
          ? 'Você já enviou uma denúncia sobre este conteúdo.'
          : 'Não foi possível enviar a denúncia. Tente novamente.';
        this.enviando = false;
      },
      complete: () => this.enviando = false,
    });
  }
}
