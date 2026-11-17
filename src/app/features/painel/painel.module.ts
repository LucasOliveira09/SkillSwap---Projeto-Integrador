import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { sessaoGuard } from '../conta/data-access/sessao.guard';
import { PainelComponent } from './pages/painel/painel.component';

const routes: Routes = [
  { path: '', canActivate: [sessaoGuard], title: 'Meu painel | SkillSwap', component: PainelComponent },
];

/** Dashboard operacional com KPIs da conta e, para ADMIN, da plataforma (carregado em /painel). */
@NgModule({
  declarations: [PainelComponent],
  imports: [SharedModule, RouterModule.forChild(routes)],
})
export class PainelModule {}
