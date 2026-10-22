import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { SessaoService } from './sessao.service';

export const sessaoInterceptor: HttpInterceptorFn = (req, next) => {
  const sessao = inject(SessaoService);
  const interno = req.url.startsWith('/api/');
  const token = sessao.credencial();
  const requisicao =
    interno && token ? req.clone({ setHeaders: { Authorization: 'Bearer ' + token } }) : req;
  return next(requisicao).pipe(
    catchError((erro) => {
      if (interno && erro instanceof HttpErrorResponse && erro.status === 401) sessao.sair();
      return throwError(() => erro);
    }),
  );
};
