import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Alerte {
  id: number;
  type: string;
  message: string;
  date: string;
  lu: boolean;
  priorite: string;
  lien?: string;
  entiteType?: string;
  entiteId?: number;
}

export interface AlertePage {
  content: Alerte[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

@Injectable({
  providedIn: 'root'
})
export class AlerteService {
  private apiUrl = `${environment.apiUrl}/alertes`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Alerte[]> {
    return this.http.get<Alerte[]>(this.apiUrl);
  }

  /** Pagination côté backend : page 0-based, size par page. */
  getPage(page: number, size: number): Observable<AlertePage> {
    return this.http.get<AlertePage>(`${this.apiUrl}/page`, {
      params: { page: String(page), size: String(size) }
    });
  }

  getNonLues(): Observable<Alerte[]> {
    return this.http.get<Alerte[]>(`${this.apiUrl}/non-lues`);
  }

  getById(id: number): Observable<Alerte> {
    return this.http.get<Alerte>(`${this.apiUrl}/${id}`);
  }

  marquerCommeLue(id: number, alerte: Alerte): Observable<Alerte> {
    return this.http.put<Alerte>(`${this.apiUrl}/${id}`, { ...alerte, lu: true });
  }
}
