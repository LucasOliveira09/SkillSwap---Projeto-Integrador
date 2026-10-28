import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'conta',
    title: 'Minha conta | SkillSwap',
    loadComponent: () =>
      import('./features/conta/pages/conta.component').then((m) => m.ContaComponent),
  },
  {
    path: 'meu-perfil',
    title: 'Editar perfil profissional | SkillSwap',
    loadComponent: () =>
      import('./features/perfil/pages/meu-perfil/meu-perfil.component').then(
        (m) => m.MeuPerfilComponent,
      ),
  },
  {
    path: 'profissionais',
    title: 'Encontrar profissionais | SkillSwap',
    loadComponent: () =>
      import('./features/perfil/pages/busca-profissionais/busca-profissionais.component').then(
        (m) => m.BuscaProfissionaisComponent,
      ),
  },
  {
    path: 'mensagens',
    title: 'Mensagens | SkillSwap',
    loadComponent: () =>
      import('./features/negociacao/pages/caixa-mensagens/caixa-mensagens.component').then(
        (m) => m.CaixaMensagensComponent,
      ),
  },
  {
    path: 'acordos/:id',
    title: 'Conversa do acordo | SkillSwap',
    loadComponent: () =>
      import('./features/negociacao/pages/acordo-detalhe/acordo-detalhe.component').then(
        (m) => m.AcordoDetalheComponent,
      ),
  },
  {
    path: 'moderacao/denuncias',
    title: 'Moderação de conteúdo | SkillSwap',
    loadComponent: () =>
      import('./features/moderacao/pages/denuncias/denuncias.component').then((m) => m.DenunciasComponent),
  },
  {
    path: 'profissionais/:id',
    title: 'Perfil profissional | SkillSwap',
    loadComponent: () =>
      import('./features/perfil/pages/perfil-publico/perfil-publico.component').then(
        (m) => m.PerfilPublicoComponent,
      ),
  },
  { path: '', pathMatch: 'full', redirectTo: 'anuncios' },
  {
    path: 'anuncios',
    loadChildren: () =>
      import('./features/anuncios/anuncios.routes').then((m) => m.ANUNCIOS_ROUTES),
  },
  { path: '**', redirectTo: 'anuncios' },
];
