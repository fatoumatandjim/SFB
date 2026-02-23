import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Achat {
  id?: number;
  depotId?: number;
  depotNom?: string;
  produitId?: number;
  produitNom?: string;
  typeProduit?: string;
  quantite?: number;
  prixUnitaire?: number;
  montantTotal?: number;
  dateAchat?: string;
  description?: string;
  notes?: string;
  unite?: string;
  factureId?: number;
  factureNumero?: string;
  statutFacture?: string; // PAYEE, EMISE, etc. (pour compatibilité)
  transactionId?: number;
  transactionReference?: string;
  statutPaiement?: string; // VALIDE, EN_ATTENTE, REJETE, ANNULE
  cession?: boolean;
  clientId?: number;
  clientNom?: string;
}

export interface AchatPage {
  achats: Achat[];
  currentPage: number;
  totalPages: number;
  totalElements: number;
  size: number;
}

export interface AchatMarge {
  achatId?: number;
  quantiteAchetee?: number;
  prixUnitaireAchat?: number;
  montantTotalAchat?: number;
  fraisTotaux?: number;
  fraisProportionnels?: number;
  coutReelParLitre?: number;
  quantiteVendue?: number;
  prixVenteMoyen?: number;
  montantVenteTotal?: number;
  margeBrute?: number;
  margeNet?: number;
  margePourcentage?: number;
  stockRestant?: number;
  valeurStockRestant?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AchatsService {
  private apiUrl = `${environment.apiUrl}/achats`;

  constructor(private http: HttpClient) {}

  getAllAchats(): Observable<Achat[]> {
    return this.http.get<Achat[]>(this.apiUrl);
  }

  getAchatsPaginated(page: number = 0, size: number = 10): Observable<AchatPage> {
    return this.http.get<AchatPage>(`${this.apiUrl}/paginated`, {
      params: { page: page.toString(), size: size.toString() }
    });
  }

  getAchatsByDate(date: string, page: number = 0, size: number = 10): Observable<AchatPage> {
    return this.http.get<AchatPage>(`${this.apiUrl}/paginated/date`, {
      params: { date, page: page.toString(), size: size.toString() }
    });
  }

  getAchatsByDateRange(startDate: string, endDate: string, page: number = 0, size: number = 10): Observable<AchatPage> {
    return this.http.get<AchatPage>(`${this.apiUrl}/paginated/date-range`, {
      params: { startDate, endDate, page: page.toString(), size: size.toString() }
    });
  }

  getAchatById(id: number): Observable<Achat> {
    return this.http.get<Achat>(`${this.apiUrl}/${id}`);
  }

  getAchatsByDepot(depotId: number): Observable<Achat[]> {
    return this.http.get<Achat[]>(`${this.apiUrl}/depot/${depotId}`);
  }

  getAchatsByDepotPaginated(depotId: number, page: number = 0, size: number = 10): Observable<AchatPage> {
    return this.http.get<AchatPage>(`${this.apiUrl}/depot/${depotId}/paginated`, {
      params: { page: page.toString(), size: size.toString() }
    });
  }

  getAchatsByDepotAndDate(depotId: number, date: string, page: number = 0, size: number = 10): Observable<AchatPage> {
    return this.http.get<AchatPage>(`${this.apiUrl}/depot/${depotId}/paginated/date`, {
      params: { date, page: page.toString(), size: size.toString() }
    });
  }

  getAchatsByDepotAndDateRange(depotId: number, startDate: string, endDate: string, page: number = 0, size: number = 10): Observable<AchatPage> {
    return this.http.get<AchatPage>(`${this.apiUrl}/depot/${depotId}/paginated/date-range`, {
      params: { startDate, endDate, page: page.toString(), size: size.toString() }
    });
  }

  getAchatsByProduit(produitId: number): Observable<Achat[]> {
    return this.http.get<Achat[]>(`${this.apiUrl}/produit/${produitId}`);
  }

  createAchat(achat: Achat): Observable<Achat> {
    return this.http.post<Achat>(this.apiUrl, achat);
  }

  deleteAchat(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getMargeAchat(id: number): Observable<AchatMarge> {
    return this.http.get<AchatMarge>(`${this.apiUrl}/${id}/marge`);
  }

  createAchatWithFacture(dto: CreateAchatWithFactureDTO): Observable<Achat> {
    return this.http.post<Achat>(`${this.apiUrl}/with-facture`, dto);
  }

  payerAchat(dto: PayerAchatDTO): Observable<Achat> {
    return this.http.post<Achat>(`${this.apiUrl}/payer`, dto);
  }

  getAchatsByStatutFacture(statut: string, page: number = 0, size: number = 10): Observable<AchatPage> {
    return this.http.get<AchatPage>(`${this.apiUrl}/statut/${statut}/paginated`, {
      params: { page: page.toString(), size: size.toString() }
    });
  }

  getAchatsByStatutFactureAndDate(statut: string, date: string, page: number = 0, size: number = 10): Observable<AchatPage> {
    return this.http.get<AchatPage>(`${this.apiUrl}/statut/${statut}/paginated/date`, {
      params: { date, page: page.toString(), size: size.toString() }
    });
  }

  getAchatsByStatutFactureAndDateRange(statut: string, startDate: string, endDate: string, page: number = 0, size: number = 10): Observable<AchatPage> {
    return this.http.get<AchatPage>(`${this.apiUrl}/statut/${statut}/paginated/date-range`, {
      params: { startDate, endDate, page: page.toString(), size: size.toString() }
    });
  }

  createAchatCession(dto: CreateAchatCessionDTO): Observable<Achat> {
    return this.http.post<Achat>(`${this.apiUrl}/cession`, dto);
  }

  getAchatsCessionPaginated(page: number = 0, size: number = 10): Observable<AchatPage> {
    return this.http.get<AchatPage>(`${this.apiUrl}/cession/paginated`, {
      params: { page: page.toString(), size: size.toString() }
    });
  }

  getAchatsCessionByDate(date: string, page: number = 0, size: number = 10): Observable<AchatPage> {
    return this.http.get<AchatPage>(`${this.apiUrl}/cession/paginated/date`, {
      params: { date, page: page.toString(), size: size.toString() }
    });
  }

  getAchatsCessionByDateRange(startDate: string, endDate: string, page: number = 0, size: number = 10): Observable<AchatPage> {
    return this.http.get<AchatPage>(`${this.apiUrl}/cession/paginated/date-range`, {
      params: { startDate, endDate, page: page.toString(), size: size.toString() }
    });
  }
}

export interface CreateAchatCessionDTO {
  clientId: number;
  depotId: number;
  produitId: number;
  quantite: number;
  description?: string;
  notes?: string;
  unite?: string;
}

export interface CreateAchatWithFactureDTO {
  depotId: number;
  produitId: number;
  fournisseurId: number;
  quantite: number;
  prixUnitaire: number;
  description?: string;
  notes?: string;
  unite?: string;
}

export interface PayerAchatDTO {
  achatId: number;
  compteBancaireId: number;
}
