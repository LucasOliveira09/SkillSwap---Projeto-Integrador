import { Routes } from '@angular/router';
import { sessaoGuard } from '../conta/data-access/sessao.guard';

export const ANUNCIOS_ROUTES: Routes = [
  {
    path: 'novo',
    canActivate: [sessaoGuard],
    title: 'Novo anúncio | SkillSwap',
    loadComponent: () =>
      import('./pages/cadastro-anuncio/cadastro-anuncio.component').then(
        (m) => m.CadastroAnuncioComponent,
      ),
  },
  {
    path: 'minhas',
    title: 'Meus anúncios | SkillSwap',
    loadComponent: () =>
      import('./pages/minhas-anuncios/minhas-anuncios.component').then(
        (m) => m.MinhasAnunciosComponent,
      ),
  },
  {
    path: ':id/editar',
    title: 'Editar anúncio | SkillSwap',
    loadComponent: () =>
      import('./pages/cadastro-anuncio/cadastro-anuncio.component').then(
        (m) => m.CadastroAnuncioComponent,
      ),
  },
  {
    path: ':id',
    title: 'Detalhes do anúncio | SkillSwap',
    loadComponent: () =>
      import('./pages/detalhe-anuncio/detalhe-anuncio.component').then(
        (m) => m.DetalheAnuncioComponent,
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
