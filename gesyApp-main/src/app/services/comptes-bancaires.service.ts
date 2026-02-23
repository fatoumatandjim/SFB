import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface CompteBancaire {
  id?: number;
  numero: string;
  type: 'BANQUE' | 'CAISSE' | 'MOBILE_MONEY';
  solde: number;
  banque: string;
  numeroCompteBancaire?: string;
  statut: 'ACTIF' | 'FERME' | 'SUSPENDU';
  description?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ComptesBancairesService {
  private apiUrl = `${environment.apiUrl}/comptes-bancaires`;

  constructor(private http: HttpClient) {}

  getAllComptes(): Observable<CompteBancaire[]> {
    return this.http.get<CompteBancaire[]>(this.apiUrl);
  }

  getCompteById(id: number): Observable<CompteBancaire> {
    return this.http.get<CompteBancaire>(`${this.apiUrl}/${id}`);
  }

  getCompteByNumero(numero: string): Observable<CompteBancaire> {
    return this.http.get<CompteBancaire>(`${this.apiUrl}/numero/${numero}`);
  }

  createCompte(compte: CompteBancaire): Observable<CompteBancaire> {
    return this.http.post<CompteBancaire>(this.apiUrl, compte);
  }

  updateCompte(id: number, compte: CompteBancaire): Observable<CompteBancaire> {
    return this.http.put<CompteBancaire>(`${this.apiUrl}/${id}`, compte);
  }

  deleteCompte(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getStats(): Observable<BanqueCaisseStats> {
    return this.http.get<BanqueCaisseStats>(`${this.apiUrl}/stats`);
  }
}

export interface BanqueCaisseStats {
  soldeTotal: {
    montant: number;
    evolution: string;
    periode: string;
  };
  soldeCaisse: {
    montant: number;
    entrees: number;
    sorties: number;
    date: string;
  };
  comptesBancaires: {
    total: number;
    actifs: number;
  };
  totalEntrees: {
    montant: number;
  };
  totalSorties: {
    montant: number;
  };
}

