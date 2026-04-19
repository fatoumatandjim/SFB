import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FINANCE_RESPONSABLES_COPY } from './responsables';

/**
 * Bloc « Responsables » + bouton édition (admin) pour cartes banque/caisse.
 */
@Component({
  selector: 'app-finance-responsables-footer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="finance-resp-footer">
      <div class="finance-resp-footer__text">
        <span class="finance-resp-footer__label">{{ copy.sectionLabel }}</span>
        <span class="finance-resp-footer__value">{{ displayLine }}</span>
      </div>
      @if (showEdit) {
        <button
          type="button"
          class="finance-resp-footer__edit"
          (click)="editRequested.emit()"
          [title]="copy.editTitle"
          [attr.aria-label]="copy.editTitle"
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="M12 20h9" />
            <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
          </svg>
        </button>
      }
    </div>
  `,
  styleUrls: ['./finance-responsables-footer.component.scss']
})
export class FinanceResponsablesFooterComponent {
  readonly copy = FINANCE_RESPONSABLES_COPY;

  @Input({ required: true }) displayLine!: string;
  @Input() showEdit = false;
  @Output() readonly editRequested = new EventEmitter<void>();
}
