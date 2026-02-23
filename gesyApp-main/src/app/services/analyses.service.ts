import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Analyse {
  kpis: {
    croissance: {
      valeur: number;
      evolution: string;
    };
    efficacite: {
      valeur: number;
      evolution: string;
    };
    satisfaction: {
      valeur: number;
      evolution: string;
    };
    rentabilite: {
      valeur: number;
      evolution: string;
    };
  };
  donneesHebdomadaires: DonneeHebdomadaire[];
  tendances: Tendance[];
  performances: Performance[];
}

export interface DonneeHebdomadaire {
  semaine: string;
  ventes: number;
  clients: number;
  camions: number;
}

export interface Tendance {
  categorie: string;
  evolution: number;
  tendance: 'hausse' | 'baisse' | 'stable';
  couleur: string;
}

export interface Performance {
  indicateur: string;
  valeur: number;
  cible: number;
  pourcentage: number;
  couleur: string;
}

@Injectable({
  providedIn: 'root'
})
export class AnalysesService {
  private apiUrl = `${environment.apiUrl}/analyses`;

  constructor(private http: HttpClient) {}

  getAnalyse(periode?: string, annee?: number, dateDebut?: string, dateFin?: string): Observable<Analyse> {
    let params = new HttpParams();
    if (periode) params = params.set('periode', periode);
    if (annee) params = params.set('annee', annee.toString());
    if (dateDebut) params = params.set('dateDebut', dateDebut);
    if (dateFin) params = params.set('dateFin', dateFin);
    
    return this.http.get<Analyse>(this.apiUrl, { params });
  }
}

