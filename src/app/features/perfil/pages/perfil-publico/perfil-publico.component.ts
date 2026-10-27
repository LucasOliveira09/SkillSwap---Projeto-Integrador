import { Component, computed, inject } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, of, startWith, switchMap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { AnunciosApiService } from '../../../anuncios/data-access/anuncios-api.service';
import { AnuncioApi } from '../../../anuncios/models/anuncio-api.model';
import { IconeComponent } from '../../../../shared/ui/icone/icone.component';
import { PerfilApiService } from '../../data-access/perfil-api.service';
import { PerfilProfissional } from '../../models/perfil.model';

interface EstadoPerfil {
  carregando: boolean;
  erro: boolean;
  perfil: PerfilProfissional | null;
  ofertas: readonly AnuncioApi[];
}

const estadoInicial: EstadoPerfil = {
  carregando: true,
  erro: false,
  perfil: null,
  ofertas: [],
};

@Component({
  selector: 'app-perfil-publico',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, DatePipe, DecimalPipe, MatButtonModule, IconeComponent],
  templateUrl: './perfil-publico.component.html',
  styleUrl: './perfil-publico.component.scss',
})
export class PerfilPublicoComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly perfilApi = inject(PerfilApiService);
  private readonly anunciosApi = inject(AnunciosApiService);

  protected readonly estado = toSignal(
    this.route.paramMap.pipe(
      switchMap((params) => {
        const id = params.get('id');
        if (!id || !/^\d+$/.test(id)) {
          return of({ carregando: false, erro: true, perfil: null, ofertas: [] });
        }
        const usuarioId = Number(id);
        if (!Number.isSafeInteger(usuarioId) || usuarioId <= 0) {
          return of({ carregando: false, erro: true, perfil: null, ofertas: [] });
        }
        return this.perfilApi.publico(usuarioId).pipe(
          switchMap((perfil) =>
            this.anunciosApi.listarDoUsuario(perfil.id).pipe(
              map((ofertas) => ({
                carregando: false,
                erro: false,
                perfil,
                ofertas: ofertas.itens,
              })),
            ),
          ),
          catchError(() => of({ carregando: false, erro: true, perfil: null, ofertas: [] })),
          startWith(estadoInicial),
        );
      }),
    ),
    { initialValue: estadoInicial },
  );

  protected readonly perfil = computed(() => this.estado().perfil);
  protected readonly ofertas = computed(() => this.estado().ofertas);
}
