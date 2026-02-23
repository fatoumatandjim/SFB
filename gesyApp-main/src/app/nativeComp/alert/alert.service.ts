import { Injectable, ComponentRef, ApplicationRef, Injector, createComponent, EnvironmentInjector } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { AlertComponent, AlertConfig } from './alert.component';

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  private currentAlert?: ComponentRef<AlertComponent>;
  private resultSubject = new Subject<boolean>();

  constructor(
    private appRef: ApplicationRef,
    private injector: Injector
  ) {}

  show(config: AlertConfig): Observable<boolean> {
    // Fermer l'alerte précédente si elle existe
    if (this.currentAlert) {
      this.close();
    }

    // Créer un nouveau Subject pour cette alerte
    const resultSubject = new Subject<boolean>();

    // Créer le composant
    const componentRef = createComponent(AlertComponent, {
      environmentInjector: this.injector.get(EnvironmentInjector)
    });

    this.currentAlert = componentRef;
    const alert = componentRef.instance;

    alert.title = config.title || '';
    alert.message = config.message;
    alert.type = config.type || 'info';
    alert.showCancelButton = config.showCancelButton ?? false;
    alert.showConfirmButton = config.showConfirmButton ?? true;
    alert.confirmText = config.confirmText || 'Valider';
    alert.cancelText = config.cancelText || 'Annuler';

    // Gérer les événements - s'abonner AVANT d'attacher au DOM
    const confirmSub = alert.confirm.subscribe(() => {
      if (config.onConfirm) {
        config.onConfirm();
      }
      resultSubject.next(true);
      resultSubject.complete();
      this.close();
      confirmSub.unsubscribe();
      cancelSub.unsubscribe();
    });

    const cancelSub = alert.cancel.subscribe(() => {
      if (config.onCancel) {
        config.onCancel();
      }
      resultSubject.next(false);
      resultSubject.complete();
      this.close();
      confirmSub.unsubscribe();
      cancelSub.unsubscribe();
    });

    // Attacher au DOM
    document.body.appendChild(componentRef.location.nativeElement);
    this.appRef.attachView(componentRef.hostView);

    // Déclencher la détection de changement
    componentRef.changeDetectorRef.detectChanges();

    return resultSubject.asObservable();
  }

  confirm(message: string, title?: string): Observable<boolean> {
    return this.show({
      message,
      title: title || 'Confirmation',
      type: 'info',
      showCancelButton: true,
      showConfirmButton: true
    });
  }

  success(message: string, title?: string): Observable<boolean> {
    return this.show({
      message,
      title: title || 'Succès',
      type: 'success',
      showCancelButton: false,
      showConfirmButton: true,
      confirmText: 'OK'
    });
  }

  error(message: string, title?: string): Observable<boolean> {
    return this.show({
      message,
      title: title || 'Erreur',
      type: 'error',
      showCancelButton: false,
      showConfirmButton: true,
      confirmText: 'OK'
    });
  }

  warning(message: string, title?: string): Observable<boolean> {
    return this.show({
      message,
      title: title || 'Attention',
      type: 'warning',
      showCancelButton: false,
      showConfirmButton: true,
      confirmText: 'OK'
    });
  }

  info(message: string, title?: string): Observable<boolean> {
    return this.show({
      message,
      title: title || 'Information',
      type: 'info',
      showCancelButton: false,
      showConfirmButton: true,
      confirmText: 'OK'
    });
  }

  close() {
    if (this.currentAlert) {
      this.appRef.detachView(this.currentAlert.hostView);
      if (this.currentAlert.location.nativeElement.parentElement) {
        this.currentAlert.location.nativeElement.parentElement.removeChild(this.currentAlert.location.nativeElement);
      }
      this.currentAlert.destroy();
      this.currentAlert = undefined;
    }
  }
}

