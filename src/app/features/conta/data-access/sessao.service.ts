import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpBackend, HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, finalize, map, of, shareReplay, tap, throwError, timeout } from 'rxjs';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class SessaoService {
  // A restauração não passa pelo interceptor que a solicita.
  private readonly http = new HttpClient(inject(HttpBackend));
  private readonly jwt = signal<string | null>(null);
  private renovacao: Observable<void> | null = null;
  private geracao = 0;
  private readonly opcoes = { headers: { 'X-SkillSwap-Session': '1' } };
  readonly autenticado = computed(() => this.jwt() !== null);
  readonly saindo = signal(false);
  readonly erroSaida = signal('');

  credencial(): string | null { return this.jwt(); }

  restaurar(): Observable<void> {
    return this.renovar().pipe(catchError(() => of(undefined)));
  }

  renovar(): Observable<void> {
    if (this.renovacao) return this.renovacao;
    const geracao = this.geracao;
    this.renovacao = this.http.post<{ token: string } | null>('/api/sessoes/atual', {}, this.opcoes).pipe(
      timeout(10000),
      tap(resposta => {
        if (geracao !== this.geracao) return;
        if (resposta !== null && (typeof resposta.token !== 'string' || !resposta.token))
          throw new Error('Resposta inválida da sessão.');
        this.jwt.set(resposta?.token ?? null);
      }),
      map(() => undefined),
      catchError(erro => {
        if (geracao === this.geracao && erro instanceof HttpErrorResponse && erro.status === 401)
          this.jwt.set(null);
        return throwError(() => erro);
      }),
      finalize(() => this.renovacao = null),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.renovacao;
  }

  sair(): void {
    if (this.saindo()) return;
    this.saindo.set(true);
    this.erroSaida.set('');
    ++this.geracao;
    this.http.delete('/api/sessoes/atual', this.opcoes).pipe(
      timeout(10000), finalize(() => this.saindo.set(false)),
    ).subscribe({
      next: () => this.jwt.set(null),
      error: () => this.erroSaida.set('Não foi possível encerrar a sessão. Tente sair novamente.'),
    });
  }

  entrar(email: string, senha: string) {
    return this.autenticar('/sessoes', { email: email.trim().toLowerCase(), senha });
  }
  registrar(nome: string, email: string, senha: string) {
    return this.autenticar('/usuarios', { nome: nome.trim(), email: email.trim().toLowerCase(), senha });
  }
  private autenticar(rota: string, dados: object) {
    return this.http.post<{ token: string }>(environment.apiUrl + rota, dados, this.opcoes).pipe(
      tap(resposta => {
        if (typeof resposta.token !== 'string' || !resposta.token) throw new Error('Resposta inválida da sessão.');
        ++this.geracao;
        this.jwt.set(resposta.token);
      }),
      map(() => undefined),
    );
  }
}
