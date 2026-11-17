import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { BuscaProfissionaisComponent } from './pages/busca-profissionais/busca-profissionais.component';
import { MeuPerfilComponent } from './pages/meu-perfil/meu-perfil.component';
import { PerfilPublicoComponent } from './pages/perfil-publico/perfil-publico.component';
import { HistoricoProfissionalEditorComponent } from './ui/historico-profissional-editor/historico-profissional-editor.component';
import { PublicacaoInteracoesComponent } from './ui/publicacao-interacoes/publicacao-interacoes.component';

/** Rotas completas: o módulo é montado na raiz por um matcher de prefixo (ver app.routes.ts). */
const routes: Routes = [
  { path: 'meu-perfil', title: 'Editar perfil profissional | SkillSwap', component: MeuPerfilComponent },
  { path: 'profissionais', title: 'Encontrar profissionais | SkillSwap', component: BuscaProfissionaisComponent },
  { path: 'profissionais/:id', title: 'Perfil profissional | SkillSwap', component: PerfilPublicoComponent },
];

/** Perfil profissional editável, perfil público, portfólio e busca de profissionais. */
@NgModule({
  declarations: [
    BuscaProfissionaisComponent,
    MeuPerfilComponent,
    PerfilPublicoComponent,
    HistoricoProfissionalEditorComponent,
    PublicacaoInteracoesComponent,
  ],
  imports: [SharedModule, RouterModule.forChild(routes)],
})
export class PerfilModule {}
