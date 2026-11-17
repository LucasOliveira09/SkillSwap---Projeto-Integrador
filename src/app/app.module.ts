import { inject, NgModule, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { App } from './app';
import { appProviders } from './app.config';
import { AppRoutingModule } from './app-routing.module';
import { MatButtonModule } from '@angular/material/button';
import { MatSidenavModule } from '@angular/material/sidenav';
import { IconesModule } from './shared/icones.module';
import { SessaoService } from './features/conta/data-access/sessao.service';
import { IndicadorMensagensService } from './features/negociacao/data-access/indicador-mensagens.service';

/**
 * Módulo raiz. As áreas funcionais (anúncios, conta, perfil, negociação, moderação e painel)
 * são NgModules carregados sob demanda pelo AppRoutingModule.
 */
@NgModule({
  declarations: [App],
  // O layout principal usa apenas botões, menu lateral e ícones; o restante do Material
  // fica no SharedModule, importado pelos módulos carregados sob demanda.
  imports: [BrowserModule, AppRoutingModule, MatButtonModule, MatSidenavModule, IconesModule],
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    ...appProviders,
    // Restaura a sessão persistente antes da primeira navegação.
    provideAppInitializer(() => {
      inject(IndicadorMensagensService).iniciar();
      return inject(SessaoService).restaurar();
    }),
  ],
  bootstrap: [App],
})
export class AppModule {}
