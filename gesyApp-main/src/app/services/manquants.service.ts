import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Manquant {
  id?: number;
  voyageId: number;
  numeroVoyage?: string;
  quantite: number;
  dateCreation: string;
  description?: string;
  creePar?: string;
  camionImmatriculation?: string;
  produitNom?: string;
  depotNom?: string;
}

export interface ManquantPage {
  manquants: Manquant[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

@Injectable({
  providedIn: 'root'
})
export class ManquantsService {
  private apiUrl = `${environment.apiUrl}/manquants`;

  constructor(private http: HttpClient) {}

  getManquants(page: number = 0, size: number = 10): Observable<ManquantPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<ManquantPage>(this.apiUrl, { params });
  }

  getAllManquants(): Observable<Manquant[]> {
    return this.http.get<Manquant[]>(`${this.apiUrl}/all`);
  }

  getManquantById(id: number): Observable<Manquant> {
    return this.http.get<Manquant>(`${this.apiUrl}/${id}`);
  }

  getManquantsByVoyageId(voyageId: number): Observable<Manquant[]> {
    return this.http.get<Manquant[]>(`${this.apiUrl}/voyage/${voyageId}`);
  }

  getManquantsByDate(date: string, page: number = 0, size: number = 10): Observable<ManquantPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<ManquantPage>(`${this.apiUrl}/date/${date}`, { params });
  }

  getManquantsByDateRange(startDate: string, endDate: string, page: number = 0, size: number = 10): Observable<ManquantPage> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<ManquantPage>(`${this.apiUrl}/date-range`, { params });
  }

  deleteManquant(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

