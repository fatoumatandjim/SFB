import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { VoyagesService, Voyage, EtatVoyage, ClientVoyage } from '../../services/voyages.service';
import { TransactionsService, Transaction } from '../../services/transactions.service';
import { TransitairesService, Transitaire } from '../../services/transitaires.service';
import { AxesService, Axe } from '../../services/axes.service';
import { ComptesBancairesService } from '../../services/comptes-bancaires.service';
import { CaissesService } from '../../services/caisses.service';
import { ClientsService, Client } from '../../services/clients.service';
import { FacturesService, Facture } from '../../services/factures.service';
import { AlertService } from '../../nativeComp/alert/alert.service';
import { ToastService } from '../../nativeComp/toast/toast.service';
import { PdfService } from '../../services/pdf.service';
import { ExcelService, CamionExcelData } from '../../services/excel.service';
import { AuthService } from 'src/app/services/auth.service';
import { EditPrixTransportModalComponent, VoyagePrixRef } from '../shared/edit-prix-transport-modal/edit-prix-transport-modal.component';

interface VoyageDisplay extends Voyage {
  camionImmatriculation?: string;
  clientNom?: string;
  clientEmail?: string;
  clientVoyages?: ClientVoyage[]; // Liste des clients associés au voyage
  transitaireNom?: string;
  transitaireIdentifiant?: string;
  transitairePhone?: string;
  typeProduit?: string;
  transactions?: Transaction[];
  etats?: EtatVoyage[];
}

@Component({
  selector: 'app-suivi-transport',
  templateUrl: './suivi-transport.component.html',
  styleUrls: ['./suivi-transport.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, EditPrixTransportModalComponent]
})
export class SuiviTransportComponent implements OnInit {
  activeTab: 'actifs' | 'receptionnes' | 'non-declares' | 'archives' | 'sans-prix-achat' | 'sans-prix-transport' | 'partiellement-decharges' | 'en-cours' = 'actifs';
  /** Valeur affichée par la liste déroulante (quand on est sur onglet Sortie de douane/En cours, on garde la dernière vue liste). */
  dropdownListView: 'actifs' | 'non-declares' | 'archives' | 'sans-prix-achat' | 'sans-prix-transport' | 'partiellement-decharges' = 'actifs';
  voyagesNonDeclares: VoyageDisplay[] = [];
  searchTerm: string = '';
  isLoading: boolean = false;
  voyages: VoyageDisplay[] = [];
  filteredVoyages: VoyageDisplay[] = [];
  filterType: 'tous' | 'date' | 'range' | 'axe' = 'tous';
  filterDate: string = '';
  filterStartDate: string = '';
  filterEndDate: string = '';
  filterAxeId: number | undefined = undefined;
  // Pagination pour filtrage par axe
  voyagesAxePage: { voyages: VoyageDisplay[]; currentPage: number; totalPages: number; totalElements: number; size: number } | null = null;
  currentPageAxe: number = 0;
  pageSizeAxe: number = 10;
  // Pagination pour voyages sans prix d'achat
  voyagesSansPrixAchatPage: { voyages: VoyageDisplay[]; currentPage: number; totalPages: number; totalElements: number; size: number } | null = null;
  currentPageSansPrixAchat: number = 0;
  pageSizeSansPrixAchat: number = 10;
  // Pagination pour voyages sans prix de transport (comptable)
  voyagesSansPrixTransportPage: { voyages: VoyageDisplay[]; currentPage: number; totalPages: number; totalElements: number; size: number } | null = null;
  currentPageSansPrixTransport: number = 0;
  pageSizeSansPrixTransport: number = 10;
  // Pagination pour voyages partiellement déchargés
  voyagesPartiellementDechargesPage: { voyages: VoyageDisplay[]; currentPage: number; totalPages: number; totalElements: number; size: number } | null = null;
  currentPagePartiellementDecharges: number = 0;
  pageSizePartiellementDecharges: number = 10;
  // Pagination pour voyages en cours
  voyagesEnCoursPage: { voyages: VoyageDisplay[]; currentPage: number; totalPages: number; totalElements: number; size: number } | null = null;
  currentPageEnCours: number = 0;
  pageSizeEnCours: number = 10;
  // Pagination pour archives
  voyagesArchivesPage: { voyages: VoyageDisplay[]; currentPage: number; totalPages: number; totalElements: number; size: number } | null = null;
  currentPageArchives: number = 0;
  pageSizeArchives: number = 10;
  // Filtre pour regrouper par client
  groupByClient: boolean = false;
  // Filtre archives: n'afficher que les voyages déclarés
  showOnlyDeclaredArchives: boolean = false;
  // Données groupées par client
  voyagesParClientPage: { clientsVoyages: Array<{ clientId: number; clientNom: string; clientEmail?: string; voyages: VoyageDisplay[]; nombreVoyages: number }>; currentPage: number; totalPages: number; totalElements: number; size: number } | null = null;
  showDetailModal: boolean = false;
  selectedVoyage: VoyageDisplay | null = null;
  activeDetailTab: 'details' | 'frais' = 'details';
  transitaires: Transitaire[] = [];
  axes: Axe[] = [];
  clients: Client[] = [];
  showAddTransitaireForm: boolean = false;
  showAddFraisForm: boolean = false;
  selectedTransitaireId: number | undefined;
  selectedClientId: number | undefined;
  isAssigningClient: boolean = false;
  newFrais: Partial<Transaction> = {
    type: 'FRAIS_FRONTIERE',
    montant: 0,
    date: new Date().toISOString().split('T')[0],
    statut: 'EN_ATTENTE',
    description: '',
    compteId: undefined,
    caisseId: undefined
  };
  compteType: 'banque' | 'caisse' = 'banque';
  comptesBancaires: any[] = [];
  caisses: any[] = [];
  showStatutModal: boolean = false;
  voyageForStatutChange: VoyageDisplay | null = null;
  selectedStatut: string = '';
  etatsVoyage: EtatVoyage[] = [];
  // Données pour le statut LIVRE - plusieurs clients avec quantités
  livreClients: Array<{ clientId: number | undefined; quantite: number | undefined }> = [];
  // Données pour le statut DECHARGER - manquant par client
  dechargerManquants: { [key: number]: number } = {}; // clientVoyageId -> manquant
  // État de livraison pour chaque ClientVoyage lors du DECHARGER
  dechargerClientLivres: { [key: number]: boolean } = {}; // clientVoyageId -> isLivrer
  // Mode modification pour chaque ClientVoyage
  dechargerModifierMode: { [key: number]: boolean } = {}; // clientVoyageId -> isModifierMode
  // Données pour le prix d'achat (séparé) - par ClientVoyage
  showPrixAchatModal: boolean = false;
  voyageForPrixAchat: VoyageDisplay | null = null;
  clientVoyageForPrixAchat: ClientVoyage | null = null;
  prixAchatValue: number | undefined;
  // Modal pour modifier le client et la quantité d'un ClientVoyage
  showEditClientVoyageModal: boolean = false;
  clientVoyageForEdit: ClientVoyage | null = null;
  quantiteClientVoyageValue: number | undefined;
  editClientId: number | undefined;
  showSelectModal: boolean = false;
  selectModalType: 'transitaire' | 'client' | null = null;
  searchSelectTerm: string = '';
  filteredTransitaires: Transitaire[] = [];
  filteredClients: Client[] = [];
  showClientsPopup: boolean = false;
  clientsForPopup: ClientVoyage[] = [];
  clientsPopupPosition: { x: number; y: number } = { x: 0, y: 0 };
  selectedVoyagesForBon: Set<number> = new Set();
  showBonGenerationModal: boolean = false;
  rapportPdfGenerating: boolean = false;
  isAdmin: boolean = false;
  // Modal pour saisir la quantité lors de l'ajout d'un client
  showQuantiteModal: boolean = false;
  selectedClientForAdd: Client | null = null;
  quantiteToAdd: number | undefined;

  constructor(
    private voyagesService: VoyagesService,
    private transitairesService: TransitairesService,
    private axesService: AxesService,
    private transactionsService: TransactionsService,
    private comptesBancairesService: ComptesBancairesService,
    private caissesService: CaissesService,
    private clientsService: ClientsService,
    private facturesService: FacturesService,
    private alertService: AlertService,
    private toastService: ToastService,
    private pdfService: PdfService,
    private excelService: ExcelService,
    private authService: AuthService
  ) { }

  private readonly COMPTABLE_VIEW_KEY = 'gesy_comptable_suivi_transport_view';

  ngOnInit() {
    this.isAdmin = this.authService.hasRole('ADMIN') || this.authService.hasRole('ROLE_ADMIN');
    if (this.authService.isComptable()) {
      const saved = localStorage.getItem(this.COMPTABLE_VIEW_KEY) as 'sans-prix-transport' | 'sans-prix-achat' | null;
      const view = saved && (saved === 'sans-prix-transport' || saved === 'sans-prix-achat') ? saved : 'sans-prix-transport';
      this.dropdownListView = view;
      this.activeTab = view;
      this.setTab(view);
    } else {
      this.loadVoyages();
    }
    this.loadTransitaires();
    this.loadAxes();
    this.loadComptesBancaires();
    this.loadCaisses();
    this.loadClients();
  }

  loadComptesBancaires() {
    this.comptesBancairesService.getAllComptes().subscribe({
      next: (data: any[]) => {
        // Filtrer uniquement les comptes actifs
        this.comptesBancaires = data.filter((c: any) => c.statut === 'ACTIF');
      },
      error: (error: any) => {
        console.error('Erreur lors du chargement des comptes bancaires:', error);
      }
    });
  }

  loadCaisses() {
    this.caissesService.getAllCaisses().subscribe({
      next: (data: any[]) => {
        // Filtrer uniquement les caisses actives
        this.caisses = data.filter((c: any) => c.statut === 'ACTIF');
      },
      error: (error: any) => {
        console.error('Erreur lors du chargement des caisses:', error);
      }
    });
  }

  loadClients() {
    this.clientsService.getAllClients().subscribe({
      next: (data) => {
        this.clients = data;
        this.filteredClients = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des clients:', error);
      }
    });
  }

  openSelectModal(type: 'transitaire' | 'client') {
    this.selectModalType = type;
    this.searchSelectTerm = '';
    if (type === 'transitaire') {
      this.filteredTransitaires = this.transitaires;
    } else {
      this.filteredClients = this.clients;
    }
    this.showSelectModal = true;
  }

  closeSelectModal() {
    this.showSelectModal = false;
    this.selectModalType = null;
    this.searchSelectTerm = '';
  }

  onSearchSelectChange() {
    if (this.selectModalType === 'transitaire') {
      if (!this.searchSelectTerm || this.searchSelectTerm.trim() === '') {
        this.filteredTransitaires = this.transitaires;
      } else {
        const term = this.searchSelectTerm.toLowerCase();
        this.filteredTransitaires = this.transitaires.filter(t =>
          t.nom?.toLowerCase().includes(term) ||
          t.identifiant?.toLowerCase().includes(term) ||
          t.telephone?.toLowerCase().includes(term)
        );
      }
    } else if (this.selectModalType === 'client') {
      if (!this.searchSelectTerm || this.searchSelectTerm.trim() === '') {
        this.filteredClients = this.clients;
      } else {
        const term = this.searchSelectTerm.toLowerCase();
        this.filteredClients = this.clients.filter(c =>
          c.nom?.toLowerCase().includes(term) ||
          c.email?.toLowerCase().includes(term)
        );
      }
    }
  }

  selectTransitaire(transitaire: Transitaire) {
    this.selectedTransitaireId = transitaire.id;
    this.closeSelectModal();
    this.assignTransitaire();
  }

  selectClient(client: Client) {
    // Si on est dans le modal de statut (LIVRE), ajouter à livreClients
    if (this.showStatutModal && this.selectedStatut === 'LIVRE') {
      this.selectClientForLivre(client);
      return;
    }

    // Si on est dans les détails et qu'on veut ajouter un client, ouvrir le modal de quantité
    if (this.showDetailModal && this.selectedVoyage) {
      this.selectedClientForAdd = client;
      this.quantiteToAdd = undefined;
      this.closeSelectModal();
      this.showQuantiteModal = true;
      return;
    }

    // Sinon, comportement par défaut (assigner un client au voyage)
    this.selectedClientId = client.id;
    this.closeSelectModal();
    this.assignClient();
  }

  closeQuantiteModal() {
    this.showQuantiteModal = false;
    this.selectedClientForAdd = null;
    this.quantiteToAdd = undefined;
  }

  generateRapportCamionsPdf() {
    this.rapportPdfGenerating = true;
    this.voyagesService.getVoyagesEnCoursAvecClients().subscribe({
      next: (data) => {
        this.rapportPdfGenerating = false;
        this.pdfService.exportRapportCamionsClients(data);
        this.toastService.success(`Rapport PDF généré (${data.length} voyage(s) en cours)`);
      },
      error: () => {
        this.rapportPdfGenerating = false;
        this.toastService.error('Erreur lors du chargement des voyages en cours');
      }
    });
  }

  confirmAddClientToVoyage() {
    if (!this.selectedVoyage || !this.selectedVoyage.id || !this.selectedClientForAdd) {
      this.toastService.error('Voyage ou client invalide');
      return;
    }

    if (!this.quantiteToAdd || this.quantiteToAdd <= 0) {
      this.toastService.error('Veuillez saisir une quantité valide');
      return;
    }

    // Calculer la quantité disponible
    const quantiteTotale = (this.selectedVoyage.clientVoyages || []).reduce((sum, cv) => sum + (cv.quantite || 0), 0);
    const quantiteDisponible = (this.selectedVoyage.quantite || 0) - quantiteTotale;

    // Vérifier que la quantité demandée ne dépasse pas la quantité disponible
    if (this.quantiteToAdd > quantiteDisponible) {
      this.toastService.error(`La quantité demandée (${this.quantiteToAdd} L) dépasse la quantité disponible (${quantiteDisponible} L)`);
      return;
    }

    // Demander confirmation
    const message = `Confirmer l'ajout du client ${this.selectedClientForAdd.nom} avec une quantité de ${this.quantiteToAdd} L ?`;
    this.alertService.confirm(message, 'Confirmation').subscribe(confirmed => {
      if (!confirmed) return;

      this.addClientToVoyage(this.selectedClientForAdd!, this.quantiteToAdd!);
    });
  }

  addClientToVoyage(client: Client, quantite: number) {
    if (!this.selectedVoyage || !this.selectedVoyage.id) {
      this.toastService.error('Voyage invalide');
      return;
    }

    // Calculer la quantité totale
    const quantiteTotale = (this.selectedVoyage.clientVoyages || []).reduce((sum, cv) => sum + (cv.quantite || 0), 0);

    // Vérifier que la quantité totale ne dépasse pas la quantité du voyage
    if (quantiteTotale + quantite > (this.selectedVoyage.quantite || 0)) {
      this.toastService.error(`La quantité totale (${quantiteTotale + quantite} L) dépasse la quantité du voyage (${this.selectedVoyage.quantite} L)`);
      return;
    }

    // Mettre à jour le statut du voyage à LIVRE avec ce nouveau client
    const params: any = {
      clients: [
        ...(this.selectedVoyage.clientVoyages || []).map(cv => ({
          clientId: cv.clientId,
          quantite: cv.quantite
        })),
        {
          clientId: client.id,
          quantite: quantite
        }
      ]
    };

    this.isLoading = true;
    this.voyagesService.updateStatut(this.selectedVoyage.id, 'LIVRE', params).subscribe({
      next: (updatedVoyage) => {
        this.isLoading = false;
        this.toastService.success(`Client ${client.nom} ajouté avec succès (${quantite} L)`);
        this.closeQuantiteModal();
        // Recharger le voyage pour avoir les données à jour
        this.voyagesService.getVoyageById(this.selectedVoyage!.id!).subscribe({
          next: (reloadedVoyage) => {
            this.selectedVoyage = { ...this.selectedVoyage!, ...reloadedVoyage };
          },
          error: (error) => {
            console.error('Erreur lors du rechargement:', error);
          }
        });
      },
      error: (error) => {
        this.isLoading = false;
        const errorMessage = this.getErrorMessage(error, 'Erreur lors de l\'ajout du client');
        this.toastService.error(errorMessage);
      }
    });
  }

  openSelectClientModal() {
    this.selectModalType = 'client';
    this.searchSelectTerm = '';
    this.filteredClients = this.clients;
    this.showSelectModal = true;
  }

  selectClientForLivre(client: Client) {
    // Vérifier si le client n'est pas déjà dans la liste
    if (!this.livreClients.find(c => c.clientId === client.id)) {
      this.livreClients.push({ clientId: client.id, quantite: undefined });
    }
    this.closeSelectModal();
  }

  addClientForLivre() {
    this.selectModalType = 'client';
    this.searchSelectTerm = '';
    this.filteredClients = this.clients;
    this.showSelectModal = true;
  }

  removeClientForLivre(index: number) {
    this.livreClients.splice(index, 1);
  }

  getTotalQuantiteClients(): number {
    return this.livreClients.reduce((total, c) => total + (c.quantite || 0), 0);
  }

  /**
   * Quantité déjà attribuée à partir des clients existants du voyage (avant ajout).
   */
  getTotalQuantiteClientsExistants(): number {
    if (!this.voyageForStatutChange || !this.voyageForStatutChange.clientVoyages) {
      return 0;
    }
    return this.voyageForStatutChange.clientVoyages
      .reduce((total, cv) => total + (cv.quantite || 0), 0);
  }

  /**
   * Quantité totale attribuée (clients existants + nouveaux clients dans le formulaire LIVRE).
   */
  getTotalQuantiteAttribueeLIVRE(): number {
    return this.getTotalQuantiteClientsExistants() + this.getTotalQuantiteClients();
  }

  /**
   * Quantité restante disponible pour l'attribution (LIVRE).
   */
  getResteQuantiteLIVRE(): number {
    const totalVoyage = this.voyageForStatutChange?.quantite || 0;
    return totalVoyage - this.getTotalQuantiteAttribueeLIVRE();
  }

  getClientName(clientId: number | undefined): string {
    if (!clientId) return '';
    const client = this.clients.find(c => c.id === clientId);
    return client ? client.nom : '';
  }

  getFactureStatutClass(statut: string | undefined): string {
    if (!statut) return '';
    const classes: { [key: string]: string } = {
      'BROUILLON': 'statut-brouillon',
      'EMISE': 'statut-emise',
      'PAYEE': 'statut-payee',
      'PARTIELLEMENT_PAYEE': 'statut-partiellement-payee',
      'ANNULEE': 'statut-annulee',
      'EN_RETARD': 'statut-en-retard'
    };
    return classes[statut] || '';
  }

  getFactureStatutLabel(statut: string | undefined): string {
    if (!statut) return '';
    const labels: { [key: string]: string } = {
      'BROUILLON': 'Brouillon',
      'EMISE': 'Emise',
      'PAYEE': 'Payée',
      'PARTIELLEMENT_PAYEE': 'Partiellement payée',
      'ANNULEE': 'Annulée',
      'EN_RETARD': 'En retard'
    };
    return labels[statut] || statut;
  }

  addClientInDetail() {
    if (!this.selectedVoyage) {
      return;
    }
    // Ouvrir le modal de sélection de client
    this.selectModalType = 'client';
    this.searchSelectTerm = '';
    this.filteredClients = this.clients;
    this.showSelectModal = true;
  }

  marquerFacturePayee(facture: Facture) {
    if (!facture.id) {
      this.toastService.error('Facture invalide');
      return;
    }

    this.alertService.confirm(
      `Voulez-vous marquer la facture #${facture.numero} comme payée ?`,
      'Confirmation'
    ).subscribe(confirmed => {
      if (!confirmed) {
        return;
      }

      // Mettre à jour la facture
      const factureUpdate: Facture = {
        ...facture,
        statut: 'PAYEE',
        montantPaye: facture.montant
      };

      this.facturesService.updateFacture(facture.id!, factureUpdate).subscribe({
        next: (updatedFacture) => {
          this.toastService.success('Facture marquée comme payée');
          // Mettre à jour la facture dans selectedVoyage
          if (this.selectedVoyage && this.selectedVoyage.factures) {
            const index = this.selectedVoyage.factures.findIndex(f => f.id === facture.id);
            if (index !== -1) {
              this.selectedVoyage.factures[index] = updatedFacture;
            }
          }
          // Recharger le voyage pour avoir les données à jour
          if (this.selectedVoyage?.id) {
            this.voyagesService.getVoyageById(this.selectedVoyage.id).subscribe({
              next: (updatedVoyage) => {
                this.selectedVoyage = { ...this.selectedVoyage!, ...updatedVoyage };
              },
              error: (error) => {
                console.error('Erreur lors du rechargement du voyage:', error);
              }
            });
          }
        },
        error: (error) => {
          const errorMessage = this.getErrorMessage(error, 'Erreur lors de la mise à jour de la facture');
          this.toastService.error(errorMessage);
        }
      });
    });
  }

  loadTransitaires() {
    this.transitairesService.getAllTransitaires().subscribe({
      next: (data) => {
        this.transitaires = data;
        this.filteredTransitaires = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des transitaires:', error);
      }
    });
  }

  loadAxes() {
    this.axesService.getAllAxes().subscribe({
      next: (data) => {
        this.axes = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des axes:', error);
      }
    });
  }

  loadVoyages() {
    this.isLoading = true;
    this.voyagesService.getAllVoyages().subscribe({
      next: (data) => {
        let list = data.map(v => ({
          ...v,
          camionImmatriculation: (v as any).camionImmatriculation,
          clientNom: (v as any).clientNom,
          clientEmail: (v as any).clientEmail,
          transitaireNom: (v as any).transitaireNom,
          depotNom: (v as any).depotNom,
          typeProduit: (v as any).typeProduit || 'Essence',
          transactions: (v as any).transactions || [],
          etats: (v as any).etats || [],
          responsableIdentifiant: (v as any).responsableIdentifiant
        }));
        // Logisticien : ne voir que les voyages dont il est le responsable
        if (this.isLogisticienOnly()) {
          const identifiant = this.authService.getIdentifiant();
          list = list.filter(v => (v as any).responsableIdentifiant === identifiant);
        }
        this.voyages = this.sortByDateDepartAsc(list);
        this.updateFilteredVoyages();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des voyages:', error);
        this.isLoading = false;
      }
    });
  }

  onListViewChangeByValue(value: string) {
    const tab = value as 'actifs' | 'non-declares' | 'archives' | 'sans-prix-achat' | 'sans-prix-transport' | 'partiellement-decharges';
    this.dropdownListView = tab;
    this.setTab(tab);
    if (this.authService.isComptable() && (tab === 'sans-prix-transport' || tab === 'sans-prix-achat')) {
      localStorage.setItem(this.COMPTABLE_VIEW_KEY, tab);
    }
  }

  setTab(tab: 'actifs' | 'receptionnes' | 'non-declares' | 'archives' | 'sans-prix-achat' | 'sans-prix-transport' | 'partiellement-decharges' | 'en-cours') {
    this.activeTab = tab;
    if (tab !== 'receptionnes' && tab !== 'en-cours') {
      this.dropdownListView = tab as 'actifs' | 'non-declares' | 'archives' | 'sans-prix-achat' | 'sans-prix-transport' | 'partiellement-decharges';
    }
    // Réinitialiser filteredVoyages pour éviter les conflits entre onglets
    this.filteredVoyages = [];

    if (tab === 'non-declares') {
      this.voyagesNonDeclares = [];
      this.loadVoyagesPassesNonDeclares();
    } else if (tab === 'sans-prix-achat') {
      this.currentPageSansPrixAchat = 0;
      this.voyagesSansPrixAchatPage = null;
      this.voyagesParClientPage = null;
      this.loadVoyagesSansPrixAchat();
    } else if (tab === 'sans-prix-transport') {
      this.currentPageSansPrixTransport = 0;
      this.voyagesSansPrixTransportPage = null;
      this.loadVoyagesSansPrixTransport();
    } else if (tab === 'partiellement-decharges') {
      this.currentPagePartiellementDecharges = 0;
      this.voyagesPartiellementDechargesPage = null;
      this.loadVoyagesPartiellementDecharges();
    } else if (tab === 'en-cours') {
      this.currentPageEnCours = 0;
      this.voyagesEnCoursPage = null;
      this.loadVoyagesEnCours();
    } else if (tab === 'archives') {
      this.currentPageArchives = 0;
      this.voyagesArchivesPage = null;
      this.loadVoyagesArchives();
    } else if (this.filterType === 'axe' && this.filterAxeId) {
      this.currentPageAxe = 0;
      this.voyagesAxePage = null;
      this.loadVoyagesByAxe();
    } else {
      // Pour actifs et réceptionnes, charger les voyages et filtrer
      this.loadVoyages();
    }
  }

  loadVoyagesPassesNonDeclares() {
    this.isLoading = true;
    this.voyagesService.getVoyagesPassesNonDeclares().subscribe({
      next: (data) => {
        this.voyagesNonDeclares = this.sortByDateDepartAsc(data.map(v => ({
          ...v,
          camionImmatriculation: (v as any).camionImmatriculation,
          clientNom: (v as any).clientNom,
          clientEmail: (v as any).clientEmail,
          transitaireNom: (v as any).transitaireNom,
          depotNom: (v as any).depotNom,
          typeProduit: (v as any).typeProduit || 'Essence',
          transactions: (v as any).transactions || [],
          etats: (v as any).etats || []
        })));
        // Réinitialiser filteredVoyages pour cet onglet
        this.filteredVoyages = [];
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des voyages passés non déclarés:', error);
        this.isLoading = false;
      }
    });
  }

  loadVoyagesSansPrixAchat() {
    this.isLoading = true;
    // Réinitialiser filteredVoyages pour cet onglet
    this.filteredVoyages = [];

    if (this.groupByClient) {
      // Charger les voyages groupés par client
      this.voyagesService.getVoyagesAvecClientSansFactureGroupesParClient(this.currentPageSansPrixAchat, this.pageSizeSansPrixAchat).subscribe({
        next: (data) => {
          const clientsVoyages = data.clientsVoyages.map(cv => ({
            clientId: cv.clientId,
            clientNom: cv.clientNom,
            clientEmail: cv.clientEmail,
            nombreVoyages: cv.nombreVoyages,
            voyages: cv.voyages.map(v => ({
              ...v,
              camionImmatriculation: (v as any).camionImmatriculation,
              clientNom: (v as any).clientNom || cv.clientNom,
              clientEmail: (v as any).clientEmail || cv.clientEmail,
              transitaireNom: (v as any).transitaireNom,
              depotNom: (v as any).depotNom,
              typeProduit: (v as any).typeProduit || 'Essence',
              transactions: (v as any).transactions || [],
              etats: (v as any).etats || []
            }))
          }));

          this.voyagesParClientPage = {
            clientsVoyages: clientsVoyages,
            currentPage: data.currentPage,
            totalPages: data.totalPages,
            totalElements: data.totalElements,
            size: data.size
          };

          // Aplatir les voyages pour filteredVoyages (pour la recherche)
          this.filteredVoyages = clientsVoyages.reduce((acc: VoyageDisplay[], cv: { clientId: number; clientNom: string; clientEmail?: string; voyages: VoyageDisplay[]; nombreVoyages: number }) => {
            return acc.concat(cv.voyages);
          }, []);
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Erreur lors du chargement des voyages sans prix d\'achat groupés:', error);
          this.isLoading = false;
        }
      });
    } else {
      // Charger les voyages normalement
      this.voyagesService.getVoyagesAvecClientSansFacture(this.currentPageSansPrixAchat, this.pageSizeSansPrixAchat).subscribe({
        next: (data) => {
          const voyages = this.sortByDateDepartAsc(data.voyages.map(v => ({
            ...v,
            camionImmatriculation: (v as any).camionImmatriculation,
            clientNom: (v as any).clientNom,
            clientEmail: (v as any).clientEmail,
            transitaireNom: (v as any).transitaireNom,
            depotNom: (v as any).depotNom,
            typeProduit: (v as any).typeProduit || 'Essence',
            transactions: (v as any).transactions || [],
            etats: (v as any).etats || []
          })));
          this.voyagesSansPrixAchatPage = {
            voyages: voyages,
            currentPage: data.currentPage,
            totalPages: data.totalPages,
            totalElements: data.totalElements,
            size: data.size
          };
          this.filteredVoyages = voyages;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Erreur lors du chargement des voyages sans prix d\'achat:', error);
          this.isLoading = false;
        }
      });
    }
  }

  toggleGroupByClient() {
    this.groupByClient = !this.groupByClient;
    this.currentPageSansPrixAchat = 0;
    this.loadVoyagesSansPrixAchat();
  }

  loadVoyagesSansPrixTransport() {
    this.isLoading = true;
    this.filteredVoyages = [];
    this.voyagesService.getVoyagesSansPrixTransport(this.currentPageSansPrixTransport, this.pageSizeSansPrixTransport).subscribe({
      next: (data) => {
        const voyages = this.sortByDateDepartAsc(data.voyages.map(v => ({
          ...v,
          camionImmatriculation: (v as any).camionImmatriculation,
          clientNom: (v as any).clientNom,
          clientEmail: (v as any).clientEmail,
          transitaireNom: (v as any).transitaireNom,
          depotNom: (v as any).depotNom,
          typeProduit: (v as any).typeProduit || 'Essence',
          transactions: (v as any).transactions || [],
          etats: (v as any).etats || [],
          responsableIdentifiant: (v as any).responsableIdentifiant
        })));
        this.voyagesSansPrixTransportPage = {
          voyages,
          currentPage: data.currentPage,
          totalPages: data.totalPages,
          totalElements: data.totalElements,
          size: data.size
        };
        this.filteredVoyages = voyages;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement des voyages sans prix transport:', err);
        this.isLoading = false;
      }
    });
  }

  changePageSansPrixTransport(page: number) {
    if (page >= 0 && this.voyagesSansPrixTransportPage && page < this.voyagesSansPrixTransportPage.totalPages) {
      this.currentPageSansPrixTransport = page;
      this.loadVoyagesSansPrixTransport();
    }
  }

  // Voyages archives filtrés selon showOnlyDeclaredArchives
  get voyagesArchivesFiltered(): VoyageDisplay[] {
    if (!this.voyagesArchivesPage || !this.voyagesArchivesPage.voyages) {
      return [];
    }
    if (!this.showOnlyDeclaredArchives) {
      return this.voyagesArchivesPage.voyages as VoyageDisplay[];
    }
    return (this.voyagesArchivesPage.voyages as VoyageDisplay[]).filter(v => v.declarer);
  }

  changePageSansPrixAchat(page: number) {
    if (page >= 0 && this.voyagesSansPrixAchatPage && page < this.voyagesSansPrixAchatPage.totalPages) {
      this.currentPageSansPrixAchat = page;
      this.loadVoyagesSansPrixAchat();
    }
  }

  loadVoyagesPartiellementDecharges() {
    this.isLoading = true;
    // Réinitialiser filteredVoyages pour cet onglet
    this.filteredVoyages = [];
    this.voyagesService.getVoyagesPartiellementDecharges(this.currentPagePartiellementDecharges, this.pageSizePartiellementDecharges).subscribe({
      next: (data) => {
        this.voyagesPartiellementDechargesPage = {
          voyages: this.sortByDateDepartAsc(data.voyages || []),
          currentPage: data.currentPage || 0,
          totalPages: data.totalPages || 0,
          totalElements: data.totalElements || 0,
          size: data.size || 10
        };
        this.isLoading = false;
      },
      error: (error) => {
        this.isLoading = false;
        const errorMessage = this.getErrorMessage(error, 'Erreur lors du chargement des voyages partiellement déchargés');
        this.toastService.error(errorMessage);
      }
    });
  }

  changePagePartiellementDecharges(page: number) {
    if (page >= 0 && this.voyagesPartiellementDechargesPage && page < this.voyagesPartiellementDechargesPage.totalPages) {
      this.currentPagePartiellementDecharges = page;
      this.loadVoyagesPartiellementDecharges();
    }
  }

  loadVoyagesEnCours() {
    this.isLoading = true;
    this.voyagesService.getVoyagesEnCours(this.currentPageEnCours, this.pageSizeEnCours).subscribe({
      next: (data) => {
        let voyages = data.voyages || [];
        if (this.isLogisticienOnly()) {
          const identifiant = this.authService.getIdentifiant();
          voyages = voyages.filter((v: any) => v.responsableIdentifiant === identifiant);
        }
        this.voyagesEnCoursPage = {
          voyages: this.sortByDateDepartAsc(voyages),
          currentPage: data.currentPage || 0,
          totalPages: data.totalPages || 0,
          totalElements: data.totalElements || 0,
          size: data.size || 10
        };
        this.filteredVoyages = [];
        this.isLoading = false;
      },
      error: (error) => {
        this.isLoading = false;
        const errorMessage = this.getErrorMessage(error, 'Erreur lors du chargement des voyages en cours');
        this.toastService.error(errorMessage);
      }
    });
  }

  changePageEnCours(page: number) {
    if (page >= 0 && this.voyagesEnCoursPage && page < this.voyagesEnCoursPage.totalPages) {
      this.currentPageEnCours = page;
      this.loadVoyagesEnCours();
    }
  }

  loadVoyagesArchives() {
    this.isLoading = true;
    // Réinitialiser filteredVoyages pour cet onglet
    this.filteredVoyages = [];
    this.voyagesService.getArchivedVoyages(this.currentPageArchives, this.pageSizeArchives).subscribe({
      next: (data) => {
        let voyages = data.voyages || [];
        if (this.isLogisticienOnly()) {
          const identifiant = this.authService.getIdentifiant();
          voyages = voyages.filter((v: any) => v.responsableIdentifiant === identifiant);
        }
        this.voyagesArchivesPage = {
          voyages: this.sortByDateDepartAsc(voyages),
          currentPage: data.currentPage || 0,
          totalPages: data.totalPages || 0,
          totalElements: data.totalElements || 0,
          size: data.size || 10
        };
        this.isLoading = false;
      },
      error: (error) => {
        this.isLoading = false;
        const errorMessage = this.getErrorMessage(error, 'Erreur lors du chargement des archives');
        this.toastService.error(errorMessage);
      }
    });
  }

  changePageArchives(page: number) {
    if (page >= 0 && this.voyagesArchivesPage && page < this.voyagesArchivesPage.totalPages) {
      this.currentPageArchives = page;
      this.loadVoyagesArchives();
    }
  }

  updateFilteredVoyages() {
    let filtered = this.voyages;

    // Filtrer par onglet (actifs vs sortie de douane)
    if (this.activeTab === 'actifs') {
      filtered = filtered.filter(v =>
        v.statut !== 'LIVRE' &&
        v.statut !== 'RECEPTIONNER' &&
        v.statut !== 'DECHARGER' &&
        v.passager !== 'passer_non_declarer'
      );
    } else if (this.activeTab === 'receptionnes') {
      filtered = filtered.filter(v =>
        v.statut === 'RECEPTIONNER'
      );
    }

    // Filtrer par date
    if (this.filterType === 'date' && this.filterDate) {
      filtered = filtered.filter(v => {
        if (!v.dateDepart) return false;
        const voyageDate = new Date(v.dateDepart);
        const filterDate = new Date(this.filterDate);
        return voyageDate.toDateString() === filterDate.toDateString();
      });
    } else if (this.filterType === 'range' && this.filterStartDate && this.filterEndDate) {
      filtered = filtered.filter(v => {
        if (!v.dateDepart) return false;
        const voyageDate = new Date(v.dateDepart);
        const startDate = new Date(this.filterStartDate);
        const endDate = new Date(this.filterEndDate);
        // Définir l'heure à minuit pour la comparaison
        startDate.setHours(0, 0, 0, 0);
        endDate.setHours(23, 59, 59, 999);
        voyageDate.setHours(0, 0, 0, 0);
        return voyageDate >= startDate && voyageDate <= endDate;
      });
    } else if (this.filterType === 'axe' && this.filterAxeId) {
      filtered = filtered.filter(v => v.axeId === this.filterAxeId);
    }

    // Filtrer par recherche
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(v =>
        (v.numeroVoyage && v.numeroVoyage.toLowerCase().includes(term)) ||
        (v.clientNom && v.clientNom.toLowerCase().includes(term)) ||
        (v.destination && v.destination.toLowerCase().includes(term)) ||
        (v.lieuDepart && v.lieuDepart.toLowerCase().includes(term)) ||
        (v.camionImmatriculation && v.camionImmatriculation.toLowerCase().includes(term))
      );
    }

    this.filteredVoyages = filtered;
  }

  onSearchChange() {
    if (this.filterType === 'axe' && this.filterAxeId) {
      // Pour le filtrage par axe, on recharge avec pagination
      // Le filtrage par recherche se fait côté client dans loadVoyagesByAxe
      this.currentPageAxe = 0;
      this.loadVoyagesByAxe();
    } else {
      this.updateFilteredVoyages();
    }
  }

  onFilterChange() {
    // Réinitialiser les filtres selon le type sélectionné
    if (this.filterType !== 'date') {
      this.filterDate = '';
    }
    if (this.filterType !== 'range') {
      this.filterStartDate = '';
      this.filterEndDate = '';
    }
    if (this.filterType !== 'axe') {
      this.filterAxeId = undefined;
      this.voyagesAxePage = null;
      this.currentPageAxe = 0;
    } else {
      // Réinitialiser la page si on change de filtre axe
      this.currentPageAxe = 0;
    }

    if (this.filterType === 'axe' && this.filterAxeId) {
      this.loadVoyagesByAxe();
    } else {
      this.updateFilteredVoyages();
    }
  }

  loadVoyagesByAxe() {
    if (!this.filterAxeId) {
      this.filteredVoyages = [];
      this.voyagesAxePage = null;
      return;
    }

    this.isLoading = true;
    this.voyagesService.getVoyagesByAxePaginated(this.filterAxeId, this.currentPageAxe, this.pageSizeAxe).subscribe({
      next: (page) => {
        let voyages = page.voyages.map(v => ({
          ...v,
          camionImmatriculation: (v as any).camionImmatriculation,
          clientNom: (v as any).clientNom,
          clientEmail: (v as any).clientEmail,
          transitaireNom: (v as any).transitaireNom,
          depotNom: (v as any).depotNom,
          typeProduit: (v as any).typeProduit || 'Essence',
          transactions: (v as any).transactions || [],
          etats: (v as any).etats || [],
          responsableIdentifiant: (v as any).responsableIdentifiant
        }));
        if (this.isLogisticienOnly()) {
          const identifiant = this.authService.getIdentifiant();
          voyages = voyages.filter((v: any) => v.responsableIdentifiant === identifiant);
        }

        // Filtrer par onglet
        let filtered = voyages;
        if (this.activeTab === 'actifs') {
          filtered = filtered.filter(v =>
            v.statut !== 'LIVRE' &&
            v.statut !== 'RECEPTIONNER' &&
            v.statut !== 'DECHARGER' &&
            v.passager !== 'passer_non_declarer'
          );
        } else if (this.activeTab === 'receptionnes') {
          filtered = filtered.filter(v => v.statut === 'RECEPTIONNER');
        } else if (this.activeTab === 'archives') {
          filtered = filtered.filter(v => v.statut === 'DECHARGER');
        }

        // Filtrer par recherche
        if (this.searchTerm) {
          const term = this.searchTerm.toLowerCase();
          filtered = filtered.filter(v =>
            (v.numeroVoyage && v.numeroVoyage.toLowerCase().includes(term)) ||
            (v.clientNom && v.clientNom.toLowerCase().includes(term)) ||
            (v.destination && v.destination.toLowerCase().includes(term)) ||
            (v.lieuDepart && v.lieuDepart.toLowerCase().includes(term)) ||
            (v.camionImmatriculation && v.camionImmatriculation.toLowerCase().includes(term))
          );
        }

        this.filteredVoyages = this.sortByDateDepartAsc(filtered);
        this.voyagesAxePage = {
          voyages: this.filteredVoyages,
          currentPage: page.currentPage,
          totalPages: page.totalPages,
          totalElements: page.totalElements,
          size: page.size
        };
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des voyages par axe:', error);
        this.filteredVoyages = [];
        this.voyagesAxePage = null;
        this.isLoading = false;
      }
    });
  }

  changePageAxe(page: number) {
    if (this.filterType === 'axe' && this.filterAxeId && this.voyagesAxePage) {
      if (page >= 0 && page < this.voyagesAxePage.totalPages) {
        this.currentPageAxe = page;
        this.loadVoyagesByAxe();
      }
    }
  }

  getPageNumbers(currentPage: number, totalPages: number): (number | string)[] {
    const pages: (number | string)[] = [];
    const maxVisible = 5;

    if (totalPages <= maxVisible) {
      // Afficher toutes les pages si le nombre total est petit
      for (let i = 0; i < totalPages; i++) {
        pages.push(i);
      }
    } else {
      // Logique pour afficher les pages avec des ellipses
      if (currentPage < 3) {
        // Début : afficher les premières pages
        for (let i = 0; i < 4; i++) {
          pages.push(i);
        }
        pages.push('...');
        pages.push(totalPages - 1);
      } else if (currentPage > totalPages - 4) {
        // Fin : afficher les dernières pages
        pages.push(0);
        pages.push('...');
        for (let i = totalPages - 4; i < totalPages; i++) {
          pages.push(i);
        }
      } else {
        // Milieu : afficher autour de la page actuelle
        pages.push(0);
        pages.push('...');
        for (let i = currentPage - 1; i <= currentPage + 1; i++) {
          pages.push(i);
        }
        pages.push('...');
        pages.push(totalPages - 1);
      }
    }

    return pages;
  }

  openStatutModal(voyage: VoyageDisplay) {
    this.voyageForStatutChange = voyage;
    this.selectedStatut = '';
    // Initialiser livreClients et dechargerManquants
    this.livreClients = [];
    this.dechargerManquants = {};
    this.dechargerClientLivres = {};
    this.dechargerModifierMode = {};

    // Si le voyage a déjà des ClientVoyage, initialiser dechargerManquants et dechargerClientLivres
    if (voyage.clientVoyages && voyage.clientVoyages.length > 0) {
      voyage.clientVoyages.forEach(cv => {
        if (cv.id) {
          // Garder la valeur existante de manquant, même si elle vaut 0
          if (cv.manquant !== undefined && cv.manquant !== null) {
            this.dechargerManquants[cv.id] = cv.manquant;
          }
          // Initialiser l'état de livraison basé sur le statut existant
          this.dechargerClientLivres[cv.id] = cv.statut === 'LIVRER';
        }
      });
    }

    // Charger les états du voyage
    if (voyage.id) {
      this.voyagesService.getVoyageById(voyage.id).subscribe({
        next: (updatedVoyage) => {
          // Mettre à jour les ClientVoyage depuis la réponse
          if (this.voyageForStatutChange && updatedVoyage.clientVoyages) {
            this.voyageForStatutChange.clientVoyages = updatedVoyage.clientVoyages;
            // Réinitialiser dechargerManquants et dechargerClientLivres avec les nouvelles données
            this.dechargerManquants = {};
            this.dechargerClientLivres = {};
            updatedVoyage.clientVoyages.forEach(cv => {
              if (cv.id) {
                // Garder la valeur existante de manquant, même si elle vaut 0
                if (cv.manquant !== undefined && cv.manquant !== null) {
                  this.dechargerManquants[cv.id] = cv.manquant;
                }
                // Initialiser l'état de livraison basé sur le statut existant
                this.dechargerClientLivres[cv.id] = cv.statut === 'LIVRER';
              }
            });
          }

          this.etatsVoyage = updatedVoyage.etats || [];
          // Si pas d'états, créer la liste des 5 états par défaut
          if (this.etatsVoyage.length === 0) {
            this.etatsVoyage = this.getDefaultEtats();
          } else {
            // S'assurer que tous les états par défaut sont présents
            const defaultEtats = this.getDefaultEtats();
            const existingEtats = new Map(this.etatsVoyage.map(e => [e.etat, e]));
            this.etatsVoyage = defaultEtats.map(defaultEtat => {
              const existing = existingEtats.get(defaultEtat.etat);
              return existing || defaultEtat;
            });
          }
          this.showStatutModal = true;
        },
        error: (error) => {
          console.error('Erreur lors du chargement des états:', error);
          // Utiliser les états par défaut en cas d'erreur
          this.etatsVoyage = this.getDefaultEtats();
          this.showStatutModal = true;
        }
      });
    } else {
      this.etatsVoyage = this.getDefaultEtats();
      this.showStatutModal = true;
    }
  }

  closeStatutModal() {
    this.showStatutModal = false;
    this.voyageForStatutChange = null;
    this.selectedStatut = '';
    this.etatsVoyage = [];
    this.livreClients = [];
    this.dechargerManquants = {};
    this.dechargerClientLivres = {};
    this.dechargerModifierMode = {};
  }

  openPrixAchatModal(voyage: VoyageDisplay, clientVoyage?: ClientVoyage) {
    // Vérifier que le voyage a des clients
    if (!voyage.clientVoyages || voyage.clientVoyages.length === 0) {
      this.toastService.warning('Le voyage doit avoir au moins un client assigné avant de pouvoir donner le prix d\'achat');
      return;
    }

    // Si un ClientVoyage spécifique est fourni, l'utiliser
    if (clientVoyage) {
      this.clientVoyageForPrixAchat = clientVoyage;
      // Initialiser avec le prix existant s'il existe, sinon undefined
      this.prixAchatValue = clientVoyage.prixAchat ? Number(clientVoyage.prixAchat) : undefined;
    } else {
      // Sinon, prendre le premier ClientVoyage (même s'il a déjà un prix)
      this.clientVoyageForPrixAchat = voyage.clientVoyages[0];
      this.prixAchatValue = this.clientVoyageForPrixAchat.prixAchat ? Number(this.clientVoyageForPrixAchat.prixAchat) : undefined;
    }

    // Vérifier que le voyage est déchargé (pour les voyages actifs) ou qu'on est dans l'onglet sans-prix-achat
    if (this.activeTab !== 'sans-prix-achat' && voyage.statut !== 'DECHARGER' && voyage.statut !== 'PARTIELLEMENT_DECHARGER') {
      this.toastService.warning('Le voyage doit être déchargé avant de pouvoir donner le prix d\'achat');
      return;
    }
    this.voyageForPrixAchat = voyage;
    this.showPrixAchatModal = true;
  }

  closePrixAchatModal() {
    this.showPrixAchatModal = false;
    this.voyageForPrixAchat = null;
    this.prixAchatValue = undefined;
  }

  donnerPrixAchat() {
    if (!this.voyageForPrixAchat || !this.voyageForPrixAchat.id) {
      this.toastService.error('Voyage invalide');
      return;
    }
    if (!this.clientVoyageForPrixAchat || !this.clientVoyageForPrixAchat.id) {
      this.toastService.error('Client invalide');
      return;
    }
    if (!this.prixAchatValue || this.prixAchatValue <= 0) {
      this.toastService.warning('Veuillez saisir un prix d\'achat valide');
      return;
    }

    this.isLoading = true;
    this.voyagesService.donnerPrixAchat(this.voyageForPrixAchat.id, this.clientVoyageForPrixAchat.id, this.prixAchatValue).subscribe({
      next: (updatedVoyage) => {
        this.isLoading = false;
        this.toastService.success('Prix d\'achat enregistré avec succès!');
        this.closePrixAchatModal();

        // Mettre à jour le voyage dans le modal si ouvert
        if (this.selectedVoyage && updatedVoyage.id && this.selectedVoyage.id === updatedVoyage.id) {
          // Recharger les détails complets du voyage
          this.voyagesService.getVoyageById(updatedVoyage.id).subscribe({
            next: (reloadedVoyage) => {
              this.selectedVoyage = { ...this.selectedVoyage, ...reloadedVoyage };
            },
            error: (error) => {
              console.error('Erreur lors du rechargement:', error);
            }
          });
        }

        // Recharger les voyages
        if (this.activeTab === 'sans-prix-achat') {
          this.loadVoyagesSansPrixAchat();
        } else {
          this.loadVoyages();
        }
      },
      error: (error) => {
        this.isLoading = false;
        const errorMessage = this.getErrorMessage(error, 'Erreur lors de l\'enregistrement du prix d\'achat');
        this.toastService.error(errorMessage);
      }
    });
  }

  // ---- Modification du client et de la quantité d'un ClientVoyage (dans les détails du voyage) ----

  openEditClientVoyageModal(voyage: VoyageDisplay, clientVoyage: ClientVoyage) {
    if (!clientVoyage.id) {
      this.toastService.error('Client du voyage invalide');
      return;
    }
    this.clientVoyageForEdit = clientVoyage;
    this.quantiteClientVoyageValue = clientVoyage.quantite ? Number(clientVoyage.quantite) : undefined;
    this.editClientId = clientVoyage.clientId;
    this.showEditClientVoyageModal = true;
  }

  closeEditClientVoyageModal() {
    this.showEditClientVoyageModal = false;
    this.clientVoyageForEdit = null;
    this.quantiteClientVoyageValue = undefined;
     this.editClientId = undefined;
  }

  saveClientVoyageQuantite() {
    if (!this.selectedVoyage || !this.selectedVoyage.id) {
      this.toastService.error('Voyage invalide');
      return;
    }
    if (!this.clientVoyageForEdit || !this.clientVoyageForEdit.id) {
      this.toastService.error('Client du voyage invalide');
      return;
    }
    if (!this.editClientId) {
      this.toastService.warning('Veuillez sélectionner un client');
      return;
    }
    if (this.quantiteClientVoyageValue === undefined || this.quantiteClientVoyageValue === null || this.quantiteClientVoyageValue <= 0) {
      this.toastService.warning('Veuillez saisir une quantité valide');
      return;
    }

    const nouvelleQuantite = this.quantiteClientVoyageValue;

    // Vérifier côté front que la somme des quantités des autres clients + nouvelleQuantite
    // ne dépasse pas la quantité totale du voyage
    const totalAutres = (this.selectedVoyage.clientVoyages || [])
      .filter(cv => cv.id !== this.clientVoyageForEdit!.id)
      .reduce((sum, cv) => sum + (cv.quantite || 0), 0);

    const totalVoyage = this.selectedVoyage.quantite || 0;
    if (totalAutres + nouvelleQuantite > totalVoyage) {
      this.toastService.warning(
        `La somme des quantités (${totalAutres + nouvelleQuantite} L) dépasse la quantité du voyage (${totalVoyage} L)`
      );
      return;
    }

    const nouveauClient = this.clients.find(c => c.id === this.editClientId);
    const nomClient = nouveauClient?.nom || this.clientVoyageForEdit.clientNom || 'ce client';
    const message = `Confirmer la modification de ce client du voyage pour ${nomClient} avec une quantité de ${nouvelleQuantite} L ?`;
    this.alertService.confirm(message, 'Confirmation').subscribe(confirmed => {
      if (!confirmed) return;

      this.isLoading = true;
      this.voyagesService
        .updateClientVoyageQuantite(
          this.selectedVoyage!.id!,
          this.clientVoyageForEdit!.id!,
          this.editClientId!,
          nouvelleQuantite
        )
        .subscribe({
          next: (updatedVoyage) => {
            this.isLoading = false;
            this.toastService.success('Quantité du client mise à jour avec succès');
            this.closeEditClientVoyageModal();

            // Mettre à jour selectedVoyage avec les données complètes renvoyées
            if (this.selectedVoyage && updatedVoyage.id && this.selectedVoyage.id === updatedVoyage.id) {
              this.selectedVoyage = { ...this.selectedVoyage, ...updatedVoyage };
            } else if (updatedVoyage.id) {
              this.selectedVoyage = { ...(updatedVoyage as any) } as VoyageDisplay;
            }

            // Mettre à jour la liste principale des voyages si nécessaire
            const index = this.voyages.findIndex(v => v.id === updatedVoyage.id);
            if (index !== -1) {
              this.voyages[index] = { ...this.voyages[index], ...updatedVoyage };
              this.updateFilteredVoyages();
            }
          },
          error: (error) => {
            this.isLoading = false;
            const errorMessage = this.getErrorMessage(error, 'Erreur lors de la mise à jour de la quantité du client');
            this.toastService.error(errorMessage);
          }
        });
    });
  }

  showClientsList(clients: ClientVoyage[], event: MouseEvent) {
    event.stopPropagation();
    this.clientsForPopup = clients;
    this.showClientsPopup = true;
  }

  closeClientsPopup() {
    this.showClientsPopup = false;
    this.clientsForPopup = [];
  }

  // Méthodes pour gérer le toggle de livraison et le mode modifier
  toggleClientLivrer(clientVoyageId: number | undefined) {
    if (!clientVoyageId) return;
    this.dechargerClientLivres[clientVoyageId] = !this.dechargerClientLivres[clientVoyageId];
    // Si on désactive la livraison, sortir du mode modifier
    if (!this.dechargerClientLivres[clientVoyageId]) {
      this.dechargerModifierMode[clientVoyageId] = false;
    }
  }

  isClientLivrer(clientVoyageId: number | undefined): boolean {
    if (!clientVoyageId) return false;
    return this.dechargerClientLivres[clientVoyageId] || false;
  }

  toggleModifierMode(clientVoyageId: number | undefined) {
    if (!clientVoyageId) return;
    this.dechargerModifierMode[clientVoyageId] = !this.dechargerModifierMode[clientVoyageId];
  }

  isModifierMode(clientVoyageId: number | undefined): boolean {
    if (!clientVoyageId) return false;
    return this.dechargerModifierMode[clientVoyageId] || false;
  }

  isClientFieldDisabled(clientVoyageId: number | undefined): boolean {
    if (!clientVoyageId) return false;
    // Le champ est désactivé si le client est livré ET qu'on n'est pas en mode modifier
    return this.dechargerClientLivres[clientVoyageId] && !this.dechargerModifierMode[clientVoyageId];
  }

  /**
   * Quand la valeur du manquant change pour un client lors du DECHARGER :
   * - on enregistre la valeur saisie (y compris 0 si l'utilisateur le tape explicitement)
   * - si c'est la première modification, on coche automatiquement le client comme "livré"
   *   et on le met en mode modification.
   *
   * Très important : on ne met PLUS 0 par défaut. Aucune valeur = champ vide = pas de manquant envoyé.
   */
  onDechargerManquantChange(clientVoyageId: number | undefined, value: any): void {
    if (!clientVoyageId) return;

    // Normaliser la valeur saisie
    let parsed: number | undefined;
    if (value === null || value === undefined || value === '') {
      parsed = undefined;
    } else {
      const num = Number(value);
      parsed = !isNaN(num) && num >= 0 ? num : undefined;
    }

    // Mettre à jour la map des manquants
    if (parsed === undefined) {
      // Champ vidé => on supprime le manquant explicite
      delete this.dechargerManquants[clientVoyageId];
    } else {
      this.dechargerManquants[clientVoyageId] = parsed;
    }

    // Si c'est une vraie saisie (parsed défini) et que le client n'était pas encore marqué livré,
    // on le coche automatiquement et on l'autorise à modifier le champ.
    if (parsed !== undefined && !this.dechargerClientLivres[clientVoyageId]) {
      this.dechargerClientLivres[clientVoyageId] = true;
      this.dechargerModifierMode[clientVoyageId] = true;
    }
  }

  // Méthodes pour calculer les quantités dans le modal d'ajout de client
  getQuantiteTotaleAttribuee(): number {
    if (!this.selectedVoyage || !this.selectedVoyage.clientVoyages) {
      return 0;
    }
    return this.selectedVoyage.clientVoyages.reduce((sum, cv) => sum + (cv.quantite || 0), 0);
  }

  getQuantiteDisponible(): number {
    if (!this.selectedVoyage) {
      return 0;
    }
    const quantiteTotale = this.getQuantiteTotaleAttribuee();
    return (this.selectedVoyage.quantite || 0) - quantiteTotale;
  }

  /** Quantité restante non attribuée aux clients (pour affichage dans les listes). */
  getQuantiteRestanteNonAttribuee(voyage: VoyageDisplay | null): number {
    if (!voyage) return 0;
    const totalVoyage = voyage.quantite ?? 0;
    const totalAttribue = (voyage.clientVoyages ?? []).reduce((sum, cv) => sum + (cv.quantite ?? 0), 0);
    return Math.max(0, totalVoyage - totalAttribue);
  }

  getDefaultEtats(): EtatVoyage[] {
    return [
      { etat: 'Chargement', dateHeure: '', valider: false },
      { etat: 'Chargé', dateHeure: '', valider: false },
      { etat: 'Départ', dateHeure: '', valider: false },
      { etat: 'Arrivé', dateHeure: '', valider: false },
      { etat: 'Douane', dateHeure: '', valider: false },
      { etat: 'Réceptionné', dateHeure: '', valider: false },
      { etat: 'Livré', dateHeure: '', valider: false },
      { etat: 'Décharger', dateHeure: '', valider: false }
    ];
  }

  getStatutFromEtat(etat: string): string {
    const mapping: { [key: string]: string } = {
      'Chargement': 'CHARGEMENT',
      'Chargé': 'CHARGE',
      'Départ': 'DEPART',
      'Arrivé': 'ARRIVER',
      'Douane': 'DOUANE',
      'Réceptionné': 'RECEPTIONNER',
      'Livré': 'LIVRE',
      'Décharger': 'DECHARGER'
    };
    return mapping[etat] || '';
  }

  getEtatLabel(etat: string): string {
    // Mapper "Livré" vers "Attribué" et "Réceptionné" vers "Sortie de douane" pour l'affichage uniquement
    if (etat === 'Livré') {
      return 'Attribué';
    }
    if (etat === 'Réceptionné') {
      return 'Sortie de douane';
    }
    return etat;
  }

  canSelectEtat(etat: EtatVoyage): boolean {
    return !etat.valider;
  }

  /**
   * Retourne les états visibles dans le modal de changement de statut.
   * Basé sur le responsable du voyage (Voyage.responsable / responsableIdentifiant) et l'identifiant connecté (auth) :
   * - Responsable du voyage (ou Admin/Contrôleur) : états déjà validés + le prochain état à valider.
   * - Autres comptes : uniquement les états déjà validés (ils ne voient pas le prochain à valider).
   */
  getVisibleEtats(): EtatVoyage[] {
    if (!this.etatsVoyage || this.etatsVoyage.length === 0) {
      return [];
    }

    const ordreEtats = ['Chargement', 'Chargé', 'Départ', 'Arrivé', 'Douane', 'Réceptionné', 'Livré', 'Décharger'];

    const etatsTries: EtatVoyage[] = [];
    for (const etatNom of ordreEtats) {
      const etatExistant = this.etatsVoyage.find(e => e.etat === etatNom);
      if (etatExistant) {
        etatsTries.push(etatExistant);
      } else {
        etatsTries.push({ etat: etatNom, dateHeure: '', valider: false });
      }
    }

    let result = etatsTries;
    if (this.voyageForStatutChange && !this.voyageForStatutChange.liberer) {
      result = etatsTries.filter(e => e.etat !== 'Livré');
    }

    // Responsable = identifiant connecté === voyage.responsableIdentifiant (ou Admin/Contrôleur) ; voir Voyage.responsable côté backend
    const peutVoirProchainEtat = this.voyageForStatutChange && this.canUpdateVoyageStatus(this.voyageForStatutChange);

    // Responsable du voyage (ou Admin/Contrôleur) : validés + prochain à valider
    if (peutVoirProchainEtat) {
      const premierNonValideIndex = result.findIndex(e => !e.valider);
      if (premierNonValideIndex === -1) {
        return result;
      }
      return result.slice(0, premierNonValideIndex + 1);
    }

    // Autres comptes : uniquement les états déjà validés
    return result.filter(e => e.valider);
  }

  confirmStatutChange() {
    if (!this.voyageForStatutChange || !this.voyageForStatutChange.id || !this.selectedStatut) {
      this.toastService.warning('Veuillez sélectionner un état');
      return;
    }

    // Vérifier si le statut est "Douane" et si le voyage n'a pas de transitaire
    if (this.selectedStatut === 'DOUANE' && !this.voyageForStatutChange.transitaireId) {
      this.alertService.error('Ce voyage n\'est lié à aucun transitaire. Veuillez assigner un transitaire avant de mettre le statut à "Douane".').subscribe();
      return;
    }

    // Vérifier si le statut est "LIVRE"
    if (this.selectedStatut === 'LIVRE') {
      // Vérifier qu'au moins un client est assigné
      const hasExistingClients = this.voyageForStatutChange?.clientVoyages && this.voyageForStatutChange.clientVoyages.length > 0;
      if (!hasExistingClients && (!this.livreClients || this.livreClients.length === 0)) {
        this.toastService.warning('Veuillez sélectionner au moins un client pour livrer ce voyage');
        return;
      }
      // Vérifier que tous les clients ont une quantité
      if (this.livreClients.some(c => !c.clientId || !c.quantite || c.quantite <= 0)) {
        this.toastService.warning('Veuillez saisir une quantité valide pour tous les clients');
        return;
      }
      // Vérifier que la somme des quantités ne dépasse pas la quantité du voyage
      const totalQuantite = this.getTotalQuantiteClients();
      if (this.voyageForStatutChange && this.voyageForStatutChange.quantite && totalQuantite > this.voyageForStatutChange.quantite) {
        this.toastService.warning(`La somme des quantités (${totalQuantite}) dépasse la quantité du voyage (${this.voyageForStatutChange.quantite})`);
        return;
      }
    }

    // Vérifier si l'état est déjà validé
    const selectedEtat = this.etatsVoyage.find(e => this.getStatutFromEtat(e.etat) === this.selectedStatut);
    if (selectedEtat && selectedEtat.valider) {
      this.toastService.warning('Cet état est déjà validé. Vous ne pourrez plus le modifier.');
      return;
    }

    // Afficher l'alerte d'abord
    const etatLabel = selectedEtat?.etat || this.getStatutLabel(this.selectedStatut);
    const confirmMessage = `Vous êtes sur le point de valider l'état: "${etatLabel}"\n\nAttention: Une fois validé, vous ne pourrez plus prendre ce même état.`;

    this.alertService.confirm(confirmMessage, 'Confirmation').subscribe(confirmed => {
      if (!confirmed) {
        return; // L'utilisateur a annulé
      }
      this.proceedWithStatutChange();
    });
  }

  private proceedWithStatutChange() {

    // Enregistrer après confirmation
    this.isLoading = true;
    const voyageId = this.voyageForStatutChange?.id;
    if (voyageId === undefined) {
      this.isLoading = false;
      this.toastService.error('ID de voyage invalide.');
      return;
    }

    // Validation spéciale pour DECHARGER : au moins un client doit être coché
    if (this.selectedStatut === 'DECHARGER') {
      const hasClientLivrer = this.voyageForStatutChange?.clientVoyages?.some(cv =>
        cv.id && this.dechargerClientLivres[cv.id]
      ) || false;

      if (!hasClientLivrer) {
        this.isLoading = false;
        this.toastService.error('Veuillez cocher au moins un client livré avant de décharger.');
        return;
      }
    }

    // Préparer les paramètres pour le statut LIVRE ou DECHARGER
    let params: any = undefined;
    if (this.selectedStatut === 'LIVRE') {
      // Pour LIVRE, envoyer tous les clients avec leurs quantités
      params = {};
      if (this.livreClients && this.livreClients.length > 0) {
        const clients = this.livreClients
          .filter(c => c.clientId && c.quantite && c.quantite > 0)
          .map(c => ({
            clientId: c.clientId,
            quantite: c.quantite
          }));
        if (clients.length > 0) {
          params.clients = clients;
        }
      } else if (this.voyageForStatutChange?.clientVoyages && this.voyageForStatutChange.clientVoyages.length > 0) {
        // Si pas de nouveaux clients mais des clients existants, les utiliser
        const clients = this.voyageForStatutChange.clientVoyages
          .filter(cv => cv.clientId && cv.quantite && cv.quantite > 0)
          .map(cv => ({
            clientId: cv.clientId,
            quantite: cv.quantite
          }));
        if (clients.length > 0) {
          params.clients = clients;
        }
      } else if (this.voyageForStatutChange?.clientId) {
        // Compatibilité avec l'ancien format
        params.clientId = this.voyageForStatutChange.clientId;
      }
      // Si params est vide après tout ça, le mettre à undefined
      if (Object.keys(params).length === 0) {
        params = undefined;
      }
    } else if (this.selectedStatut === 'DECHARGER') {
      // Pour DECHARGER, envoyer uniquement les manquants pour les ClientVoyage marqués comme livrés
      // Cette validation a déjà été faite plus haut, donc on est sûr qu'au moins un client est coché
      params = {};
      params.manquants = {};

      // Ne traiter que les ClientVoyage marqués comme livrés
      if (this.voyageForStatutChange?.clientVoyages && this.voyageForStatutChange.clientVoyages.length > 0) {
        this.voyageForStatutChange.clientVoyages.forEach(cv => {
          if (cv.id && this.dechargerClientLivres[cv.id]) {
            // Seulement si le client est marqué comme livré
            const manquant = this.dechargerManquants[cv.id];
            if (manquant !== undefined && manquant !== null && manquant >= 0) {
              // Manquant saisi explicitement (y compris 0 saisi par l'utilisateur)
              params.manquants[Number(cv.id)] = manquant;
            } else if (cv.manquant !== undefined && cv.manquant !== null && cv.manquant >= 0) {
              // Utiliser le manquant existant s'il existe déjà côté backend
              params.manquants[Number(cv.id)] = cv.manquant;
            }
            // Sinon : aucun manquant n'est envoyé pour ce client (différent d'un 0 explicite)
          }
        });
      }

      // Double vérification : si aucun ClientVoyage n'est marqué comme livré après traitement,
      // cela ne devrait pas arriver grâce à la validation plus haut, mais on vérifie quand même
      if (Object.keys(params.manquants).length === 0) {
        this.isLoading = false;
        this.toastService.error('Aucun client livré sélectionné. Impossible de décharger.');
        return;
      }
    }
    // Pour les autres statuts, params reste undefined

    this.voyagesService.updateStatut(voyageId, this.selectedStatut!, params).subscribe({
      next: (updatedVoyage) => {
        this.isLoading = false;
        this.toastService.success('Statut du voyage mis à jour avec succès!');

        // Mettre à jour immédiatement les états depuis la réponse
        if (updatedVoyage.etats && updatedVoyage.etats.length > 0) {
          // S'assurer que tous les états par défaut sont présents
          const defaultEtats = this.getDefaultEtats();
          const existingEtats = new Map(updatedVoyage.etats.map(e => [e.etat, e]));
          // Créer une nouvelle référence du tableau pour déclencher la détection de changement
          this.etatsVoyage = [...defaultEtats.map(defaultEtat => {
            const existing = existingEtats.get(defaultEtat.etat);
            return existing || defaultEtat;
          })];
        } else {
          // Si pas d'états, utiliser les états par défaut
          this.etatsVoyage = [...this.getDefaultEtats()];
        }

        // Réinitialiser la sélection
        this.selectedStatut = '';

        // Mettre à jour le voyage dans la liste principale
        const index = this.voyages.findIndex(v => v.id === this.voyageForStatutChange!.id);
        if (index !== -1) {
          this.voyages[index].statut = updatedVoyage.statut as any;
          this.voyages[index].etats = updatedVoyage.etats || [];
          this.updateFilteredVoyages();
        }

        // Mettre à jour aussi le voyage dans voyageForStatutChange pour que le statut affiché soit à jour
        if (this.voyageForStatutChange) {
          this.voyageForStatutChange.statut = updatedVoyage.statut as any;
          this.voyageForStatutChange.etats = updatedVoyage.etats || [];
        }

        // Si le voyage sélectionné est celui qui a été mis à jour, mettre à jour aussi
        if (this.selectedVoyage && this.voyageForStatutChange && this.selectedVoyage.id === this.voyageForStatutChange.id) {
          this.selectedVoyage.statut = updatedVoyage.statut as any;
          this.selectedVoyage.etats = updatedVoyage.etats || [];
        }
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour du statut:', error);
        this.isLoading = false;
        const errorMessage = this.getErrorMessage(error, 'Erreur lors de la mise à jour du statut du voyage');
        this.toastService.error(errorMessage);
      }
    });
  }

  getStatutLabel(statut: string | undefined): string {
    if (!statut) return 'N/A';
    const labels: { [key: string]: string } = {
      'CHARGEMENT': 'Chargement',
      'CHARGE': 'Chargé',
      'DEPART': 'Départ',
      'PARTIELLEMENT_DECHARGER': 'Partiellement Déchargé',
      'ARRIVER': 'Arrivé',
      'DOUANE': 'Douane',
      'RECEPTIONNER': 'Sortie de douane',
      'LIVRE': 'Attribué',
      'DECHARGER': 'Décharger'
    };
    return labels[statut] || statut;
  }

  getStatutClass(statut: string | undefined): string {
    if (!statut) return 'badge-gray';
    const classes: { [key: string]: string } = {
      'CHARGEMENT': 'badge-blue',
      'CHARGE': 'badge-orange',
      'DEPART': 'badge-purple',
      'ARRIVER': 'badge-green',
      'DOUANE': 'badge-yellow',
      'RECEPTIONNER': 'badge-teal',
      'LIVRE': 'badge-teal',
      'DECHARGER': 'badge-gray'
    };
    return classes[statut] || 'badge-gray';
  }

  /** True si l'utilisateur peut ouvrir le modal et modifier le statut.
   * - Admin : toujours.
   * - Contrôleur : uniquement si voyage.liberer et au stade Livrer/DECHARGER/PartiellementDecharcher (RECEPTIONNER, LIVRE, etc.).
   * - Responsable (sans Admin/Contrôleur) : uniquement si responsable du voyage et pas au stade Livrer/DECHARGER/PartiellementDecharcher. */
  canUpdateVoyageStatus(voyage: Voyage): boolean {
    if (this.authService.isAdmin()) return true;
    if (this.authService.isControleur()) {
      return voyage?.liberer === true && ['RECEPTIONNER', 'LIVRE', 'DECHARGER', 'PARTIELLEMENT_DECHARGER'].includes(voyage.statut || '');
    }
    const identifiant = this.authService.getIdentifiant();
    if (!identifiant || !voyage?.responsableIdentifiant) return false;
    if (identifiant !== voyage.responsableIdentifiant) return false;
    const statutsSansDroitResponsable = ['RECEPTIONNER', 'LIVRE', 'DECHARGER', 'PARTIELLEMENT_DECHARGER'];
    return !statutsSansDroitResponsable.includes(voyage.statut || '');
  }

  /** True si l'utilisateur peut ajouter/attribuer des clients au voyage (Contrôleur ou Admin). */
  canAssignClientByRole(): boolean {
    return this.authService.isAdmin() || this.authService.isComptable();
  }

  /** True si l'utilisateur est un logisticien (pas Admin, pas Responsable logistique, pas Contrôleur) : ne voit que ses voyages. */
  isLogisticienOnly(): boolean {
    return this.authService.isLogisticien() && !this.authService.isAdmin()
      && !this.authService.isResponsableLogistique() && !this.authService.isControleur();
  }

  isComptable(): boolean {
    return this.authService.isComptable();
  }

  // Modal modification prix unitaire transport (comptable) — référence stable
  editingVoyageForPrix: VoyageDisplay | null = null;
  voyageRefToPass: VoyagePrixRef | null = null;

  openEditPrixModal(voyage: VoyageDisplay) {
    this.editingVoyageForPrix = voyage;
    this.voyageRefToPass = {
      id: voyage.id!,
      numeroVoyage: voyage.numeroVoyage,
      quantite: voyage.quantite,
      prixUnitaire: voyage.prixUnitaire
    };
  }

  closeEditPrixModal() {
    this.editingVoyageForPrix = null;
    this.voyageRefToPass = null;
  }

  onSavePrixUnitaire(prixUnitaire: number) {
    if (!this.editingVoyageForPrix?.id || prixUnitaire <= 0) {
      this.toastService.warning('Veuillez saisir un prix valide');
      return;
    }
    const payload = { ...this.editingVoyageForPrix, prixUnitaire };
    this.voyagesService.updateVoyage(this.editingVoyageForPrix.id, payload).subscribe({
      next: (updated) => {
        if (this.selectedVoyage?.id === updated.id) {
          this.selectedVoyage = { ...this.selectedVoyage!, prixUnitaire: updated.prixUnitaire };
        }
        const idx = this.voyages.findIndex(v => v.id === updated.id);
        if (idx !== -1) this.voyages[idx] = { ...this.voyages[idx], prixUnitaire: updated.prixUnitaire };
        if (this.activeTab === 'sans-prix-transport' && this.voyagesSansPrixTransportPage) {
          this.loadVoyagesSansPrixTransport();
        }
        this.toastService.success('Prix unitaire mis à jour');
        this.closeEditPrixModal();
      },
      error: (err) => {
        this.toastService.error(err?.error?.message || 'Erreur lors de la mise à jour du prix');
      }
    });
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {
        return dateString;
      }
      return date.toLocaleDateString('fr-FR', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return dateString;
    }
  }

  private sortByDateDepartAsc<T extends { dateDepart?: string }>(voyages: T[]): T[] {
    return voyages.sort((a, b) => {
      const da = a.dateDepart ? new Date(a.dateDepart).getTime() : 0;
      const db = b.dateDepart ? new Date(b.dateDepart).getTime() : 0;
      return da - db;
    });
  }

  getClientInitiales(clientNom: string | undefined): string {
    if (!clientNom) return '??';
    return clientNom.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
  }

  getClientColor(clientNom: string | undefined): string {
    if (!clientNom) return 'gray';
    const colors = ['blue', 'purple', 'red', 'green', 'orange', 'teal', 'pink'];
    const hash = clientNom.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    return colors[hash % colors.length];
  }

  /**
   * Extrait le message d'erreur du backend depuis l'objet d'erreur HTTP
   * Essaie plusieurs propriétés pour trouver le message le plus pertinent
   * Spring Boot retourne généralement les messages dans error.error.message
   */
  getErrorMessage(error: any, defaultMessage: string = 'Une erreur est survenue'): string {
    if (!error) {
      return defaultMessage;
    }

    // Essayer d'extraire le message depuis différentes propriétés
    // Spring Boot peut retourner les erreurs dans différentes structures

    // 1. Message personnalisé du backend (le plus courant avec RuntimeException)
    if (error.error) {
      // Message dans error.error.message (cas le plus fréquent)
      if (error.error.message) {
        if (typeof error.error.message === 'string') {
          // Message direct
          return error.error.message;
        } else if (Array.isArray(error.error.message)) {
          // Message dans un tableau (validation errors)
          return error.error.message.join(', ');
        }
      }

      // Message dans error.error (si c'est une string directe)
      if (typeof error.error === 'string') {
        return error.error;
      }

      // Message dans error.error.error (type d'erreur, mais on l'évite si c'est générique)
      if (error.error.error && typeof error.error.error === 'string') {
        const errorType = error.error.error;
        // Éviter les messages génériques comme "Bad Request", "Internal Server Error"
        if (errorType !== 'Bad Request' &&
          errorType !== 'Internal Server Error' &&
          errorType !== 'Not Found' &&
          errorType !== 'Forbidden') {
          return errorType;
        }
      }

      // Message dans error.error.detail (parfois utilisé par Spring)
      if (error.error.detail && typeof error.error.detail === 'string') {
        return error.error.detail;
      }
    }

    // 2. Message HTTP standard (à éviter si possible car trop technique)
    if (error.message && typeof error.message === 'string') {
      // Éviter les messages techniques comme "Http failure response"
      if (!error.message.includes('Http failure') &&
        !error.message.includes('HttpErrorResponse') &&
        !error.message.includes('status code')) {
        return error.message;
      }
    }

    // 3. Status text (dernier recours avant le message par défaut)
    if (error.statusText && typeof error.statusText === 'string') {
      // Éviter les messages génériques
      if (error.statusText !== 'Bad Request' &&
        error.statusText !== 'Internal Server Error' &&
        error.statusText !== 'OK') {
        return error.statusText;
      }
    }

    // Dernier recours : message par défaut
    return defaultMessage;
  }

  viewVoyage(voyage: VoyageDisplay) {
    this.selectedVoyage = voyage;
    this.activeDetailTab = 'details';
    this.showDetailModal = true;
    this.showAddTransitaireForm = false;
    this.showAddFraisForm = false;
    this.selectedTransitaireId = voyage.transitaireId || undefined;
    this.selectedClientId = undefined;
    this.resetNewFrais();

    // Charger les données complètes du voyage depuis l'API pour avoir les clientVoyages
    if (voyage.id) {
      this.voyagesService.getVoyageById(voyage.id).subscribe({
        next: (updatedVoyage) => {
          // Mettre à jour le voyage sélectionné avec les données complètes
          this.selectedVoyage = {
            ...this.selectedVoyage!,
            ...updatedVoyage,
            clientVoyages: updatedVoyage.clientVoyages || []
          };
        },
        error: (error) => {
          console.error('Erreur lors du chargement des détails du voyage:', error);
        }
      });
    }
  }

  closeDetailModal() {
    this.showDetailModal = false;
    this.selectedVoyage = null;
    this.activeDetailTab = 'details';
    this.showAddTransitaireForm = false;
    this.showAddFraisForm = false;
    this.selectedTransitaireId = undefined;
    this.resetNewFrais();
  }

  setDetailTab(tab: 'details' | 'frais') {
    this.activeDetailTab = tab;
  }

  canAssignClient(voyage: VoyageDisplay | null): boolean {
    return !!voyage &&
      (this.getQuantiteTotaleAttribuee() < (voyage.quantite ?? 0));
  }

  // getQuantiteTotaleAttribuee(voyage: VoyageDisplay): number {
  //   return voyage.clientVoyages?.map(cv => cv.quantite).reduce((sum, quantite) => sum + quantite, 0) || 0;
  // }

  assignClient() {
    if (!this.selectedVoyage || !this.selectedVoyage.id) {
      this.toastService.error('Voyage introuvable');
      return;
    }
    if (!this.selectedClientId || this.selectedClientId <= 0) {
      this.toastService.warning('Veuillez sélectionner un client');
      return;
    }

    this.isAssigningClient = true;
    const payload: Voyage = {
      camionId: this.selectedVoyage.camionId,
      clientId: this.selectedClientId,
      transitaireId: this.selectedVoyage.transitaireId,
      produitId: this.selectedVoyage.produitId,
      depotId: this.selectedVoyage.depotId,
      dateDepart: this.selectedVoyage.dateDepart,
      dateArrivee: this.selectedVoyage.dateArrivee,
      destination: this.selectedVoyage.destination,
      lieuDepart: this.selectedVoyage.lieuDepart,
      statut: this.selectedVoyage.statut,
      quantite: this.selectedVoyage.quantite,
      notes: this.selectedVoyage.notes
    };

    this.voyagesService.updateVoyage(this.selectedVoyage.id, payload).subscribe({
      next: (updated) => {
        // Mettre à jour la sélection
        this.selectedVoyage = {
          ...this.selectedVoyage!,
          ...updated
        };
        // Mettre à jour la liste
        this.voyages = this.voyages.map(v =>
          v.id === updated.id ? { ...v, ...updated } as VoyageDisplay : v
        );
        this.updateFilteredVoyages();
        this.selectedClientId = undefined;
        this.isAssigningClient = false;
        this.toastService.success('Client assigné au voyage');
      },
      error: (error) => {
        console.error('Erreur lors de l\'assignation du client:', error);
        const errorMessage = this.getErrorMessage(error, 'Impossible d\'assigner le client. Vérifiez les informations.');
        this.toastService.error(errorMessage);
        this.isAssigningClient = false;
      }
    });
  }

  getTotalFrais(): number {
    if (!this.selectedVoyage || !this.selectedVoyage.transactions) return 0;
    return this.selectedVoyage.transactions.reduce((sum, t) => sum + (t.montant || 0), 0);
  }

  getTransactionTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'DEPOT': 'Dépôt',
      'RETRAIT': 'Retrait',
      'VIREMENT_ENTRANT': 'Virement entrant',
      'VIREMENT_SORTANT': 'Virement sortant',
      'FRAIS': 'Frais',
      'INTERET': 'Intérêt',
      'FRAIS_LOCATION': 'Frais de location',
      'FRAIS_FRONTIERE': 'Frais frontière',
      'TS_FRAIS_PRESTATIONS': 'TS Frais de prestations',
      'FRAIS_REPERTOIRE': 'Frais répertoire',
      'FRAIS_CHAMBRE_COMMERCE': 'Frais chambre de commerce'
    };
    return labels[type] || type;
  }

  getAvailableFraisTypes(): string[] {
    if (!this.selectedVoyage) {
      return ['FRAIS_FRONTIERE', 'TS_FRAIS_PRESTATIONS', 'FRAIS_REPERTOIRE', 'FRAIS_CHAMBRE_COMMERCE'];
    }

    // Si le voyage est à la douane, on propose les frais de douane
    if (this.selectedVoyage.statut === 'DOUANE') {
      return ['TS_FRAIS_PRESTATIONS', 'FRAIS_REPERTOIRE', 'FRAIS_CHAMBRE_COMMERCE'];
    }

    // Par défaut, on propose tous les types
    return ['FRAIS_FRONTIERE', 'TS_FRAIS_PRESTATIONS', 'FRAIS_REPERTOIRE', 'FRAIS_CHAMBRE_COMMERCE'];
  }

  getFraisTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'FRAIS_FRONTIERE': 'Frais frontière',
      'TS_FRAIS_PRESTATIONS': 'TS Frais de prestations',
      'FRAIS_REPERTOIRE': 'Frais répertoire',
      'FRAIS_CHAMBRE_COMMERCE': 'Frais chambre de commerce'
    };
    return labels[type] || type;
  }

  formatDateTime(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {
        return dateString;
      }
      return date.toLocaleString('fr-FR', {
        day: 'numeric',
        month: 'long',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return dateString;
    }
  }

  toggleAddTransitaireForm() {
    this.showAddTransitaireForm = !this.showAddTransitaireForm;
    if (this.showAddTransitaireForm) {
      this.showAddFraisForm = false;
    }
  }

  assignTransitaire() {
    if (!this.selectedVoyage || !this.selectedVoyage.id || !this.selectedTransitaireId) {
      this.toastService.warning('Veuillez sélectionner un transitaire');
      return;
    }

    this.isLoading = true;

    this.voyagesService.assignTransitaire(this.selectedVoyage.id, this.selectedTransitaireId).subscribe({
      next: (updated) => {
        this.isLoading = false;
        this.toastService.success('Transitaire assigné avec succès!');
        this.showAddTransitaireForm = false;
        this.loadVoyages();
        // Mettre à jour le voyage sélectionné
        const updatedDisplay: VoyageDisplay = {
          ...updated,
          camionImmatriculation: updated.camionImmatriculation,
          clientNom: updated.clientNom,
          clientEmail: updated.clientEmail,
          transitaireNom: updated.transitaireNom,
          transitaireIdentifiant: updated.transitaireIdentifiant,
          transitairePhone: updated.transitairePhone,
          typeProduit: updated.typeProduit,
          transactions: updated.transactions || []
        };
        this.selectedVoyage = updatedDisplay;
        this.selectedTransitaireId = updated.transitaireId || undefined;
      },
      error: (error) => {
        console.error('Erreur lors de l\'assignation du transitaire:', error);
        this.isLoading = false;
        const errorMessage = this.getErrorMessage(error, 'Erreur lors de l\'assignation du transitaire');
        this.toastService.error(errorMessage);
      }
    });
  }

  toggleAddFraisForm() {
    this.showAddFraisForm = !this.showAddFraisForm;
    if (this.showAddFraisForm) {
      this.showAddTransitaireForm = false;
      this.resetNewFrais();
    }
  }

  resetNewFrais() {
    this.newFrais = {
      type: 'FRAIS_FRONTIERE',
      montant: 0,
      date: new Date().toISOString().split('T')[0],
      statut: 'EN_ATTENTE',
      description: '',
      compteId: undefined,
      caisseId: undefined
    };
    this.compteType = 'banque';
  }

  onCompteTypeChange() {
    // Réinitialiser les IDs quand on change de type
    this.newFrais.compteId = undefined;
    this.newFrais.caisseId = undefined;
  }

  addFrais() {
    if (!this.selectedVoyage || !this.selectedVoyage.id) {
      this.toastService.warning('Aucun voyage sélectionné');
      return;
    }

    if (!this.newFrais.montant || this.newFrais.montant <= 0) {
      this.toastService.warning('Veuillez saisir un montant valide');
      return;
    }

    if (!this.newFrais.description || this.newFrais.description.trim() === '') {
      this.toastService.warning('Veuillez saisir une description');
      return;
    }

    // Vérifier qu'un compte ou une caisse est sélectionné
    if (this.compteType === 'banque' && !this.newFrais.compteId) {
      this.toastService.warning('Veuillez sélectionner un compte bancaire');
      this.isLoading = false;
      return;
    }

    if (this.compteType === 'caisse' && !this.newFrais.caisseId) {
      this.toastService.warning('Veuillez sélectionner une caisse');
      this.isLoading = false;
      return;
    }

    this.isLoading = true;
    const transaction: Transaction = {
      type: this.newFrais.type || 'FRAIS_FRONTIERE',
      montant: this.newFrais.montant!,
      date: new Date(this.newFrais.date!).toISOString(),
      statut: this.newFrais.statut || 'VALIDE', // Par défaut VALIDE pour débit immédiat
      description: this.newFrais.description,
      reference: this.selectedVoyage.numeroVoyage,
      beneficiaire: this.selectedVoyage.transitaireNom || 'Transitaire',
      voyageId: this.selectedVoyage.id,
      compteId: this.compteType === 'banque' ? this.newFrais.compteId : undefined,
      caisseId: this.compteType === 'caisse' ? this.newFrais.caisseId : undefined
    };

    // Utiliser createPaiement pour débit automatique
    this.transactionsService.createPaiement(transaction).subscribe({
      next: (createdTransaction) => {
        this.isLoading = false;
        this.toastService.success('Frais ajouté avec succès!');
        this.showAddFraisForm = false;
        this.resetNewFrais();
        // Recharger le voyage pour avoir les transactions à jour
        if (this.selectedVoyage?.id) {
          this.voyagesService.getVoyageById(this.selectedVoyage.id).subscribe({
            next: (updatedVoyage) => {
              const updatedDisplay: VoyageDisplay = {
                ...updatedVoyage,
                camionImmatriculation: updatedVoyage.camionImmatriculation,
                clientNom: updatedVoyage.clientNom,
                clientEmail: updatedVoyage.clientEmail,
                transitaireNom: updatedVoyage.transitaireNom,
                transitaireIdentifiant: updatedVoyage.transitaireIdentifiant,
                typeProduit: updatedVoyage.typeProduit,
                transactions: updatedVoyage.transactions || []
              };
              this.selectedVoyage = updatedDisplay;
            },
            error: (error) => {
              console.error('Erreur lors du rechargement du voyage:', error);
            }
          });
        }
      },
      error: (error) => {
        console.error('Erreur lors de l\'ajout du frais:', error);
        this.isLoading = false;
        const errorMessage = this.getErrorMessage(error, 'Erreur lors de l\'ajout du frais');
        this.toastService.error(errorMessage);
      }
    });
  }

  // Méthodes pour les bons d'enlèvement
  get voyagesEnChargement(): VoyageDisplay[] {
    return this.filteredVoyages.filter(v => v.statut === 'CHARGEMENT');
  }

  toggleVoyageSelection(voyageId: number | undefined): void {
    if (!voyageId) return;
    if (this.selectedVoyagesForBon.has(voyageId)) {
      this.selectedVoyagesForBon.delete(voyageId);
    } else {
      this.selectedVoyagesForBon.add(voyageId);
    }
  }

  isVoyageSelected(voyageId: number | undefined): boolean {
    if (!voyageId) return false;
    return this.selectedVoyagesForBon.has(voyageId);
  }

  toggleSelectAllChargement(): void {
    const voyagesChargement = this.voyagesEnChargement;
    if (voyagesChargement.length === 0) return;

    const allSelected = voyagesChargement.every(v => v.id && this.selectedVoyagesForBon.has(v.id));

    if (allSelected) {
      // Désélectionner tous
      voyagesChargement.forEach(v => {
        if (v.id) this.selectedVoyagesForBon.delete(v.id);
      });
    } else {
      // Sélectionner tous
      voyagesChargement.forEach(v => {
        if (v.id) this.selectedVoyagesForBon.add(v.id);
      });
    }
  }

  get allChargementSelected(): boolean {
    const voyagesChargement = this.voyagesEnChargement;
    if (voyagesChargement.length === 0) return false;
    return voyagesChargement.every(v => v.id && this.selectedVoyagesForBon.has(v.id));
  }

  get selectedChargementCount(): number {
    return this.selectedVoyagesForBon.size;
  }

  async generateBonEnlevement(voyageId?: number): Promise<void> {
    let voyagesToGenerate: VoyageDisplay[] = [];

    if (voyageId) {
      // Générer pour un seul voyage
      const voyage = this.voyages.find(v => v.id === voyageId);
      if (voyage && voyage.statut === 'CHARGEMENT') {
        voyagesToGenerate = [voyage];
      } else {
        this.toastService.warning('Ce voyage n\'est pas en chargement');
        return;
      }
    } else {
      // Générer pour les voyages sélectionnés
      if (this.selectedVoyagesForBon.size === 0) {
        this.toastService.warning('Veuillez sélectionner au moins un voyage en chargement');
        return;
      }
      voyagesToGenerate = this.voyages.filter(v =>
        v.id && this.selectedVoyagesForBon.has(v.id) && v.statut === 'CHARGEMENT'
      );
    }

    if (voyagesToGenerate.length === 0) {
      this.toastService.warning('Aucun voyage en chargement sélectionné');
      return;
    }

    try {
      this.isLoading = true;

      // Générer les numéros de bon d'enlèvement pour les voyages qui n'en ont pas
      const promises: Promise<void>[] = [];
      for (const voyage of voyagesToGenerate) {
        if (voyage.id && !voyage.numeroBonEnlevement) {
          const promise = new Promise<void>((resolve, reject) => {
            this.voyagesService.genererNumeroBonEnlevement(voyage.id!).subscribe({
              next: (updatedVoyage) => {
                // Mettre à jour le voyage dans la liste
                const index = voyagesToGenerate.findIndex(v => v.id === voyage.id);
                if (index !== -1) {
                  voyagesToGenerate[index].numeroBonEnlevement = updatedVoyage.numeroBonEnlevement;
                }
                resolve();
              },
              error: (error) => {
                console.error(`Erreur lors de la génération du numéro pour le voyage ${voyage.id}:`, error);
                resolve(); // Continuer même en cas d'erreur
              }
            });
          });
          promises.push(promise);
        }
      }

      // Attendre que tous les numéros soient générés
      await Promise.all(promises);

      await this.pdfService.generateBonEnlevement(voyagesToGenerate);
      this.toastService.success(`${voyagesToGenerate.length} bon(s) d'enlèvement généré(s) avec succès`);
      // Réinitialiser la sélection après génération
      this.selectedVoyagesForBon.clear();
      // Recharger les voyages pour avoir les numéros mis à jour
      this.loadVoyages();
      this.isLoading = false;
    } catch (error: any) {
      console.error('Erreur lors de la génération du bon d\'enlèvement:', error);
      const errorMessage = this.getErrorMessage(error, 'Erreur lors de la génération du bon d\'enlèvement');
      this.toastService.error(errorMessage);
      this.isLoading = false;
    }
  }

  /**
   * Génère un fichier Excel avec la liste des camions pour un dépôt
   * Groupé par dépôt pour les voyages en chargement
   */
  async generateListeCamionsExcel(): Promise<void> {
    // Filtrer les voyages en chargement
    const voyagesChargement = this.voyages.filter(v => v.statut === 'CHARGEMENT');

    if (voyagesChargement.length === 0) {
      this.toastService.warning('Aucun voyage en chargement trouvé');
      return;
    }

    // Grouper les voyages par dépôt
    const voyagesByDepot = new Map<string, VoyageDisplay[]>();
    voyagesChargement.forEach(voyage => {
      const depotKey = voyage.depotNom || voyage.depotId?.toString() || 'UNKNOWN';
      if (!voyagesByDepot.has(depotKey)) {
        voyagesByDepot.set(depotKey, []);
      }
      voyagesByDepot.get(depotKey)!.push(voyage);
    });

    // Générer un fichier Excel par dépôt
    for (const [depotKey, depotVoyages] of voyagesByDepot.entries()) {
      const depotNom = depotVoyages[0]?.depotNom || depotKey;

      // Trier les voyages par ID pour avoir un ordre cohérent
      const sortedVoyages = [...depotVoyages].sort((a, b) => (a.id || 0) - (b.id || 0));

      // Générer les numéros de bon d'enlèvement pour les voyages qui n'en ont pas
      const genererNumeroPromises = sortedVoyages
        .filter(voyage => voyage.id && !voyage.numeroBonEnlevement)
        .map(voyage =>
          this.voyagesService.genererNumeroBonEnlevement(voyage.id!).pipe(
            // Mettre à jour le voyage dans la liste après génération
            tap(updatedVoyage => {
              const index = sortedVoyages.findIndex(v => v.id === voyage.id);
              if (index !== -1 && updatedVoyage.numeroBonEnlevement) {
                sortedVoyages[index].numeroBonEnlevement = updatedVoyage.numeroBonEnlevement;
              }
            }),
            catchError(error => {
              console.error(`Erreur lors de la génération du numéro pour le voyage ${voyage.id}:`, error);
              return of(null);
            })
          )
        );

      // Attendre que tous les numéros soient générés
      if (genererNumeroPromises.length > 0) {
        await forkJoin(genererNumeroPromises).toPromise();
      }

      // Préparer les données pour Excel
      const camionsData: CamionExcelData[] = sortedVoyages.map((voyage, index) => {
        // Utiliser le numéro de bon d'enlèvement du voyage (doit être généré avant)
        // Si le format est "00297-SFB/2025", extraire juste "00297"
        let bonNumber = voyage.numeroBonEnlevement || '';
        if (bonNumber && bonNumber.includes('-')) {
          bonNumber = bonNumber.substring(0, bonNumber.indexOf('-'));
        }

        return {
          numero: index + 1,
          transporteur: 'SFB PETROLEUM SA',
          vehicule: voyage.camionImmatriculation || 'N/A',
          capacite: voyage.quantite || 45000, // Utiliser la quantité du voyage ou 45000 par défaut
          axe: voyage.destination || voyage.lieuDepart || '',
          bonEnlevement: bonNumber || 'N/A'
        };
      });

      // Générer le fichier Excel
      this.excelService.generateListeCamions({
        camions: camionsData,
        depotNom: depotNom,
        date: new Date()
      });

      this.toastService.success(`Liste Excel générée pour le dépôt: ${depotNom}`);
    }

    // Recharger les voyages pour avoir les numéros mis à jour
    this.loadVoyages();
  }
}
