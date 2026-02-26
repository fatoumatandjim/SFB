import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Camion {
  id?: number;
  immatriculation: string;
  modele: string;
  marque: string;
  annee: number;
  type: string;
  capacite: number;
  kilometrage: number;
  dernierControle?: string;
  statut: 'DISPONIBLE' | 'EN_ROUTE' | 'EN_MAINTENANCE' | 'HORS_SERVICE';
  loue?: boolean;
  montantLocation?: number;
  montantLocationInitial?: number;
  fournisseurId?: number | null;
  fournisseurNom?: string;
  fournisseurEmail?: string;
  responsableId?: number | null;
  responsableIdentifiant?: string;
}

export interface CamionWithVoyagesCount {
  id?: number;
  immatriculation: string;
  modele: string;
  marque: string;
  annee: number;
  type: string;
  capacite: number;
  statut: string;
  nombreVoyages: number;
  nombreVoyagesNonCession?: number;
}

@Injectable({
  providedIn: 'root'
})
export class CamionsService {
  private apiUrl = `${environment.apiUrl}/camions`;

  constructor(private http: HttpClient) {}

  getAllCamions(): Observable<Camion[]> {
    return this.http.get<Camion[]>(this.apiUrl);
  }

  getCamionById(id: number): Observable<Camion> {
    return this.http.get<Camion>(`${this.apiUrl}/${id}`);
  }

  getCamionByImmatriculation(immatriculation: string): Observable<Camion> {
    return this.http.get<Camion>(`${this.apiUrl}/immatriculation/${immatriculation}`);
  }

  getCamionsByFournisseur(fournisseurId: number): Observable<CamionWithVoyagesCount[]> {
    return this.http.get<CamionWithVoyagesCount[]>(`${this.apiUrl}/fournisseur/${fournisseurId}`);
  }

  createCamion(camion: Camion): Observable<Camion> {
    return this.http.post<Camion>(this.apiUrl, camion);
  }

  updateCamion(id: number, camion: Camion): Observable<Camion> {
    return this.http.put<Camion>(`${this.apiUrl}/${id}`, camion);
  }

  deleteCamion(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

