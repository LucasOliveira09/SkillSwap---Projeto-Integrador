import { Routes } from '@angular/router';

export const ANUNCIOS_ROUTES: Routes = [
  {
    path: 'novo',
    title: 'Novo anúncio | SkillSwap',
    loadComponent: () =>
      import('./pages/cadastro-anuncio/cadastro-anuncio.component').then(
        (m) => m.CadastroAnuncioComponent,
      ),
  },
  {
    path: '',
    pathMatch: 'full',
    title: 'Serviços e aulas | SkillSwap',
    loadComponent: () =>
      import('./pages/catalogo/catalogo.component').then((m) => m.CatalogoComponent),
  },
];
