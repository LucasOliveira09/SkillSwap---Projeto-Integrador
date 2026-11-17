import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { sessaoGuard } from '../conta/data-access/sessao.guard';
import { AcordoDetalheComponent } from './pages/acordo-detalhe/acordo-detalhe.component';
import { CaixaMensagensComponent } from './pages/caixa-mensagens/caixa-mensagens.component';

/** Rotas completas: o módulo é montado na raiz por um matcher de prefixo (ver app.routes.ts). */
const routes: Routes = [
  { path: 'mensagens', canActivate: [sessaoGuard], title: 'Mensagens | SkillSwap', component: CaixaMensagensComponent },
  { path: 'acordos/:id', title: 'Conversa do acordo | SkillSwap', component: AcordoDetalheComponent },
];

/** Fluxo principal de negociação: conversa, proposta, acordo, conclusão e avaliação. */
@NgModule({
  declarations: [CaixaMensagensComponent, AcordoDetalheComponent],
  imports: [SharedModule, RouterModule.forChild(routes)],
})
export class NegociacaoModule {}
