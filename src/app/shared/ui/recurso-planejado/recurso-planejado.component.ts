import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-recurso-planejado',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  templateUrl: './recurso-planejado.component.html',
  styleUrl: './recurso-planejado.component.scss',
})
export class RecursoPlanejadoComponent {
  protected readonly dados = inject<{ titulo: string; mensagem: string }>(MAT_DIALOG_DATA);
}
