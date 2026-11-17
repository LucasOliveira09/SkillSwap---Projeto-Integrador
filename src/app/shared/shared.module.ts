import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MaterialModule } from './material.module';
import { IconesModule } from './icones.module';
import { RecursoPlanejadoComponent } from './ui/recurso-planejado/recurso-planejado.component';
import { DenunciarComponent } from '../features/moderacao/ui/denunciar/denunciar.component';

/**
 * Recursos reutilizados pelos módulos de funcionalidade (carregados sob demanda):
 * CommonModule (pipes e diretivas), formulários, rotas, Angular Material,
 * ícones, diálogo informativo e o formulário de denúncia.
 */
@NgModule({
  declarations: [RecursoPlanejadoComponent, DenunciarComponent],
  imports: [CommonModule, FormsModule, RouterModule, MaterialModule, IconesModule],
  exports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MaterialModule,
    IconesModule,
    RecursoPlanejadoComponent,
    DenunciarComponent,
  ],
})
export class SharedModule {}
