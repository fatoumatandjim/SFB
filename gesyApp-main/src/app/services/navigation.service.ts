import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type MenuSection =
  | 'dashbord'
  | 'camion'
  | 'achats'
  | 'facturation'
  | 'paiement'
  | 'banque-caisse'
  | 'stock'
  | 'client-fournisseur'
  | 'suivi-transport'
  | 'transitaire'
  | 'axes'
  | 'depot'
  | 'recouvrement'
  | 'rapport'
  | 'cout'
  | 'analyse'
  | 'settings'
  | 'email'
  | 'depenses'
  | 'capital';

@Injectable({
  providedIn: 'root'
})
export class NavigationService {
  private currentSection = new BehaviorSubject<MenuSection>('dashbord');
  currentSection$ = this.currentSection.asObservable();

  /** Id facture à ouvrir dans Facturation (consommé une fois au chargement de la liste). */
  private pendingOpenFactureId: number | null = null;

  setCurrentSection(section: MenuSection) {
    this.currentSection.next(section);
  }

  getCurrentSection(): MenuSection {
    return this.currentSection.value;
  }

  /** Bascule vers Facturation et ouvre le détail de la facture après chargement. */
  openFacturationWithFacture(factureId: number): void {
    if (factureId == null || factureId <= 0) {
      return;
    }
    this.pendingOpenFactureId = factureId;
    this.setCurrentSection('facturation');
  }

  consumePendingOpenFactureId(): number | null {
    const id = this.pendingOpenFactureId;
    this.pendingOpenFactureId = null;
    return id;
  }
}
