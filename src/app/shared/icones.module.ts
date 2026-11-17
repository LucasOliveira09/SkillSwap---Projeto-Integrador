import { NgModule } from '@angular/core';
import { IconeComponent } from './ui/icone/icone.component';

/** Ícones SVG locais, usados pelo layout principal e por todas as áreas. */
@NgModule({
  declarations: [IconeComponent],
  exports: [IconeComponent],
})
export class IconesModule {}
