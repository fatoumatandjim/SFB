import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Caisse {
  id?: number;
  nom: string;
  solde: number;
  statut: 'ACTIF' | 'FERME' | 'SUSPENDU';
  description?: string;
}

@Injectable({
  providedIn: 'root'
})
export class CaissesService {
  private apiUrl = `${environment.apiUrl}/caisses`;

  constructor(private http: HttpClient) {}

  getAllCaisses(): Observable<Caisse[]> {
    return this.http.get<Caisse[]>(this.apiUrl);
  }

  getCaisseById(id: number): Observable<Caisse> {
    return this.http.get<Caisse>(`${this.apiUrl}/${id}`);
  }

  getCaisseByNom(nom: string): Observable<Caisse> {
    return this.http.get<Caisse>(`${this.apiUrl}/nom/${nom}`);
  }

  createCaisse(caisse: Caisse): Observable<Caisse> {
    return this.http.post<Caisse>(this.apiUrl, caisse);
  }

  updateCaisse(id: number, caisse: Caisse): Observable<Caisse> {
    return this.http.put<Caisse>(`${this.apiUrl}/${id}`, caisse);
  }

  deleteCaisse(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

