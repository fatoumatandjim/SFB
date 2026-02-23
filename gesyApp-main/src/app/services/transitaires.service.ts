import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Transitaire {
  id?: number;
  identifiant?: string;
  motDePasse?: string;
  defaultPass?: string;
  nom: string;
  email: string;
  telephone: string;
  statut?: 'ACTIF' | 'INACTIF' | 'SUSPENDU';
  nombreVoyages?: number;
  roleIds?: number[];
}

export interface TransitairePage {
  transitaires: Transitaire[];
  currentPage: number;
  totalPages: number;
  totalElements: number;
  size: number;
}

@Injectable({
  providedIn: 'root'
})
export class TransitairesService {
  private apiUrl = `${environment.apiUrl}/transitaires`;

  constructor(private http: HttpClient) {}

  getAllTransitaires(): Observable<Transitaire[]> {
    return this.http.get<Transitaire[]>(this.apiUrl);
  }

  getAllTransitairesPaginated(page: number = 0, size: number = 10): Observable<TransitairePage> {
    return this.http.get<TransitairePage>(`${this.apiUrl}/paginated?page=${page}&size=${size}`);
  }

  getTransitaireById(id: number): Observable<Transitaire> {
    return this.http.get<Transitaire>(`${this.apiUrl}/${id}`);
  }

  getTransitaireByIdentifiant(identifiant: string): Observable<Transitaire> {
    return this.http.get<Transitaire>(`${this.apiUrl}/identifiant/${identifiant}`);
  }

  createTransitaire(transitaire: Transitaire): Observable<Transitaire> {
    return this.http.post<Transitaire>(this.apiUrl, transitaire);
  }

  updateTransitaire(id: number, transitaire: Transitaire): Observable<Transitaire> {
    return this.http.put<Transitaire>(`${this.apiUrl}/${id}`, transitaire);
  }

  deleteTransitaire(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

