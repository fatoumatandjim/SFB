import { Injectable, ComponentRef, ApplicationRef, Injector, createComponent, EnvironmentInjector, ChangeDetectorRef } from '@angular/core';
import { ToastComponent, ToastConfig, ToastPosition } from './toast.component';

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private toasts: ComponentRef<ToastComponent>[] = [];

  constructor(
    private appRef: ApplicationRef,
    private injector: Injector
  ) {}

  show(config: ToastConfig | string): void {
    const toastConfig: ToastConfig = typeof config === 'string'
      ? { message: config }
      : config;

    // Créer le composant
    const componentRef = createComponent(ToastComponent, {
      environmentInjector: this.injector.get(EnvironmentInjector)
    });

    const toast = componentRef.instance;
    toast.message = toastConfig.message || '';
    toast.type = toastConfig.type || 'info';
    toast.duration = toastConfig.duration ?? 3000;
    toast.position = toastConfig.position || 'top-right';
    toast.componentRef = componentRef;
    toast.toastService = this;

    // Attacher au DOM
    document.body.appendChild(componentRef.location.nativeElement);
    this.appRef.attachView(componentRef.hostView);

    // Déclencher la détection de changement
    componentRef.changeDetectorRef.detectChanges();

    this.toasts.push(componentRef);

    // Auto-remove après la durée
    if (toast.duration > 0) {
      setTimeout(() => {
        this.removeToast(componentRef);
      }, toast.duration + 300); // +300ms pour l'animation
    }
  }

  success(message: string, duration?: number, position?: ToastPosition): void {
    this.show({ message, type: 'success', duration, position });
  }

  error(message: string, duration?: number, position?: ToastPosition): void {
    this.show({ message, type: 'error', duration, position });
  }

  warning(message: string, duration?: number, position?: ToastPosition): void {
    this.show({ message, type: 'warning', duration, position });
  }

  info(message: string, duration?: number, position?: ToastPosition): void {
    this.show({ message, type: 'info', duration, position });
  }

  removeToast(componentRef: ComponentRef<ToastComponent>): void {
    const index = this.toasts.indexOf(componentRef);
    if (index > -1) {
      this.toasts.splice(index, 1);
      const element = componentRef.location.nativeElement;
      if (element && element.parentElement) {
        // Ajouter une classe pour l'animation de fermeture
        element.classList.add('toast-fade-out');
        setTimeout(() => {
          if (element && element.parentElement) {
            element.parentElement.removeChild(element);
          }
          this.appRef.detachView(componentRef.hostView);
          componentRef.destroy();
        }, 300);
      } else {
        this.appRef.detachView(componentRef.hostView);
        componentRef.destroy();
      }
    }
  }
}

