import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Produit {
  id?: number;
  nom: string;
  typeProduit: 'ESSENCE' | 'GAZOLE' | 'KEROSENE' | 'GPL' | 'AUTRE';
  description?: string;
  dateCreation?: string;
}

export interface StockInfo {
  stockId?: number;
  depotNom?: string;
  depotVille?: string;
  quantite?: number;
  unite?: string;
  seuilMinimum?: number;
  prixUnitaire?: number;
  citerne?: boolean; // Indique si c'est un stock citerne
  nom?: string; // Nom du stock
}

export interface ProduitAvecStocks {
  id?: number;
  nom: string;
  typeProduit: string;
  description?: string;
  quantiteTotale: number;
  stocks: StockInfo[];
}

@Injectable({
  providedIn: 'root'
})
export class ProduitsService {
  private apiUrl = `${environment.apiUrl}/produits`;

  constructor(private http: HttpClient) {}

  getAllProduits(): Observable<Produit[]> {
    return this.http.get<Produit[]>(this.apiUrl);
  }

  getAllProduitsAvecStocks(): Observable<ProduitAvecStocks[]> {
    return this.http.get<ProduitAvecStocks[]>(`${this.apiUrl}/avec-stocks`);
  }

  getProduitById(id: number): Observable<Produit> {
    return this.http.get<Produit>(`${this.apiUrl}/${id}`);
  }

  getProduitByIdAvecStocks(id: number): Observable<ProduitAvecStocks> {
    return this.http.get<ProduitAvecStocks>(`${this.apiUrl}/${id}/avec-stocks`);
  }

  getProduitByNom(nom: string): Observable<Produit> {
    return this.http.get<Produit>(`${this.apiUrl}/nom/${nom}`);
  }

  createProduit(produit: Produit): Observable<Produit> {
    return this.http.post<Produit>(this.apiUrl, produit);
  }

  updateProduit(id: number, produit: Produit): Observable<Produit> {
    return this.http.put<Produit>(`${this.apiUrl}/${id}`, produit);
  }

  deleteProduit(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

