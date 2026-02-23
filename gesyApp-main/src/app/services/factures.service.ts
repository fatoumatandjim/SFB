import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface LigneFacture {
  id?: number;
  produitId: number | undefined;
  quantite: number;
  prixUnitaire: number;
  total: number;
  produitNom?: string;
  typeProduit?: string;
}

export interface Facture {
  id?: number;
  numero?: string;
  date: string;
  montant?: number;
  montantHT?: number;
  montantTTC?: number;
  tauxTVA?: number;
  clientId: number;
  clientNom?: string;
  clientEmail?: string;
  statut: 'BROUILLON' | 'EMISE' | 'PAYEE' | 'PARTIELLEMENT_PAYEE' | 'ANNULEE' | 'EN_RETARD';
  dateEcheance: string;
  montantPaye?: number;
  description?: string;
  notes?: string;
  lignes?: LigneFacture[];
}

export interface FactureStats {
  facturesEmises: {
    total: number;
    montant: number;
    periode: string;
    evolution: string;
  };
  facturesPayees: {
    total: number;
    montant: number;
    pourcentage: string;
  };
  facturesImpayees: {
    total: number;
    montant: number;
    enRetard: number;
    urgent: boolean;
  };
}

export interface Creance {
  id: number;
  facture: string;
  clientId: number;
  clientNom: string;
  clientEmail: string;
  clientTelephone?: string;
  montant: number;
  montantPaye: number;
  resteAPayer: number;
  dateEmission: string;
  dateEcheance: string;
  joursRetard: number;
  statut: 'en-retard' | 'recouvre' | 'en-cours' | 'impaye';
  priorite: 'haute' | 'moyenne' | 'basse';
  relances: number;
  dernierContact: string;
}

export interface RecouvrementStats {
  totalCreances: {
    montant: number;
    nombre: number;
  };
  enRetard: {
    montant: number;
    nombre: number;
    joursMoyen: number;
  };
  recouvre: {
    montant: number;
    nombre: number;
    pourcentage: string;
  };
  impaye: {
    montant: number;
    nombre: number;
  };
}

export interface FacturePage {
  factures: Facture[];
  currentPage: number;
  totalPages: number;
  totalElements: number;
  size: number;
}

@Injectable({
  providedIn: 'root'
})
export class FacturesService {
  private apiUrl = `${environment.apiUrl}/factures`;

  constructor(private http: HttpClient) {}

  getAllFactures(): Observable<Facture[]> {
    return this.http.get<Facture[]>(this.apiUrl);
  }

  getAllFacturesPaginated(page: number = 0, size: number = 10): Observable<FacturePage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<FacturePage>(`${this.apiUrl}/paginated`, { params });
  }

  getFactureById(id: number): Observable<Facture> {
    return this.http.get<Facture>(`${this.apiUrl}/${id}`);
  }

  getFactureByNumero(numero: string): Observable<Facture> {
    return this.http.get<Facture>(`${this.apiUrl}/numero/${numero}`);
  }

  getFacturesByClientId(clientId: number): Observable<Facture[]> {
    return this.http.get<Facture[]>(`${this.apiUrl}/client/${clientId}`);
  }

  createFacture(facture: Facture): Observable<Facture> {
    return this.http.post<Facture>(this.apiUrl, facture);
  }

  updateFacture(id: number, facture: Facture): Observable<Facture> {
    return this.http.put<Facture>(`${this.apiUrl}/${id}`, facture);
  }

  updateStatut(id: number, statut: string): Observable<Facture> {
    return this.http.put<Facture>(`${this.apiUrl}/${id}/statut?statut=${statut}`, {});
  }

  deleteFacture(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getStats(): Observable<FactureStats> {
    return this.http.get<FactureStats>(`${this.apiUrl}/stats`);
  }

  getUnpaidFactures(): Observable<Creance[]> {
    return this.http.get<Creance[]>(`${this.apiUrl}/recouvrement/creances`);
  }

  getRecouvrementStats(): Observable<RecouvrementStats> {
    return this.http.get<RecouvrementStats>(`${this.apiUrl}/recouvrement/stats`);
  }

  exportFacturesPdf(clientId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/client/${clientId}/export-pdf`, {
      responseType: 'blob'
    });
  }
}

