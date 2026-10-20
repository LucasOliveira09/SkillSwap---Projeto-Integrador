import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'anuncios' },
  {
    path: 'anuncios',
    loadChildren: () =>
      import('./features/anuncios/anuncios.routes').then((m) => m.ANUNCIOS_ROUTES),
  },
  { path: '**', redirectTo: 'anuncios' },
];
