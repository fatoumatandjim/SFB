import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Transaction } from './transactions.service';
import { Facture } from './factures.service';

export interface EtatVoyage {
  id?: number;
  etat: string;
  dateHeure: string;
  valider: boolean;
}

export interface ClientVoyage {
  id?: number;
  voyageId?: number;
  clientId?: number;
  clientNom?: string;
  clientEmail?: string;
  quantite?: number;
  prixAchat?: number;
  statut?: 'LIVRER' | 'NON_LIVRE';
  manquant?: number;
  dateCreation?: string;
  dateModification?: string;
}

export interface Voyage {
  id?: number;
  numeroVoyage?: string;
  camionId: number;
  camionImmatriculation?: string;
  clientId?: number | null; // Pour compatibilité, mais utiliser clientVoyages
  clientNom?: string;
  clientEmail?: string;
  clientVoyages?: ClientVoyage[]; // Liste des clients associés au voyage
  transitaireId?: number | null;
  transitaireNom?: string;
  transitaireIdentifiant?: string;
  transitairePhone?: string;
  axeId?: number | null;
  axeNom?: string;
  dateDepart?: string;
  dateArrivee?: string;
  destination?: string;
  lieuDepart?: string;
  statut?: 'CHARGEMENT' | 'CHARGE' | 'DEPART' | 'ARRIVER' | 'DOUANE' | 'RECEPTIONNER' | 'LIVRE' | 'PARTIELLEMENT_DECHARGER' | 'DECHARGER';
  quantite?: number;
  manquant?: number;
  prixUnitaire?: number;
  produitId?: number;
  produitNom?: string;
  typeProduit?: string;
  notes?: string;
  transactions?: Transaction[];
  etats?: EtatVoyage[];
  depotId?: number;
  depotNom?: string;
  /** ID du compte (utilisateur) responsable du voyage */
  responsableId?: number;
  responsableIdentifiant?: string;
  coutVoyage?: number;
  compteId?: number;
  caisseId?: number;
  factureId?: number; // Pour compatibilité, mais utiliser factures
  factureNumero?: string;
  factureMontant?: number;
  factureMontantPaye?: number;
  factureStatut?: string;
  factures?: Facture[]; // Liste des factures associées au voyage
  numeroBonEnlevement?: string;
  declarer?: boolean;
  passager?: string;
  chauffeur?: string;
  numeroChauffeur?: string;
  /** Vente de type cession : pas de cout du voyage, client ayant déjà un achat */
  cession?: boolean;
  /** Voyage libéré par le transitaire */
  liberer?: boolean;
}

export interface ClientVoyages {
  clientId: number;
  clientNom: string;
  clientEmail?: string;
  voyages: Voyage[];
  nombreVoyages: number;
}

export interface VoyagesParClientPage {
  clientsVoyages: ClientVoyages[];
  currentPage: number;
  totalPages: number;
  totalElements: number;
  size: number;
}

export interface VoyagePage {
  voyages: Voyage[];
  currentPage: number;
  totalPages: number;
  totalElements: number;
  size: number;
}

@Injectable({
  providedIn: 'root'
})
export class VoyagesService {
  private apiUrl = `${environment.apiUrl}/voyages`;

  constructor(private http: HttpClient) { }

  getAllVoyages(): Observable<Voyage[]> {
    return this.http.get<Voyage[]>(this.apiUrl);
  }

  getVoyageById(id: number): Observable<Voyage> {
    return this.http.get<Voyage>(`${this.apiUrl}/${id}`);
  }

  getVoyagesByCamion(camionId: number): Observable<Voyage[]> {
    return this.http.get<Voyage[]>(`${this.apiUrl}/camion/${camionId}`);
  }

  getVoyagesByClient(clientId: number): Observable<Voyage[]> {
    return this.http.get<Voyage[]>(`${this.apiUrl}/client/${clientId}`);
  }

  getVoyagesByTransitaire(transitaireId: number): Observable<Voyage[]> {
    return this.http.get<Voyage[]>(`${this.apiUrl}/transitaire/${transitaireId}`);
  }

  getVoyagesByAxe(axeId: number): Observable<Voyage[]> {
    return this.http.get<Voyage[]>(`${this.apiUrl}/axe/${axeId}`);
  }

  getVoyagesByAxePaginated(axeId: number, page: number = 0, size: number = 10): Observable<VoyagePage> {
    return this.http.get<VoyagePage>(`${this.apiUrl}/axe/${axeId}/paginated?page=${page}&size=${size}`);
  }

  getVoyagesNonDeclaresByTransitaire(transitaireId: number): Observable<Voyage[]> {
    return this.http.get<Voyage[]>(`${this.apiUrl}/transitaire/${transitaireId}/non-declares`);
  }

  getVoyagesNonDeclaresByTransitaireIdentifiant(identifiant: string): Observable<Voyage[]> {
    return this.http.get<Voyage[]>(`${this.apiUrl}/transitaire/identifiant/${identifiant}/non-declares`);
  }

  getTransitaireStatsByIdentifiant(identifiant: string): Observable<TransitaireStats> {
    return this.http.get<TransitaireStats>(`${this.apiUrl}/transitaire/identifiant/${identifiant}/stats`);
  }

  /** Voyages en cours du transitaire (non déchargés), paginés */
  getVoyagesEnCoursByTransitaireIdentifiant(
    identifiant: string,
    page: number = 0,
    size: number = 10
  ): Observable<VoyagePage> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<VoyagePage>(
      `${this.apiUrl}/transitaire/identifiant/${identifiant}/en-cours`,
      { params }
    );
  }

  passerNonDeclarer(voyageId: number): Observable<Voyage> {
    return this.http.put<Voyage>(`${this.apiUrl}/${voyageId}/passer-non-declarer`, {});
  }

  libererVoyage(voyageId: number): Observable<Voyage> {
    return this.http.put<Voyage>(`${this.apiUrl}/${voyageId}/liberer`, {});
  }

  libererVoyages(voyageIds: number[]): Observable<Voyage[]> {
    return this.http.put<Voyage[]>(`${this.apiUrl}/liberer-multiple`, voyageIds);
  }

  createVoyage(voyage: Voyage): Observable<Voyage> {
    return this.http.post<Voyage>(this.apiUrl, voyage);
  }

  updateVoyage(id: number, voyage: Voyage): Observable<Voyage> {
    return this.http.put<Voyage>(`${this.apiUrl}/${id}`, voyage);
  }

  donnerPrixAchat(voyageId: number, clientVoyageId: number, prixAchat: number): Observable<Voyage> {
    return this.http.put<Voyage>(`${this.apiUrl}/${voyageId}/prix-achat?clientVoyageId=${clientVoyageId}&prixAchat=${prixAchat}`, {});
  }

  updateClientVoyageQuantite(
    voyageId: number,
    clientVoyageId: number,
    clientId: number,
    quantite: number
  ): Observable<Voyage> {
    return this.http.put<Voyage>(
      `${this.apiUrl}/${voyageId}/client-voyage/quantite?clientVoyageId=${clientVoyageId}&clientId=${clientId}&quantite=${quantite}`,
      {}
    );
  }

  assignTransitaire(voyageId: number, transitaireId: number): Observable<Voyage> {
    return this.http.put<Voyage>(`${this.apiUrl}/${voyageId}/transitaire?transitaireId=${transitaireId}`, {});
  }

  declarerVoyage(voyageId: number, compteId?: number, caisseId?: number): Observable<Voyage> {
    let url = `${this.apiUrl}/${voyageId}/declarer?`;
    if (compteId) {
      url += `compteId=${compteId}`;
    }
    if (caisseId) {
      url += `caisseId=${caisseId}`;
    }
    return this.http.put<Voyage>(url, {});
  }

  getArchivedVoyagesByTransitaire(transitaireId: number, page: number = 0, size: number = 10): Observable<VoyagePage> {
    return this.http.get<VoyagePage>(`${this.apiUrl}/transitaire/${transitaireId}/archives?page=${page}&size=${size}`);
  }

  getArchivedVoyagesByTransitaireAndDate(transitaireId: number, date: string, page: number = 0, size: number = 10): Observable<VoyagePage> {
    return this.http.get<VoyagePage>(`${this.apiUrl}/transitaire/${transitaireId}/archives/date?date=${date}&page=${page}&size=${size}`);
  }

  getArchivedVoyagesByTransitaireAndDateRange(transitaireId: number, startDate: string, endDate: string, page: number = 0, size: number = 10): Observable<VoyagePage> {
    return this.http.get<VoyagePage>(`${this.apiUrl}/transitaire/${transitaireId}/archives/date-range?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`);
  }

  getArchivedVoyagesByTransitaireIdentifiant(identifiant: string, page: number = 0, size: number = 10): Observable<VoyagePage> {
    return this.http.get<VoyagePage>(`${this.apiUrl}/transitaire/identifiant/${identifiant}/archives?page=${page}&size=${size}`);
  }

  getArchivedVoyagesByTransitaireIdentifiantAndDate(identifiant: string, date: string, page: number = 0, size: number = 10): Observable<VoyagePage> {
    return this.http.get<VoyagePage>(`${this.apiUrl}/transitaire/identifiant/${identifiant}/archives/date?date=${date}&page=${page}&size=${size}`);
  }

  getArchivedVoyagesByTransitaireIdentifiantAndDateRange(identifiant: string, startDate: string, endDate: string, page: number = 0, size: number = 10): Observable<VoyagePage> {
    return this.http.get<VoyagePage>(`${this.apiUrl}/transitaire/identifiant/${identifiant}/archives/date-range?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`);
  }

  getArchivedVoyages(page: number = 0, size: number = 10): Observable<VoyagePage> {
    return this.http.get<VoyagePage>(`${this.apiUrl}/archives?page=${page}&size=${size}`);
  }

  getArchivedVoyagesByDate(date: string, page: number = 0, size: number = 10): Observable<VoyagePage> {
    return this.http.get<VoyagePage>(`${this.apiUrl}/archives/date?date=${date}&page=${page}&size=${size}`);
  }

  getArchivedVoyagesByDateRange(startDate: string, endDate: string, page: number = 0, size: number = 10): Observable<VoyagePage> {
    return this.http.get<VoyagePage>(`${this.apiUrl}/archives/date-range?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`);
  }

  // Voyages passés non déclarés
  getVoyagesPassesNonDeclares(): Observable<Voyage[]> {
    return this.http.get<Voyage[]>(`${this.apiUrl}/passes-non-declares`);
  }

  getVoyagesPassesNonDeclaresPaginated(page: number = 0, size: number = 10): Observable<VoyagePage> {
    return this.http.get<VoyagePage>(`${this.apiUrl}/passes-non-declares/paginated?page=${page}&size=${size}`);
  }

  getVoyagesAvecClientSansFacture(page: number = 0, size: number = 10): Observable<VoyagePage> {
    return this.http.get<VoyagePage>(`${this.apiUrl}/avec-client-sans-facture?page=${page}&size=${size}`);
  }

  /** Voyages attribués (non cession) sans prix de transport — pour le comptable */
  getVoyagesSansPrixTransport(page: number = 0, size: number = 10): Observable<VoyagePage> {
    return this.http.get<VoyagePage>(`${this.apiUrl}/sans-prix-transport?page=${page}&size=${size}`);
  }

  getVoyagesAvecClientSansFactureGroupesParClient(page: number = 0, size: number = 10): Observable<VoyagesParClientPage> {
    return this.http.get<VoyagesParClientPage>(`${this.apiUrl}/avec-client-sans-facture/groupes-par-client?page=${page}&size=${size}`);
  }

  getVoyagesPartiellementDecharges(page: number = 0, size: number = 10): Observable<VoyagePage> {
    return this.http.get<VoyagePage>(`${this.apiUrl}/partiellement-decharges?page=${page}&size=${size}`);
  }

  getVoyagesEnCours(page: number = 0, size: number = 10): Observable<VoyagePage> {
    return this.http.get<VoyagePage>(`${this.apiUrl}/en-cours?page=${page}&size=${size}`);
  }

  /** Voyages en cours (non déchargés) avec au moins un client assigné — pour rapport PDF camions/clients */
  getVoyagesEnCoursAvecClients(): Observable<Voyage[]> {
    return this.http.get<Voyage[]>(`${this.apiUrl}/en-cours-avec-clients`);
  }

  deleteVoyage(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  declarerVoyagesMultiple(voyageIds: number[], compteId?: number, caisseId?: number): Observable<Voyage[]> {
    let url = `${this.apiUrl}/declarer-multiple?`;
    if (compteId) {
      url += `compteId=${compteId}`;
    }
    if (caisseId) {
      url += `caisseId=${caisseId}`;
    }
    return this.http.put<Voyage[]>(url, voyageIds);
  }

  getTransitaireStats(transitaireId: number): Observable<TransitaireStats> {
    return this.http.get<TransitaireStats>(`${this.apiUrl}/transitaire/${transitaireId}/stats`);
  }

  getVoyagesByDepot(depotId: number): Observable<Voyage[]> {
    return this.http.get<Voyage[]>(`${this.apiUrl}/depot/${depotId}`);
  }

  getVoyagesByDepotChargement(depotId: number): Observable<Voyage[]> {
    return this.http.get<Voyage[]>(`${this.apiUrl}/depot/${depotId}/chargement`);
  }

  findByUtilisateurIdentifiant(identifiant: string): Observable<Voyage[]> {
    return this.http.get<Voyage[]>(`${this.apiUrl}/utilisateur/${identifiant}`);
  }

  getVoyagesChargesByIdentifiant(
    identifiant: string,
    page: number = 0,
    size: number = 10,
    date?: string,
    startDate?: string,
    endDate?: string
  ): Observable<VoyagePage> {
    let url = `${this.apiUrl}/charges/identifiant/${identifiant}?page=${page}&size=${size}`;
    if (date) {
      url += `&date=${date}`;
    }
    if (startDate) {
      url += `&startDate=${startDate}`;
    }
    if (endDate) {
      url += `&endDate=${endDate}`;
    }
    return this.http.get<VoyagePage>(url);
  }

  getVoyagesByUtilisateurIdentifiant(identifiant: string): Observable<Voyage[]> {
    return this.http.get<Voyage[]>(`${this.apiUrl}/utilisateur/${identifiant}/chargement`);
  }

  updateStatutMultiple(voyageIds: number[], statut: string): Observable<Voyage[]> {
    return this.http.put<Voyage[]>(`${this.apiUrl}/update-statut-multiple?statut=${statut}`, voyageIds);
  }

  countCamionsChargesByDepot(depotId: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/depot/${depotId}/camions-charges/count`);
  }

  getVoyageMarge(voyageId: number): Observable<VoyageMarge> {
    return this.http.get<VoyageMarge>(`${this.apiUrl}/${voyageId}/marge`);
  }

  assignerNumeroBonEnlevement(voyageId: number, numeroBonEnlevement: string): Observable<Voyage> {
    return this.http.put<Voyage>(`${this.apiUrl}/${voyageId}/bon-enlevement/assigner?numeroBonEnlevement=${encodeURIComponent(numeroBonEnlevement)}`, {});
  }

  genererNumeroBonEnlevement(voyageId: number): Observable<Voyage> {
    return this.http.put<Voyage>(`${this.apiUrl}/${voyageId}/bon-enlevement/generer`, {});
  }

  getCoutsTransport(fournisseurId: number, filterOption: string = 'tous', startDate?: string, endDate?: string): Observable<CoutTransportResponse> {
    let url = `${this.apiUrl}/couts-transport?fournisseurId=${fournisseurId}&filterOption=${filterOption}`;
    if (startDate) {
      url += `&startDate=${startDate}`;
    }
    if (endDate) {
      url += `&endDate=${endDate}`;
    }
    return this.http.get<CoutTransportResponse>(url);
  }

  updateStatut(voyageId: number, statut: string, params?: any): Observable<Voyage> {
    // Si params contient des clients ou manquants, utiliser le nouveau format
    if (params && (params.clients || params.manquants)) {
      const request = {
        statut: statut,
        clients: params.clients || [],
        manquants: params.manquants || {}
      };
      return this.http.put<Voyage>(`${this.apiUrl}/${voyageId}/statut`, request);
    }
    // Sinon, utiliser l'ancien format pour compatibilité
    let url = `${this.apiUrl}/${voyageId}/statut?statut=${statut}`;
    if (params?.clientId) {
      url += `&clientId=${params.clientId}`;
    }
    if (params?.manquant) {
      url += `&manquant=${params.manquant}`;
    }
    // Pour l'ancien format, ne pas envoyer de body (null)
    // Le controller détectera l'absence de body et utilisera les query parameters
    return this.http.put<Voyage>(url, null as any);
  }
}

export interface TransitaireStats {
  nombreCamionsDeclaresCeMois: number;
  totalFraisDouaneCeMois: number;
  totalMontantT1CeMois: number;
  totalFraisCeMois: number;
}

export interface CoutTransport {
  id?: number;
  numeroVoyage?: string;
  camionImmatriculation?: string;
  dateDepart?: string;
  destination?: string;
  quantite?: number;
  /** Prix unitaire transport (FCFA/litre) — modifiable par le comptable */
  prixUnitaire?: number;
  coutVoyage?: number;
  fraisTotaux?: number;
  coutTotal?: number;
  statutPaiement?: string;
  datePaiement?: string;
}

export interface CoutTransportStats {
  totalCout?: number;
  totalNonPaye?: number;
  totalPaye?: number;
  nombreVoyages?: number;
}

export interface CoutTransportResponse {
  couts: CoutTransport[];
  stats: CoutTransportStats;
}

export interface VoyageMarge {
  voyageId?: number;
  numeroVoyage?: string;
  quantite?: number;
  prixUnitaireAchat?: number;
  coutVoyage?: number;
  fraisTotaux?: number;
  coutReelTotal?: number;
  coutReelParLitre?: number;
  prixVenteUnitaire?: number;
  montantVenteTotal?: number;
  margeBrute?: number;
  margeNet?: number;
  margePourcentage?: number;
  hasFacture?: boolean;
}

