import { Routes, UrlMatcher, UrlSegment } from '@angular/router';

/**
 * Monta um módulo na raiz quando a URL começa por um dos prefixos, sem consumir segmentos.
 * Assim módulos com mais de um endereço de topo (ex.: /mensagens e /acordos/:id)
 * continuam carregados sob demanda e definem as rotas completas em seu próprio routing.
 */
export function prefixos(...nomes: string[]): UrlMatcher {
  return (segmentos: UrlSegment[]) =>
    segmentos.length > 0 && nomes.includes(segmentos[0].path) ? { consumed: [] } : null;
}

/** Rotas raiz. Cada área funcional é um NgModule carregado sob demanda (lazy loading). */
export const routes: Routes = [
  {
    path: 'conta',
    loadChildren: () => import('./features/conta/conta.module').then((m) => m.ContaModule),
  },
  {
    path: 'painel',
    loadChildren: () => import('./features/painel/painel.module').then((m) => m.PainelModule),
  },
  {
    matcher: prefixos('meu-perfil', 'profissionais'),
    loadChildren: () => import('./features/perfil/perfil.module').then((m) => m.PerfilModule),
  },
  {
    matcher: prefixos('mensagens', 'acordos'),
    loadChildren: () =>
      import('./features/negociacao/negociacao.module').then((m) => m.NegociacaoModule),
  },
  {
    path: 'moderacao',
    loadChildren: () =>
      import('./features/moderacao/moderacao.module').then((m) => m.ModeracaoModule),
  },
  { path: '', pathMatch: 'full', redirectTo: 'anuncios' },
  {
    path: 'anuncios',
    loadChildren: () => import('./features/anuncios/anuncios.module').then((m) => m.AnunciosModule),
  },
  { path: '**', redirectTo: 'anuncios' },
];
