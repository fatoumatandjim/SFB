import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Fournisseur {
  id?: number;
  nom: string;
  email: string;
  telephone: string;
  adresse: string;
  codeFournisseur?: string;
  ville?: string;
  pays?: string;
  contactPersonne?: string;
  typeFournisseur?: 'ACHAT' | 'TRANSPORT';
}

@Injectable({
  providedIn: 'root'
})
export class FournisseursService {
  private apiUrl = `${environment.apiUrl}/fournisseurs`;

  constructor(private http: HttpClient) {}

  getAllFournisseurs(): Observable<Fournisseur[]> {
    return this.http.get<Fournisseur[]>(this.apiUrl);
  }

  getFournisseursByType(type: 'ACHAT' | 'TRANSPORT'): Observable<Fournisseur[]> {
    return this.http.get<Fournisseur[]>(`${this.apiUrl}/type/${type}`);
  }

  getFournisseurById(id: number): Observable<Fournisseur> {
    return this.http.get<Fournisseur>(`${this.apiUrl}/${id}`);
  }

  createFournisseur(fournisseur: Fournisseur): Observable<Fournisseur> {
    return this.http.post<Fournisseur>(this.apiUrl, fournisseur);
  }

  updateFournisseur(id: number, fournisseur: Fournisseur): Observable<Fournisseur> {
    return this.http.put<Fournisseur>(`${this.apiUrl}/${id}`, fournisseur);
  }

  deleteFournisseur(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

