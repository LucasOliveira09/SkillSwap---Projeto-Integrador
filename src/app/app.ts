import { Component, Injector, effect, inject, signal } from '@angular/core';
import {
  ActivatedRoute,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatSidenavModule } from '@angular/material/sidenav';
import { IconeComponent } from './shared/ui/icone/icone.component';
import { SessaoService } from './features/conta/data-access/sessao.service';
import { ModeracaoApiService } from './features/moderacao/data-access/moderacao-api.service';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatSidenavModule,
    IconeComponent,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly sessao = inject(SessaoService);
  protected readonly moderador = signal(false);
  private readonly moderacaoApi = inject(ModeracaoApiService);
  private readonly verificarAcessoModeracao = effect((onCleanup) => {
    if (!this.sessao.credencial()) {
      this.moderador.set(false);
      return;
    }

    this.moderador.set(false);
    const subscription = this.moderacaoApi
      .acesso()
      .pipe(catchError(() => of(false)))
      .subscribe((autorizado) => this.moderador.set(autorizado));
    onCleanup(() => subscription.unsubscribe());
  });
  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  private readonly route = inject(ActivatedRoute);
  protected readonly parametros = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });

  protected get termoDeBuscaAtual(): string {
    const rotaAtual = this.router.url.split('?')[0];
    return rotaAtual.startsWith('/anuncios/') || rotaAtual === '/anuncios'
      ? (this.parametros().get('busca') ?? '')
      : '';
  }

  protected pesquisar(event: Event, termo: string): void {
    event.preventDefault();
    void this.router.navigate(['/anuncios'], { queryParams: { busca: termo.trim() || null } });
  }

  protected abrirConta(): void {
    void this.router.navigate(['/conta']);
  }

  protected async informar(titulo: string, mensagem: string): Promise<void> {
    const [{ MatDialog }, { RecursoPlanejadoComponent }] = await Promise.all([
      import('@angular/material/dialog'),
      import('./shared/ui/recurso-planejado/recurso-planejado.component'),
    ]);
    this.injector.get(MatDialog).open(RecursoPlanejadoComponent, {
      data: { titulo, mensagem },
      width: '420px',
      maxWidth: 'calc(100vw - 32px)',
    });
  }
}
