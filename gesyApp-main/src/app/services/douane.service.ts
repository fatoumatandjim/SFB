import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Douane {
  id?: number;
  fraisParLitre: number;
  fraisParLitreGasoil: number;
  fraisT1: number;
}

export interface HistoriqueDouane {
  id?: number;
  ancienFraisParLitre: number;
  nouveauFraisParLitre: number;
  ancienFraisParLitreGasoil: number;
  nouveauFraisParLitreGasoil: number;
  ancienFraisT1: number;
  nouveauFraisT1: number;
  dateModification: string;
  modifiePar: string;
  commentaire: string;
}

@Injectable({
  providedIn: 'root'
})
export class DouaneService {
  private apiUrl = `${environment.apiUrl}/douane`;

  constructor(private http: HttpClient) {}

  getDouane(): Observable<Douane> {
    return this.http.get<Douane>(this.apiUrl);
  }

  updateDouane(douane: Douane): Observable<Douane> {
    return this.http.put<Douane>(this.apiUrl, douane);
  }

  getHistorique(): Observable<HistoriqueDouane[]> {
    return this.http.get<HistoriqueDouane[]>(`${this.apiUrl}/historique`);
  }
}

