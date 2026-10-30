import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessaoService } from './sessao.service';

export const sessaoGuard: CanActivateFn = (_route, state) =>
  inject(SessaoService).autenticado() ||
  inject(Router).createUrlTree(['/conta'], { queryParams: { retorno: state.url } });
