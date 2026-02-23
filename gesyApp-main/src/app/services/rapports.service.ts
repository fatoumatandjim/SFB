import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface RapportFinancier {
  stats: {
    chiffreAffaires: {
      total: number;
      evolution: string;
      periode: string;
    };
    depenses: {
      total: number;
      evolution: string;
      periode: string;
    };
    benefice: {
      total: number;
      evolution: string;
      periode: string;
    };
    marge: {
      pourcentage: number;
      evolution: string;
    };
  };
  donneesMensuelles: DonneeMensuelle[];
  categoriesDepenses: CategorieDepense[];
  fraisDouaniers?: {
    total: number;
    fraisDouane: number;
    fraisT1: number;
    evolution: string;
  };
  pertes?: {
    total: number;
    quantiteTotale: number;
    evolution: string;
  };
}

export interface DonneeMensuelle {
  mois: string;
  chiffreAffaires: number;
  depenses: number;
  benefice: number;
}

export interface CategorieDepense {
  nom: string;
  montant: number;
  pourcentage: number;
  couleur: string;
}

@Injectable({
  providedIn: 'root'
})
export class RapportsService {
  private apiUrl = `${environment.apiUrl}/rapports`;

  constructor(private http: HttpClient) {}

  getRapportFinancier(periode?: string, annee?: number, dateDebut?: string, dateFin?: string): Observable<RapportFinancier> {
    let params = new HttpParams();
    if (periode) params = params.set('periode', periode);
    if (annee) params = params.set('annee', annee.toString());
    if (dateDebut) params = params.set('dateDebut', dateDebut);
    if (dateFin) params = params.set('dateFin', dateFin);
    
    return this.http.get<RapportFinancier>(`${this.apiUrl}/financier`, { params });
  }
}

