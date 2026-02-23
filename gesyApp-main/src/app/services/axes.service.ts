import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';

export interface Axe {
  id: number;
  nom: string;
}

@Injectable({
  providedIn: 'root'
})
export class AxesService {
  private apiUrl = `${environment.apiUrl}/axes`;

  constructor(private http: HttpClient) { }

  getAllAxes(): Observable<Axe[]> {
    return this.http.get<Axe[]>(this.apiUrl);
  }

  getAxeById(id: number): Observable<Axe> {
    return this.http.get<Axe>(`${this.apiUrl}/${id}`);
  }

  createAxe(axe: { nom: string }): Observable<Axe> {
    return this.http.post<Axe>(this.apiUrl, axe);
  }

  updateAxe(id: number, axe: { nom: string }): Observable<Axe> {
    return this.http.put<Axe>(`${this.apiUrl}/${id}`, axe);
  }

  deleteAxe(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

