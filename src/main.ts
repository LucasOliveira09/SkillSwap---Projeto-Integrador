import { inject, provideAppInitializer } from '@angular/core';
import { SessaoService } from './app/features/conta/data-access/sessao.service';
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

bootstrapApplication(App, { ...appConfig, providers: [
  ...appConfig.providers,
  provideAppInitializer(() => inject(SessaoService).restaurar()),
] }).catch((err) => console.error(err));
