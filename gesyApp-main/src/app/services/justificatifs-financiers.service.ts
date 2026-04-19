import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, forkJoin, of } from 'rxjs';
import { environment } from '../../environments/environment';

/** Aligné sur {@code JustificatifFinancier} (backend). */
export const JUSTIFICATIF_OWNER_DEPENSE = 'DEPENSE';
export const JUSTIFICATIF_OWNER_PAIEMENT = 'PAIEMENT';
export const JUSTIFICATIF_OWNER_TRANSACTION = 'TRANSACTION';

export interface JustificatifFinancier {
  id: number;
  ownerType: string;
  ownerId: number;
  storedFileName?: string;
  originalFilename: string;
  createdAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class JustificatifsFinanciersService {
  private readonly apiUrl = `${environment.apiUrl}/justificatifs-financiers`;

  constructor(private http: HttpClient) {}

  list(ownerType: string, ownerId: number): Observable<JustificatifFinancier[]> {
    const params = new HttpParams()
      .set('ownerType', ownerType.trim().toUpperCase())
      .set('ownerId', ownerId.toString());
    return this.http.get<JustificatifFinancier[]>(this.apiUrl, { params });
  }

  upload(ownerType: string, ownerId: number, files: File[]): Observable<JustificatifFinancier[]> {
    const form = new FormData();
    form.set('ownerType', ownerType.trim().toUpperCase());
    form.set('ownerId', ownerId.toString());
    for (const f of files) {
      form.append('files', f, f.name);
    }
    return this.http.post<JustificatifFinancier[]>(this.apiUrl, form);
  }

  /**
   * Téléverse les mêmes fichiers pour plusieurs écritures (ex. virement → 2 transactions).
   * Ne rien appeler si {@code files} ou {@code ownerIds} est vide.
   */
  uploadForOwnerIds(
    ownerType: string,
    ownerIds: number[],
    files: File[]
  ): Observable<JustificatifFinancier[][]> {
    const ot = ownerType.trim().toUpperCase();
    const ids = ownerIds.filter((id) => id != null && id > 0);
    if (!files.length || !ids.length) {
      return of([]);
    }
    return forkJoin(ids.map((id) => this.upload(ot, id, files)));
  }

  download(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${id}/download`, { responseType: 'blob' });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
