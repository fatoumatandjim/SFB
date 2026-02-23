import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Transaction {
  id?: number;
  type: string;
  montant: number;
  date: string;
  compteId?: number;
  camionId?: number;
  factureId?: number;
  voyageId?: number;
  caisseId?: number;
  transactionLieeId?: number;
  statut: string;
  description?: string;
  reference?: string;
  beneficiaire?: string;
}

export interface VirementRequest {
  montant: number;
  date: string;
  compteSourceId?: number;
  compteDestinationId?: number;
  caisseId?: number;
  caisseSourceId?: number;
  caisseDestinationId?: number;
  type: 'VIREMENT' | 'VIREMENT_SIMPLE' | 'DEPOT' | 'RETRAIT';
  statut: 'EN_ATTENTE' | 'VALIDE' | 'REJETE' | 'ANNULE';
  description?: string;
  reference?: string;
}

@Injectable({
  providedIn: 'root'
})
export class TransactionsService {
  private apiUrl = `${environment.apiUrl}/transactions`;

  constructor(private http: HttpClient) {}

  getAllTransactions(): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(this.apiUrl);
  }

  getTransactionById(id: number): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.apiUrl}/${id}`);
  }

  getTransactionsByCamion(camionId: number): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.apiUrl}/camion/${camionId}`);
  }

  getTransactionsByFacture(factureId: number): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.apiUrl}/facture/${factureId}`);
  }

  createTransaction(transaction: Transaction): Observable<Transaction> {
    return this.http.post<Transaction>(this.apiUrl, transaction);
  }

  createPaiement(transaction: Transaction): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.apiUrl}/paiement`, transaction);
  }

  updateTransaction(id: number, transaction: Transaction): Observable<Transaction> {
    return this.http.put<Transaction>(`${this.apiUrl}/${id}`, transaction);
  }

  deleteTransaction(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  createVirement(request: VirementRequest): Observable<Transaction[]> {
    return this.http.post<Transaction[]>(`${this.apiUrl}/virement`, request);
  }

  getRecentTransactions(limit: number = 5): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.apiUrl}/recent?limit=${limit}`);
  }

  getTransactionsPaginated(page: number = 0, size: number = 10): Observable<TransactionPage> {
    return this.http.get<TransactionPage>(`${this.apiUrl}/paginated?page=${page}&size=${size}`);
  }

  getTransactionsByDate(date: string, page: number = 0, size: number = 10): Observable<TransactionPage> {
    return this.http.get<TransactionPage>(`${this.apiUrl}/paginated/date?date=${date}&page=${page}&size=${size}`);
  }

  getTransactionsByDateRange(startDate: string, endDate: string, page: number = 0, size: number = 10): Observable<TransactionPage> {
    return this.http.get<TransactionPage>(`${this.apiUrl}/paginated/range?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`);
  }

  // Méthodes pour récupérer toutes les transactions (sans pagination) pour l'export
  getTransactionsByDateAll(date: string): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.apiUrl}/date?date=${date}`);
  }

  getTransactionsByDateRangeAll(startDate: string, endDate: string): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.apiUrl}/range?startDate=${startDate}&endDate=${endDate}`);
  }

  // Méthodes pour filtrer par compte bancaire avec pagination
  getTransactionsByCompteIdPaginated(compteId: number, page: number = 0, size: number = 10): Observable<TransactionPage> {
    return this.http.get<TransactionPage>(`${this.apiUrl}/compte/${compteId}/paginated?page=${page}&size=${size}`);
  }

  getTransactionsByCompteIdAndDate(compteId: number, date: string, page: number = 0, size: number = 10): Observable<TransactionPage> {
    return this.http.get<TransactionPage>(`${this.apiUrl}/compte/${compteId}/paginated/date?date=${date}&page=${page}&size=${size}`);
  }

  getTransactionsByCompteIdAndDateRange(compteId: number, startDate: string, endDate: string, page: number = 0, size: number = 10): Observable<TransactionPage> {
    return this.http.get<TransactionPage>(`${this.apiUrl}/compte/${compteId}/paginated/range?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`);
  }

  // Méthodes pour filtrer par caisse avec pagination
  getTransactionsByCaisseIdPaginated(caisseId: number, page: number = 0, size: number = 10): Observable<TransactionPage> {
    return this.http.get<TransactionPage>(`${this.apiUrl}/caisse/${caisseId}/paginated?page=${page}&size=${size}`);
  }

  getTransactionsByCaisseIdAndDate(caisseId: number, date: string, page: number = 0, size: number = 10): Observable<TransactionPage> {
    return this.http.get<TransactionPage>(`${this.apiUrl}/caisse/${caisseId}/paginated/date?date=${date}&page=${page}&size=${size}`);
  }

  getTransactionsByCaisseIdAndDateRange(caisseId: number, startDate: string, endDate: string, page: number = 0, size: number = 10): Observable<TransactionPage> {
    return this.http.get<TransactionPage>(`${this.apiUrl}/caisse/${caisseId}/paginated/range?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`);
  }

  // Méthodes pour filtrer uniquement les comptes bancaires ou les caisses
  getTransactionsByComptesBancairesOnly(page: number = 0, size: number = 10): Observable<TransactionPage> {
    return this.http.get<TransactionPage>(`${this.apiUrl}/comptes-bancaires/paginated?page=${page}&size=${size}`);
  }

  getTransactionsByCaissesOnly(page: number = 0, size: number = 10): Observable<TransactionPage> {
    return this.http.get<TransactionPage>(`${this.apiUrl}/caisses/paginated?page=${page}&size=${size}`);
  }

  getStats(): Observable<TransactionStats> {
    return this.http.get<TransactionStats>(`${this.apiUrl}/stats`);
  }

  /**
   * Filtre personnalisé: par type de transaction, optionnellement par date ou intervalle de dates.
   * Retourne les transactions paginées avec le nombre total et le montant total des éléments filtrés.
   */
  getTransactionsFilter(params: {
    type?: string;
    date?: string;
    startDate?: string;
    endDate?: string;
    page?: number;
    size?: number;
  }): Observable<TransactionFilterResult> {
    let url = `${this.apiUrl}/filter?page=${params.page ?? 0}&size=${params.size ?? 10}`;
    if (params.type) url += `&type=${encodeURIComponent(params.type)}`;
    if (params.date) url += `&date=${params.date}`;
    if (params.startDate) url += `&startDate=${params.startDate}`;
    if (params.endDate) url += `&endDate=${params.endDate}`;
    return this.http.get<TransactionFilterResult>(url);
  }
}

export interface TransactionPage {
  transactions: Transaction[];
  currentPage: number;
  totalPages: number;
  totalElements: number;
  size: number;
}

export interface TransactionFilterResult {
  transactions: Transaction[];
  totalCount: number;
  totalMontant: number;
  currentPage: number;
  totalPages: number;
  size: number;
}

export interface TransactionStats {
  paiementsEffectues: {
    total: number;
    montant: number;
    periode: string;
    evolution: string;
  };
  paiementsEnAttente: {
    total: number;
    montant: number;
    pourcentage: string;
  };
  paiementsEchec: {
    total: number;
    montant: number;
    urgent: boolean;
  };
}

