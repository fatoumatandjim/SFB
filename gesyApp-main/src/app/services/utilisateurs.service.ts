import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Role {
  id: number;
  nom: string;
  description: string;
  statut: 'ACTIF' | 'INACTIF';
}

export interface Utilisateur {
  id?: number;
  identifiant?: string;
  nom: string;
  email: string;
  telephone: string;
  roles?: Role[]; // Rôles complets depuis le DTO
  rolesIds?: number[];
  actif?: boolean; // Peut être null depuis le DTO
  statut?: 'ACTIF' | 'INACTIF' | 'SUSPENDU';
  motDePasse?: string;
  defaultPass?: string;
  depotId?: number;
}

@Injectable({
  providedIn: 'root'
})
export class UtilisateursService {
  private apiUrl = `${environment.apiUrl}/utilisateurs`;

  constructor(private http: HttpClient) {}

  getAllUtilisateurs(): Observable<Utilisateur[]> {
    return this.http.get<Utilisateur[]>(this.apiUrl);
  }

  /** Utilisateurs pouvant être responsables logistiques (voyages, camions, etc.) */
  getLogisticiensEtResponsables(): Observable<Utilisateur[]> {
    return this.http.get<Utilisateur[]>(`${this.apiUrl}/logisticiens`);
  }

  getUtilisateurById(id: number): Observable<Utilisateur> {
    return this.http.get<Utilisateur>(`${this.apiUrl}/${id}`);
  }

  getCurrentUser(): Observable<Utilisateur> {
    return this.http.get<Utilisateur>(`${this.apiUrl}/current`);
  }

  createUtilisateur(utilisateur: Utilisateur): Observable<Utilisateur> {
    return this.http.post<Utilisateur>(this.apiUrl, utilisateur);
  }

  updateUtilisateur(id: number, utilisateur: Utilisateur): Observable<Utilisateur> {
    return this.http.put<Utilisateur>(`${this.apiUrl}/${id}`, utilisateur);
  }

  deleteUtilisateur(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

