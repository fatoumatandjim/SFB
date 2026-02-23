import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Stock {
  id?: number;
  produitId: number;
  produitNom?: string;
  typeProduit?: string;
  quantite: number;
  /** Quantité en cession (achats de cession, sans paiement) */
  quantityCession?: number;
  depotId: number;
  depotNom?: string;
  seuilMinimum?: number;
  prixUnitaire?: number;
  unite?: string;
  dateDerniereMiseAJour?: string;
}

export interface StockStats {
  totalUnites: number;
  evolutionTotalUnites: string;
  periodeTotalUnites: string;
  totalDepots: number;
  villesDepots: number;
  produitsCritiques: number;
  urgentProduitsCritiques: boolean;
  valeurStock: number;
  evolutionValeurStock: string;
  periodeValeurStock: string;
}

@Injectable({
  providedIn: 'root'
})
export class StocksService {
  private apiUrl = `${environment.apiUrl}/stocks`;

  constructor(private http: HttpClient) {}

  getAllStocks(): Observable<Stock[]> {
    return this.http.get<Stock[]>(this.apiUrl);
  }

  getStats(): Observable<StockStats> {
    return this.http.get<StockStats>(`${this.apiUrl}/stats`);
  }

  getStockById(id: number): Observable<Stock> {
    return this.http.get<Stock>(`${this.apiUrl}/${id}`);
  }

  getStocksByDepot(depotId: number): Observable<Stock[]> {
    return this.http.get<Stock[]>(`${this.apiUrl}/depot/${depotId}`);
  }

  getStocksByProduit(produitId: number): Observable<Stock[]> {
    return this.http.get<Stock[]>(`${this.apiUrl}/produit/${produitId}`);
  }

  createStock(stock: Stock): Observable<Stock> {
    return this.http.post<Stock>(this.apiUrl, stock);
  }

  updateStock(id: number, stock: Stock): Observable<Stock> {
    return this.http.put<Stock>(`${this.apiUrl}/${id}`, stock);
  }

  deleteStock(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

