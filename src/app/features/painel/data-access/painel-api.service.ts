import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { PainelPlataforma, PainelUsuario } from '../models/painel.model';

@Injectable({ providedIn: 'root' })
export class PainelApiService {
  private readonly http = inject(HttpClient);

  meuPainel() {
    return this.http.get<PainelUsuario>('/api/painel');
  }

  plataforma() {
    return this.http.get<PainelPlataforma>('/api/painel/plataforma');
  }
}
