import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface DashboardStats {
  camionsActifs: {
    value: number;
    change: string;
    enRoute: number;
    disponibles: number;
  };
  chiffreAffaires: {
    value: string;
    currency: string;
    change: string;
    period: string;
    increase: string;
  };
  facturesAttente: {
    value: number;
    badge: number;
    montant: string;
    enRetard: number;
  };
  unitesStock: {
    value: string;
    stockRestant: string;
    alert: boolean;
    niveauCritique: number;
    depots: number;
    stocksParProduit: Array<{
      produitId: number;
      produitNom: string;
      typeProduit: string;
      quantiteTotale: string;
      quantiteTotaleValue: number;
      alert: boolean;
      nombreDepots: number;
    }>;
  };
  finances: {
    soldeBanque: {
      value: string;
      currency: string;
      comptes: number;
      change: string;
    };
    soldeCaisse: {
      value: string;
      currency: string;
      date: string;
      entrees: string;
    };
    creancesClients: {
      value: string;
      currency: string;
      clients: number;
      retard: string;
    };
  };
  voyagesStats: {
    totalVoyagesEnCours: number;
    voyagesArrives: number;
    voyagesALaDouane: number;
    voyagesLivre: number;
    voyagesRecents: Array<{
      id: number;
      numeroVoyage: string;
      camionImmatriculation: string;
      clientNom: string;
      destination: string;
      statut: string;
      dateDepart: string;
    }>;
  };
  douaneStats?: {
    nombreCamionsDeclares: number;
    nombreCamionsNonDeclares: number;
    montantFraisDouane: string;
    montantT1: string;
    montantFraisPayes: string;
    currency: string;
  };
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private apiUrl = `${environment.apiUrl}/dashboard`;

  constructor(private http: HttpClient) {}

  getDashboardStats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/stats`);
  }
}

