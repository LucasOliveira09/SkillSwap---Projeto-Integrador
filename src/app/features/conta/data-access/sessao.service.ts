import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, tap } from 'rxjs';
import { environment } from '../../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class SessaoService {
  private readonly http = inject(HttpClient);
  private readonly jwt = signal<string | null>(null);
  readonly autenticado = computed(() => this.jwt() !== null);

  credencial(): string | null {
    return this.jwt();
  }
  sair(): void {
    this.jwt.set(null);
  }
  entrar(email: string, senha: string) {
    return this.autenticar('/sessoes', { email: email.trim().toLowerCase(), senha });
  }
  registrar(nome: string, email: string, senha: string) {
    return this.autenticar('/usuarios', {
      nome: nome.trim(),
      email: email.trim().toLowerCase(),
      senha,
    });
  }
  private autenticar(rota: string, dados: object) {
    return this.http.post<{ token: string }>(environment.apiUrl + rota, dados).pipe(
      tap((resposta) => {
        if (typeof resposta.token !== 'string' || !resposta.token)
          throw new Error('Resposta inválida da sessão.');
        this.jwt.set(resposta.token);
      }),
      map(() => undefined),
    );
  }
}
