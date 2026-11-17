import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { ContaComponent } from './pages/conta.component';

const routes: Routes = [{ path: '', title: 'Minha conta | SkillSwap', component: ContaComponent }];

/** Entrar, criar conta, notificações e sair (carregado em /conta). */
@NgModule({
  declarations: [ContaComponent],
  imports: [SharedModule, RouterModule.forChild(routes)],
})
export class ContaModule {}
