import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Paiement {
  id: number;
  montant: number;
  date: string;
  factureId?: number;
  methode: string;
  statut: 'EN_ATTENTE' | 'VALIDE' | 'REJETE' | 'ANNULE';
  numeroCheque?: string;
  numeroCompte?: string;
  reference?: string;
  notes?: string;
  transactions?: any[];
  compteId?: number;
  caisseId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class PaiementService {
  private apiUrl = `${environment.apiUrl}/paiements`;

  constructor(private http: HttpClient) {}

  getAllPaiements(): Observable<Paiement[]> {
    return this.http.get<Paiement[]>(`${this.apiUrl}`);
  }

  getPaiementById(id: number): Observable<Paiement> {
    return this.http.get<Paiement>(`${this.apiUrl}/${id}`);
  }

  getPaiementsByStatut(statut: string): Observable<Paiement[]> {
    return this.http.get<Paiement[]>(`${this.apiUrl}/statut/${statut}`);
  }

  validerPaiement(id: number, compteId?: number, caisseId?: number): Observable<Paiement> {
    let url = `${this.apiUrl}/${id}/valider`;
    const params: string[] = [];
    if (compteId) {
      params.push(`compteId=${compteId}`);
    }
    if (caisseId) {
      params.push(`caisseId=${caisseId}`);
    }
    if (params.length > 0) {
      url += '?' + params.join('&');
    }
    return this.http.put<Paiement>(url, {});
  }
}

