import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Utilisateur } from '../../services/utilisateurs.service';
import { FINANCE_SELECT_COPY } from '../../shared/finance/responsables';

/**
 * Liste multi-sélection des comptables éligibles comme responsables banque/caisse.
 */
@Component({
  selector: 'app-responsables-comptables-multiselect',
  standalone: true,
  imports: [CommonModule, FormsModule],
  styleUrls: ['./responsables-comptables-multiselect.component.scss'],
  template: `
    <div class="rcm-root">
      <label class="rcm-label" [attr.for]="inputId">{{ label }}</label>
      @if (comptables.length === 0) {
        <p class="rcm-empty">{{ selectCopy.comptablesMultiselectEmpty }}</p>
      }
      <select
        [id]="inputId"
        [name]="controlName"
        multiple
        class="rcm-select"
        [attr.size]="size"
        [disabled]="comptables.length === 0"
        [ngModel]="selectedIds"
        (ngModelChange)="selectedIdsChange.emit($event)"
      >
        @for (u of comptables; track u.id) {
          <option [ngValue]="u.id">
            {{ u.nom }} @if (u.identifiant) { ({{ u.identifiant }}) }
          </option>
        }
      </select>
      @if (hint) {
        <p class="rcm-hint">{{ hint }}</p>
      }
    </div>
  `
})
export class ResponsablesComptablesMultiselectComponent {
  readonly selectCopy = FINANCE_SELECT_COPY;

  @Input() label = '';
  @Input() hint = '';
  @Input() inputId = 'responsables-ms';
  @Input() controlName = 'responsableIds';
  /** Nombre de lignes visibles ; la liste est limitée en hauteur et défilable (voir SCSS). */
  @Input() size = 3;
  @Input() comptables: Utilisateur[] = [];
  @Input() selectedIds: number[] = [];
  @Output() readonly selectedIdsChange = new EventEmitter<number[]>();
}
