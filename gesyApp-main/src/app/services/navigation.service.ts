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

  setCurrentSection(section: MenuSection) {
    this.currentSection.next(section);
  }

  getCurrentSection(): MenuSection {
    return this.currentSection.value;
  }
}
