import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Capitale {
  fonds: {
    totalBanques: string;
    totalBanquesValue: number;
    totalCaisses: string;
    totalCaissesValue: number;
    totalGeneral: string;
    totalGeneralValue: number;
  };
  stocks: {
    stocksDepot: Array<{
      produitId: number;
      produitNom: string;
      typeProduit: string;
      quantite: number;
      valeur: string;
      valeurValue: number;
    }>;
    totalStocksDepot: string;
    totalStocksDepotValue: number;
    stocksCamion: Array<{
      produitId: number;
      produitNom: string;
      typeProduit: string;
      quantite: number;
      valeur: string;
      valeurValue: number;
    }>;
    totalStocksCamion: string;
    totalStocksCamionValue: number;
    totalStocks: string;
    totalStocksValue: number;
  };
  depensesInvestissement: {
    total: string;
    totalValue: number;
  };
  totalCapital: string;
  totalCapitalValue: number;
}

@Injectable({
  providedIn: 'root'
})
export class CapitaleService {
  private apiUrl = `${environment.apiUrl}/capitale`;

  constructor(private http: HttpClient) {}

  getCapitale(): Observable<Capitale> {
    return this.http.get<Capitale>(this.apiUrl);
  }

  getCapitaleByMonth(year: number, month: number): Observable<Capitale> {
    const params = new HttpParams()
      .set('year', year.toString())
      .set('month', month.toString());
    return this.http.get<Capitale>(`${this.apiUrl}/month`, { params });
  }

  getCapitaleByDateRange(startDate: string, endDate: string): Observable<Capitale> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get<Capitale>(`${this.apiUrl}/range`, { params });
  }
}
