import { Component, Input, OnInit, OnDestroy, ViewEncapsulation, ComponentRef } from '@angular/core';
import { CommonModule } from '@angular/common';

export type ToastPosition = 'top-left' | 'top-center' | 'top-right' | 'bottom-left' | 'bottom-center' | 'bottom-right';
export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastConfig {
  message: string;
  type?: ToastType;
  duration?: number; // en millisecondes
  position?: ToastPosition;
}

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ToastComponent implements OnInit, OnDestroy {
  @Input() message: string = '';
  @Input() type: ToastType = 'info';
  @Input() duration: number = 3000;
  @Input() position: ToastPosition = 'top-right';
  @Input() componentRef?: ComponentRef<ToastComponent>;
  @Input() toastService?: any;

  private timeoutId?: number;

  ngOnInit() {
    if (this.duration > 0) {
      this.timeoutId = window.setTimeout(() => {
        this.close();
      }, this.duration);
    }
  }

  ngOnDestroy() {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }
  }

  close() {
    if (this.componentRef && this.toastService) {
      this.toastService.removeToast(this.componentRef);
    }
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

