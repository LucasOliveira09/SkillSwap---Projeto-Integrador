import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { DenunciasComponent } from './pages/denuncias/denuncias.component';

const routes: Routes = [
  { path: 'denuncias', title: 'Moderação de conteúdo | SkillSwap', component: DenunciasComponent },
];

/** Fila de moderação (perfil ADMIN no backend), carregada em /moderacao. */
@NgModule({
  declarations: [DenunciasComponent],
  imports: [SharedModule, RouterModule.forChild(routes)],
})
export class ModeracaoModule {}
