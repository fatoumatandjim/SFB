import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Client {
  id?: number;
  nom: string;
  email: string;
  telephone: string;
  adresse: string;
  type: string;
  codeClient?: string;
  ville?: string;
  pays?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ClientsService {
  private apiUrl = `${environment.apiUrl}/clients`;

  constructor(private http: HttpClient) {}

  getAllClients(): Observable<Client[]> {
    return this.http.get<Client[]>(this.apiUrl);
  }

  /** Clients ayant au moins un achat (pour voyage de type cession) */
  getClientsWithAtLeastOneAchat(): Observable<Client[]> {
    return this.http.get<Client[]>(`${this.apiUrl}/with-achat`);
  }

  getClientById(id: number): Observable<Client> {
    return this.http.get<Client>(`${this.apiUrl}/${id}`);
  }

  createClient(client: Client): Observable<Client> {
    return this.http.post<Client>(this.apiUrl, client);
  }

  updateClient(id: number, client: Client): Observable<Client> {
    return this.http.put<Client>(`${this.apiUrl}/${id}`, client);
  }

  deleteClient(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

