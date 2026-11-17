import {
  ApplicationConfig,
  EnvironmentProviders,
  LOCALE_ID,
  Provider,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { sessaoInterceptor } from './features/conta/data-access/sessao.interceptor';

import { routes } from './app.routes';

registerLocaleData(localePt);

/** Providers globais registrados pelo AppModule: erros globais, locale pt-BR e HttpClient com sessão. */
export const appProviders: (Provider | EnvironmentProviders)[] = [
  provideBrowserGlobalErrorListeners(),
  { provide: LOCALE_ID, useValue: 'pt-BR' },
  provideHttpClient(withInterceptors([sessaoInterceptor])),
];

/**
 * Mesmos providers com o roteador, para testes que montam páginas fora do AppModule.
 * A aplicação usa AppModule/AppRoutingModule (main.ts).
 */
export const appConfig: ApplicationConfig = {
  providers: [
    ...appProviders,
    provideRouter(routes, withInMemoryScrolling({ anchorScrolling: 'enabled' })),
  ],
};
