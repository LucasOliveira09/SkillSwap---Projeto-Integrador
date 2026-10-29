import { DOCUMENT } from '@angular/common';
import { Injector, effect, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, exhaustMap, filter, merge, of, Subject, timer } from 'rxjs';
import { SessaoService } from '../../conta/data-access/sessao.service';

@Injectable({ providedIn: 'root' })
export class IndicadorMensagensService {
  private readonly http = inject(HttpClient);
  private readonly sessao = inject(SessaoService);
  private readonly document = inject(DOCUMENT);
  private readonly injector = inject(Injector);
  private readonly repetir = new Subject<void>();
  private iniciado = false;
  readonly total = signal<number | null>(null);

  iniciar(): void {
    if (this.iniciado) return;
    this.iniciado = true;
    effect(onCleanup => {
      this.total.set(null);
      if (!this.sessao.autenticado()) return;
      const assinatura = merge(timer(0, 8000), this.repetir).pipe(
        filter(() => this.document.visibilityState === 'visible'),
        exhaustMap(() => this.http.get<{ naoLidas: number }>('/api/conversas/resumo').pipe(catchError(() => of(null)))),
      ).subscribe(resumo => { if (resumo) this.total.set(resumo.naoLidas); });
      onCleanup(() => assinatura.unsubscribe());
    }, { injector: this.injector });
  }
  atualizar(): void { this.repetir.next(); }
}
