import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA } from '@angular/material/dialog';

@Component({
  standalone: false,
  selector: 'app-recurso-planejado',
  templateUrl: './recurso-planejado.component.html',
  styleUrl: './recurso-planejado.component.scss',
})
export class RecursoPlanejadoComponent {
  protected readonly dados = inject<{ titulo: string; mensagem: string }>(MAT_DIALOG_DATA);
}
