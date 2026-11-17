import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { sessaoGuard } from '../conta/data-access/sessao.guard';
import { CadastroAnuncioComponent } from './pages/cadastro-anuncio/cadastro-anuncio.component';
import { CatalogoComponent } from './pages/catalogo/catalogo.component';
import { DetalheAnuncioComponent } from './pages/detalhe-anuncio/detalhe-anuncio.component';
import { MinhasAnunciosComponent } from './pages/minhas-anuncios/minhas-anuncios.component';

/** Rotas filhas de /anuncios. */
const routes: Routes = [
  { path: 'novo', canActivate: [sessaoGuard], title: 'Novo anúncio | SkillSwap', component: CadastroAnuncioComponent },
  { path: 'minhas', title: 'Meus anúncios | SkillSwap', component: MinhasAnunciosComponent },
  { path: ':id/editar', title: 'Editar anúncio | SkillSwap', component: CadastroAnuncioComponent },
  { path: ':id', title: 'Detalhes do anúncio | SkillSwap', component: DetalheAnuncioComponent },
  { path: '', pathMatch: 'full', title: 'Serviços e aulas | SkillSwap', component: CatalogoComponent },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AnunciosRoutingModule {}
