import { Component, Input, Output, EventEmitter, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';

export type AlertType = 'success' | 'error' | 'warning' | 'info';

export interface AlertConfig {
  title?: string;
  message: string;
  type?: AlertType;
  showCancelButton?: boolean;
  showConfirmButton?: boolean;
  confirmText?: string;
  cancelText?: string;
  onConfirm?: () => void;
  onCancel?: () => void;
}

@Component({
  selector: 'app-alert',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alert.component.html',
  styleUrls: ['./alert.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class AlertComponent {
  @Input() title: string = '';
  @Input() message: string = '';
  @Input() type: AlertType = 'info';
  @Input() showCancelButton: boolean = false;
  @Input() showConfirmButton: boolean = true;
  @Input() confirmText: string = 'Valider';
  @Input() cancelText: string = 'Annuler';
  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  close() {
    this.cancel.emit();
  }

  onConfirm() {
    this.confirm.emit();
  }

  onCancel() {
    this.cancel.emit();
  }

  getIcon(): string {
    switch (this.type) {
      case 'success':
        return '✓';
      case 'error':
        return '✕';
      case 'warning':
        return '⚠';
      case 'info':
        return 'ℹ';
      default:
        return 'ℹ';
    }
  }
}

