import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';

export interface Pays {
  id: number;
  nom: string;
  fraisParLitre: number;
  fraisParLitreGasoil: number;
  fraisT1: number;
}

export interface HistoriquePays {
  id: number;
  paysId: number;
  paysNom: string;
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
export class PaysService {
  private apiUrl = `${environment.apiUrl}/pays`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Pays[]> {
    return this.http.get<Pays[]>(this.apiUrl);
  }

  getById(id: number): Observable<Pays> {
    return this.http.get<Pays>(`${this.apiUrl}/${id}`);
  }

  create(pays: Partial<Pays>): Observable<Pays> {
    return this.http.post<Pays>(this.apiUrl, pays);
  }

  update(id: number, pays: Partial<Pays>): Observable<Pays> {
    return this.http.put<Pays>(`${this.apiUrl}/${id}`, pays);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getHistorique(paysId: number): Observable<HistoriquePays[]> {
    return this.http.get<HistoriquePays[]>(`${this.apiUrl}/${paysId}/historique`);
  }
}
