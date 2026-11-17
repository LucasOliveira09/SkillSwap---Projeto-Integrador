import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { AnunciosRoutingModule } from './anuncios-routing.module';
import { CadastroAnuncioComponent } from './pages/cadastro-anuncio/cadastro-anuncio.component';
import { CatalogoComponent } from './pages/catalogo/catalogo.component';
import { DetalheAnuncioComponent } from './pages/detalhe-anuncio/detalhe-anuncio.component';
import { MinhasAnunciosComponent } from './pages/minhas-anuncios/minhas-anuncios.component';
import { AnuncioCardComponent } from './ui/anuncio-card/anuncio-card.component';
import { BannerDestaquesComponent } from './ui/banner-destaques/banner-destaques.component';
import { ColecaoVisualComponent } from './ui/colecao-visual/colecao-visual.component';

/** Catálogo, detalhe, cadastro/edição e gestão de anúncios (carregado sob demanda em /anuncios). */
@NgModule({
  declarations: [
    CatalogoComponent,
    CadastroAnuncioComponent,
    DetalheAnuncioComponent,
    MinhasAnunciosComponent,
    AnuncioCardComponent,
    BannerDestaquesComponent,
    ColecaoVisualComponent,
  ],
  imports: [SharedModule, AnunciosRoutingModule],
})
export class AnunciosModule {}
