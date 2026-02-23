import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Mouvement {
  id?: number;
  stockId?: number;
  typeMouvement: 'ENTREE' | 'SORTIE' | 'TRANSFERT' | 'INVENTAIRE';
  quantite: number;
  unite?: string;
  description?: string;
  dateMouvement?: string;
}

export interface MouvementPage {
  mouvements: Mouvement[];
  currentPage: number;
  totalPages: number;
  totalElements: number;
  pageSize: number;
}

@Injectable({
  providedIn: 'root'
})
export class MouvementsService {
  private apiUrl = `${environment.apiUrl}/mouvements`;

  constructor(private http: HttpClient) {}

  getRecentMouvements(limit: number = 5): Observable<Mouvement[]> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.http.get<Mouvement[]>(`${this.apiUrl}/recent`, { params });
  }

  getMouvementsPaginated(page: number = 0, size: number = 10): Observable<MouvementPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<MouvementPage>(`${this.apiUrl}/paginated`, { params });
  }

  getMouvementsByDate(date: string, page: number = 0, size: number = 10): Observable<MouvementPage> {
    const params = new HttpParams()
      .set('date', date)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<MouvementPage>(`${this.apiUrl}/by-date`, { params });
  }

  getMouvementsByDateRange(
    startDate: string,
    endDate: string,
    page: number = 0,
    size: number = 10
  ): Observable<MouvementPage> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate)
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<MouvementPage>(`${this.apiUrl}/by-date-range`, { params });
  }

  getMouvementsByStock(stockId: number): Observable<Mouvement[]> {
    return this.http.get<Mouvement[]>(`${this.apiUrl}/stock/${stockId}`);
  }

  createMouvement(mouvement: Mouvement): Observable<Mouvement> {
    return this.http.post<Mouvement>(this.apiUrl, mouvement);
  }
}

