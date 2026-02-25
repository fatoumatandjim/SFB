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

export interface FraisDouaneAxe {
  id?: number;
  axeId: number;
  axeNom?: string;
  fraisParLitre: number;
  fraisParLitreGasoil: number;
  fraisT1: number;
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

  getFraisDouaneAxeAll(): Observable<FraisDouaneAxe[]> {
    return this.http.get<FraisDouaneAxe[]>(`${this.apiUrl}/frais-axe`);
  }

  getFraisDouaneAxeByAxeId(axeId: number): Observable<FraisDouaneAxe> {
    return this.http.get<FraisDouaneAxe>(`${this.apiUrl}/frais-axe/axe/${axeId}`);
  }

  createFraisDouaneAxe(frais: FraisDouaneAxe): Observable<FraisDouaneAxe> {
    return this.http.post<FraisDouaneAxe>(`${this.apiUrl}/frais-axe`, frais);
  }

  updateFraisDouaneAxe(id: number, frais: Partial<FraisDouaneAxe>): Observable<FraisDouaneAxe> {
    return this.http.put<FraisDouaneAxe>(`${this.apiUrl}/frais-axe/${id}`, frais);
  }

  deleteFraisDouaneAxe(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/frais-axe/${id}`);
  }

  /** Crée un nouvel axe et ses frais de douane en une seule transaction. */
  createFraisDouaneAxeWithNewAxe(data: {
    nomAxe: string;
    fraisParLitre: number;
    fraisParLitreGasoil: number;
    fraisT1: number;
  }): Observable<FraisDouaneAxe> {
    return this.http.post<FraisDouaneAxe>(`${this.apiUrl}/frais-axe/avec-nouvel-axe`, data);
  }
}

