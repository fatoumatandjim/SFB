import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Depot {
  id?: number;
  nom: string;
  ville?: string;
  adresse: string;
  telephone?: string;
  responsable?: string;
  capacite: number;
  capaciteUtilisee?: number;
  statut?: 'ACTIF' | 'INACTIF' | 'EN_MAINTENANCE' | 'PLEIN';
  pays?: string;
}

@Injectable({
  providedIn: 'root'
})
export class DepotsService {
  private apiUrl = `${environment.apiUrl}/depots`;

  constructor(private http: HttpClient) {}

  getAllDepots(): Observable<Depot[]> {
    return this.http.get<Depot[]>(this.apiUrl);
  }

  getDepotById(id: number): Observable<Depot> {
    return this.http.get<Depot>(`${this.apiUrl}/${id}`);
  }

  createDepot(depot: Depot): Observable<Depot> {
    return this.http.post<Depot>(this.apiUrl, depot);
  }

  updateDepot(id: number, depot: Depot): Observable<Depot> {
    return this.http.put<Depot>(`${this.apiUrl}/${id}`, depot);
  }

  deleteDepot(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

