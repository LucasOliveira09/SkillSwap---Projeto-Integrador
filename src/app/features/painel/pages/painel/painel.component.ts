import { Component, computed, inject, signal } from '@angular/core';
import { catchError, of } from 'rxjs';
import { ModeracaoApiService } from '../../../moderacao/data-access/moderacao-api.service';
import { PainelApiService } from '../../data-access/painel-api.service';
import { PainelPlataforma, PainelUsuario } from '../../models/painel.model';

interface Indicador {
  rotulo: string;
  valor: string;
  detalhe: string;
  link?: string;
}

const ROTULOS_STATUS: Record<string, string> = {
  PENDENTE: 'Pendentes',
  ACEITA: 'Aceitas',
  RECUSADA: 'Recusadas',
  CANCELADA: 'Canceladas',
  EM_ANDAMENTO: 'Em andamento',
  CONCLUIDO: 'Concluídos',
  CANCELADO: 'Cancelados',
  MANUTENCAO: 'Manutenção e reparos',
  AULAS: 'Aulas e instrução',
  TRANSPORTE: 'Transporte local',
};

/** Dashboard operacional: KPIs calculados no backend a partir dos dados persistidos. */
@Component({
  standalone: false,
  selector: 'app-painel',
  templateUrl: './painel.component.html',
  styleUrl: './painel.component.scss',
})
export class PainelComponent {
  private readonly api = inject(PainelApiService);
  private readonly moderacao = inject(ModeracaoApiService);

  protected readonly carregando = signal(true);
  protected readonly erro = signal(false);
  protected readonly painel = signal<PainelUsuario | null>(null);
  protected readonly plataforma = signal<PainelPlataforma | null>(null);

  protected readonly indicadores = computed<Indicador[]>(() => {
    const p = this.painel();
    if (!p) return [];
    return [
      {
        rotulo: 'Anúncios ativos',
        valor: String(p.anunciosAtivos),
        detalhe: `${p.anunciosPausados} pausado(s)`,
        link: '/anuncios/minhas',
      },
      {
        rotulo: 'Propostas aguardando você',
        valor: String(p.propostasRecebidasPendentes),
        detalhe: `${p.propostasEnviadasPendentes} enviada(s) aguardando resposta`,
        link: '/mensagens',
      },
      {
        rotulo: 'Acordos em andamento',
        valor: String(p.acordosEmAndamento),
        detalhe: `${p.acordosConcluidos} concluído(s)`,
        link: '/mensagens',
      },
      {
        rotulo: 'Taxa de aceitação',
        valor: p.taxaAceitacao === null ? '—' : `${this.numero(p.taxaAceitacao)}%`,
        detalhe: 'das propostas recebidas e respondidas',
      },
      {
        rotulo: 'Nota média recebida',
        valor: p.notaMedia === null ? '—' : this.numero(p.notaMedia),
        detalhe: `${p.avaliacoesRecebidas} avaliação(ões) verificada(s)`,
      },
    ];
  });

  protected readonly distribuicoes = computed(() => {
    const p = this.plataforma();
    if (!p) return [];
    return [
      { titulo: 'Propostas por situação', linhas: this.linhas(p.propostasPorStatus) },
      { titulo: 'Acordos por situação', linhas: this.linhas(p.acordosPorStatus) },
      { titulo: 'Anúncios ativos por categoria', linhas: this.linhas(p.anunciosAtivosPorCategoria) },
    ];
  });

  constructor() {
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erro.set(false);
    this.api.meuPainel().subscribe({
      next: (painel) => {
        this.painel.set(painel);
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set(true);
        this.carregando.set(false);
      },
    });
    this.moderacao
      .acesso()
      .pipe(catchError(() => of(false)))
      .subscribe((admin) => {
        if (!admin) return;
        this.api.plataforma().pipe(catchError(() => of(null))).subscribe((p) => this.plataforma.set(p));
      });
  }

  protected linhas(mapa: Record<string, number>): { rotulo: string; total: number; percentual: number }[] {
    const entradas = Object.entries(mapa);
    const soma = entradas.reduce((acc, [, total]) => acc + total, 0);
    return entradas.map(([chave, total]) => ({
      rotulo: ROTULOS_STATUS[chave] ?? chave,
      total,
      percentual: soma === 0 ? 0 : Math.round((100 * total) / soma),
    }));
  }

  protected numero(valor: number): string {
    return valor.toLocaleString('pt-BR', { maximumFractionDigits: 1 });
  }
}
