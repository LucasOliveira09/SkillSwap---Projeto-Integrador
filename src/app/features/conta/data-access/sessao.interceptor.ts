import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, of, switchMap, throwError } from 'rxjs';
import { SessaoService } from './sessao.service';

export const sessaoInterceptor: HttpInterceptorFn = (req, next) => {
  const sessao = inject(SessaoService);
  const interno = req.url.startsWith('/api/');
  const token = sessao.credencial();
  const comToken = (jwt: string) => req.clone({ setHeaders: { Authorization: 'Bearer ' + jwt } });
  return next(interno && token ? comToken(token) : req).pipe(
    catchError(erro => {
      if (!interno || !token || sessao.saindo() || !(erro instanceof HttpErrorResponse) || erro.status !== 401)
        return throwError(() => erro);
      const atual = sessao.credencial();
      const renovar = atual && atual !== token ? of(undefined) : sessao.renovar();
      return renovar.pipe(switchMap(() => {
        const novo = sessao.credencial();
        return novo ? next(comToken(novo)) : throwError(() => erro);
      }));
    }),
  );
};
