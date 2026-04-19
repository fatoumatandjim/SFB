import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { ComptesBancairesService, CompteBancaire, BanqueCaisseStats } from '../../services/comptes-bancaires.service';
import { TransactionsService, Transaction as TransactionAPI, VirementRequest, TransactionPage } from '../../services/transactions.service';
import { CaissesService, Caisse } from '../../services/caisses.service';
import { AlertService } from '../../nativeComp/alert/alert.service';
import { ToastService } from '../../nativeComp/toast/toast.service';
import { UtilisateursService, Utilisateur } from '../../services/utilisateurs.service';
import { AuthService } from '../../services/auth.service';
import { JustificatifsFinanciersPanelComponent } from '../justificatifs-financiers-panel/justificatifs-financiers-panel.component';
import { JUSTIFICATIF_OWNER_TRANSACTION } from '../../services/justificatifs-financiers.service';
import { ResponsablesComptablesMultiselectComponent } from './responsables-comptables-multiselect.component';

interface CompteBancaireDisplay {
  id?: number;
  nom?: string;
  banque: string;
  numero: string;
  solde: number;
  devise?: string;
  type: string;
  statut: string;
}

interface NewCaisseForm {
  nom: string;
  solde: number;
  statut: 'ACTIF' | 'FERME' | 'SUSPENDU';
  description?: string;
  responsableIds?: number[];
}

interface NewCompteBancaire {
  numero: string;
  type: 'BANQUE' | 'MOBILE_MONEY';
  solde: number;
  banque: string;
  numeroCompteBancaire?: string;
  statut: 'ACTIF' | 'FERME' | 'SUSPENDU';
  description?: string;
  responsableIds?: number[];
}

interface Transaction {
  id: string;
  date: string;
  type: 'entree' | 'sortie';
  categorie: string;
  description: string;
  montant: number;
  compte: string;
  reference: string;
  statut: 'valide' | 'en-attente' | 'rejete';
}

interface NewTransaction {
  type: 'VIREMENT_SORTANT' | 'VIREMENT_ENTRANT' | 'VIREMENT_SIMPLE' | 'DEPOT' | 'RETRAIT';
  montant: number;
  date: string;
  compteId?: number; // Compte destination
  compteSourceId?: number; // Compte source (pour virement)
  caisseId?: number; // Caisse (pour virement simple dans une caisse)
  statut: 'VALIDE';
  description?: string;
  reference?: string;
}

@Component({
  selector: 'app-banque-caisse',
  templateUrl: './banque-caisse.component.html',
  styleUrls: ['./banque-caisse.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    JustificatifsFinanciersPanelComponent,
    ResponsablesComptablesMultiselectComponent
  ]
})
export class BanqueCaisseComponent implements OnInit {
  readonly justificatifOwnerTransaction = JUSTIFICATIF_OWNER_TRANSACTION;

  activeTab: 'banque' | 'caisse' = 'banque';
  searchTerm: string = '';
  showAddBanqueModal: boolean = false;
  showAddCaisseModal: boolean = false;
  /** Admin : modifier uniquement les responsables (corps PUT complet depuis l’API). */
  showEditResponsablesModal: boolean = false;
  editResponsablesLoading: boolean = false;
  editResponsablesMode: 'banque' | 'caisse' | null = null;
  editResponsablesTitle: string = '';
  editResponsableIds: number[] = [];
  /** True une fois le GET terminé (le template ne peut pas lire le snapshot privé). */
  editResponsablesReady: boolean = false;
  private editFinanceEntitySnapshot: CompteBancaire | Caisse | null = null;
  showAddTransactionModal: boolean = false;
  showAllTransactionsModal: boolean = false;
  showDetailModal: boolean = false;
  selectedTransaction: TransactionAPI | null = null;
  isLoading: boolean = false;
  isLoadingCaisses: boolean = false;
  isLoadingTransactions: boolean = false;

  // Transactions récentes
  transactionsRecentes: any[] = [];

  // Pagination
  transactionsPage: TransactionPage | null = null;
  currentPage: number = 0;
  pageSize: number = 10;

  // Filtres
  filterType: 'all' | 'date' | 'range' = 'all';
  filterDate: string = '';
  filterStartDate: string = '';
  filterEndDate: string = '';

  stats: BanqueCaisseStats = {
    soldeTotal: {
      montant: 0,
      evolution: '0%',
      periode: 'vs mois dernier'
    },
    soldeCaisse: {
      montant: 0,
      entrees: 0,
      sorties: 0,
      date: "Arrêté aujourd'hui"
    },
    comptesBancaires: {
      total: 0,
      actifs: 0
    },
    totalEntrees: {
      montant: 0
    },
    totalSorties: {
      montant: 0
    }
  };

  comptesBancaires: CompteBancaireDisplay[] = [];
  caisses: Caisse[] = [];
  caissePrincipaleId: number | undefined;

  /** Comptables éligibles comme responsables banque/caisse (chargé si admin). */
  comptablesPourResponsable: Utilisateur[] = [];

  newCaisse: Partial<NewCaisseForm> = {
    nom: '',
    solde: 0,
    statut: 'ACTIF',
    description: '',
    responsableIds: []
  };

  newCompteBancaire: Partial<NewCompteBancaire> = {
    numero: '',
    type: 'BANQUE',
    solde: 0,
    banque: '',
    numeroCompteBancaire: '',
    statut: 'ACTIF',
    description: '',
    responsableIds: []
  };

  newTransaction: Partial<NewTransaction> = {
    type: 'VIREMENT_SORTANT',
    montant: 0,
    date: new Date().toISOString().split('T')[0],
    statut: 'VALIDE',
    description: '',
    reference: '',
    compteId: undefined,
    compteSourceId: undefined
  };

  transactions: Transaction[] = [];

  constructor(
    private comptesBancairesService: ComptesBancairesService,
    private transactionsService: TransactionsService,
    private caissesService: CaissesService,
    private utilisateursService: UtilisateursService,
    private authService: AuthService,
    private alertService: AlertService,
    private toastService: ToastService
  ) { }

  isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  /** Comparaison insensible à la casse (ex. ACTIF / actif). */
  private isStatutActif(statut: string | undefined | null): boolean {
    return (statut ?? '').trim().toLowerCase() === 'actif';
  }

  ngOnInit() {
    this.loadComptesBancaires();
    this.loadStats();
    this.loadCaisses();
    if (this.isAdmin()) {
      this.utilisateursService.getComptablesActifs().subscribe({
        next: (list) => (this.comptablesPourResponsable = list || []),
        error: () => (this.comptablesPourResponsable = [])
      });
    }
  }

  loadStats() {
    this.comptesBancairesService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des statistiques:', error);
      }
    });
  }

  /**
   * Charge les caisses (entité Caisse) et l’id de la caisse principale pour les formulaires de transaction.
   */
  loadCaisses() {
    this.isLoadingCaisses = true;
    this.caissesService.getAllCaisses().subscribe({
      next: (caisses: Caisse[]) => {
        this.caisses = caisses;
        const principale = caisses.find((c) => c.nom === 'Caisse Principale');
        const active = caisses.find((c) => this.isStatutActif(c.statut));
        this.caissePrincipaleId = principale?.id ?? active?.id;
        this.isLoadingCaisses = false;
        this.loadTransactionsRecentes();
      },
      error: (err: unknown) => {
        console.error('Erreur lors du chargement des caisses:', err);
        this.isLoadingCaisses = false;
        this.caissesService.getCaisseByNom('Caisse Principale').subscribe({
          next: (caisse) => {
            this.caissePrincipaleId = caisse.id;
            this.caisses = [caisse];
          },
          error: () => {
            /* ignore */
          }
        });
        this.loadTransactionsRecentes();
      }
    });
  }

  loadComptesBancaires() {
    this.isLoading = true;
    this.comptesBancairesService.getAllComptes().subscribe({
      next: (data) => {
        this.comptesBancaires = data.map(compte => ({
          id: compte.id,
          banque: compte.banque,
          numero: compte.numero,
          solde: compte.solde,
          type: this.getTypeLabel(compte.type),
          statut: compte.statut,
          nom: compte.banque + ' - ' + compte.numero,
          devise: 'FCFA'
        }));
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des comptes bancaires:', error);
        this.isLoading = false;
      }
    });
  }

  getTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'BANQUE': 'Banque',
      'MOBILE_MONEY': 'Mobile Money'
    };
    return labels[type] || type;
  }

  get filteredTransactions(): Transaction[] {
    let filtered = this.transactions;

    if (this.activeTab === 'banque') {
      filtered = filtered.filter(t => t.compte !== 'Caisse');
    } else {
      filtered = filtered.filter(t => t.compte === 'Caisse');
    }

    if (this.searchTerm) {
      filtered = filtered.filter(t =>
        t.description.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        t.reference.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        t.categorie.toLowerCase().includes(this.searchTerm.toLowerCase())
      );
    }

    return filtered;
  }

  get totalEntrees(): number {
    return this.stats.totalEntrees.montant;
  }

  get totalSorties(): number {
    return this.stats.totalSorties.montant;
  }

  setTab(tab: 'banque' | 'caisse') {
    this.activeTab = tab;
    // Recharger les transactions récentes selon le nouvel onglet
    this.loadTransactionsRecentes();
  }

  nouvelleTransaction() {
    this.newTransaction = {
      type: 'VIREMENT_SORTANT',
      montant: 0,
      date: new Date().toISOString().split('T')[0],
      statut: 'VALIDE',
      description: '',
      reference: '',
      compteId: undefined,
      compteSourceId: undefined,
      caisseId: undefined
    };
    this.showAddTransactionModal = true;
  }

  closeAddTransactionModal() {
    this.showAddTransactionModal = false;
    this.newTransaction = {
      type: 'VIREMENT_SORTANT',
      montant: 0,
      date: new Date().toISOString().split('T')[0],
      statut: 'VALIDE',
      description: '',
      reference: '',
      compteId: undefined,
      compteSourceId: undefined,
      caisseId: undefined
    };
  }

  getCaissesDisponibles(): Caisse[] {
    return this.caisses.filter((c) => this.isStatutActif(c.statut));
  }

  saveTransaction() {
    if (!this.validateTransaction()) {
      return;
    }

    this.isLoading = true;

    let virementRequest: VirementRequest;

    if (this.newTransaction.type === 'VIREMENT_SORTANT') {
      // VIREMENT ENTRE DEUX COMPTES BANCAIRES
      if (!this.newTransaction.compteSourceId || !this.newTransaction.compteId) {
        this.toastService.warning('Veuillez sélectionner le compte source et le compte destination');
        this.isLoading = false;
        return;
      }

      virementRequest = {
        type: 'VIREMENT',
        montant: this.newTransaction.montant!,
        date: new Date(this.newTransaction.date!).toISOString(),
        statut: this.newTransaction.statut!,
        description: this.newTransaction.description || '',
        reference: this.newTransaction.reference || '',
        compteSourceId: this.newTransaction.compteSourceId,
        compteDestinationId: this.newTransaction.compteId
      };

    } else if (this.newTransaction.type === 'DEPOT') {
      // DÉPÔT : CAISSE VERS BANQUE
      if (!this.caissePrincipaleId || !this.newTransaction.compteId) {
        this.toastService.error('Erreur: Caisse principale non trouvée ou compte bancaire non sélectionné');
        this.isLoading = false;
        return;
      }

      virementRequest = {
        type: 'DEPOT',
        montant: this.newTransaction.montant!,
        date: new Date(this.newTransaction.date!).toISOString(),
        statut: this.newTransaction.statut!,
        description: this.newTransaction.description || '',
        reference: this.newTransaction.reference || '',
        caisseId: this.caissePrincipaleId,
        compteDestinationId: this.newTransaction.compteId
      };

    } else if (this.newTransaction.type === 'RETRAIT') {
      // RETRAIT : BANQUE VERS CAISSE
      if (!this.newTransaction.compteId || !this.caissePrincipaleId) {
        this.toastService.error('Erreur: Compte bancaire non sélectionné ou caisse principale non trouvée');
        this.isLoading = false;
        return;
      }

      virementRequest = {
        type: 'RETRAIT',
        montant: this.newTransaction.montant!,
        date: new Date(this.newTransaction.date!).toISOString(),
        statut: this.newTransaction.statut!,
        description: this.newTransaction.description || '',
        reference: this.newTransaction.reference || '',
        compteSourceId: this.newTransaction.compteId,
        caisseId: this.caissePrincipaleId
      };

    } else if (this.newTransaction.type === 'VIREMENT_SIMPLE') {
      // VIREMENT SIMPLE : une seule transaction, pas besoin de compte source
      // Peut concerner un compte bancaire ou une caisse
      virementRequest = {
        type: 'VIREMENT_SIMPLE',
        montant: this.newTransaction.montant!,
        date: new Date(this.newTransaction.date!).toISOString(),
        statut: this.newTransaction.statut!,
        description: this.newTransaction.description || '',
        reference: this.newTransaction.reference || '',
        compteDestinationId: this.newTransaction.compteId || undefined,
        caisseId: this.newTransaction.caisseId || undefined
      };
    } else {
      this.toastService.warning('Type de transaction non supporté');
      this.isLoading = false;
      return;
    }

    this.transactionsService.createVirement(virementRequest).subscribe({
      next: (transactions) => {
        this.isLoading = false;
        this.toastService.success('Transaction créée avec succès! Les soldes ont été mis à jour.');
        this.closeAddTransactionModal();
        // Recharger les comptes bancaires et les stats pour afficher les nouveaux soldes
        this.loadComptesBancaires();
        this.loadStats();
        this.loadCaisses();
      },
      error: (error) => {
        console.error('Erreur lors de la création de la transaction:', error);
        this.isLoading = false;
        const errorMessage = error.error?.message || error.message || 'Erreur lors de la création de la transaction';
        this.toastService.error('Erreur: ' + errorMessage);
      }
    });
  }

  validateTransaction(): boolean {
    if (!this.newTransaction.type) {
      this.toastService.warning('Veuillez sélectionner un type de transaction');
      return false;
    }
    if (!this.newTransaction.montant || this.newTransaction.montant <= 0) {
      this.toastService.warning('Veuillez saisir un montant valide');
      return false;
    }
    if (!this.newTransaction.date) {
      this.toastService.warning('Veuillez saisir une date');
      return false;
    }
    if (!this.newTransaction.statut) {
      this.toastService.warning('Veuillez sélectionner un statut');
      return false;
    }
    return true;
  }

  getCompteNom(compteId: number | undefined): string {
    if (!compteId) return '';
    const compte = this.comptesBancaires.find(c => c.id === compteId);
    return compte ? (compte.nom || `${compte.banque} - ${compte.numero}`) : '';
  }

  getComptesDisponibles(): CompteBancaireDisplay[] {
    return this.comptesBancaires.filter((c) => this.isStatutActif(c.statut));
  }

  getComptesSource(): CompteBancaireDisplay[] {
    // Pour virement, on peut transférer depuis n'importe quel compte actif
    return this.getComptesDisponibles();
  }

  getComptesDestination(): CompteBancaireDisplay[] {
    // Pour virement, on peut transférer vers n'importe quel compte actif sauf le compte source
    if (this.newTransaction.compteSourceId) {
      return this.getComptesDisponibles().filter(c => c.id !== this.newTransaction.compteSourceId);
    }
    return this.getComptesDisponibles();
  }

  getTransactionTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'DEPOT': 'Dépôt',
      'RETRAIT': 'Retrait',
      'VIREMENT_ENTRANT': 'Virement entrant',
      'VIREMENT_SORTANT': 'Virement sortant',
      'VIREMENT_SIMPLE': 'Virement simple',
      'FRAIS': 'Frais',
      'INTERET': 'Intérêt',
      'FRAIS_LOCATION': 'Frais de location',
      'FRAIS_FRONTIERE': 'Frais frontière',
      'TS_FRAIS_PRESTATIONS': 'TS Frais de prestations',
      'FRAIS_REPERTOIRE': 'Frais répertoire',
      'FRAIS_CHAMBRE_COMMERCE': 'Frais chambre de commerce',
      'SALAIRE': 'Salaire',
      'FRAIS_DOUANE': 'Frais douane',
      'FRAIS_T1': 'Frais T1'
    };
    return labels[type] || type;
  }

  viewTransaction(transaction: Transaction) {
    // Récupérer la transaction complète depuis l'API
    const transactionId = transaction.id;
    if (transactionId) {
      this.transactionsService.getTransactionById(parseInt(transactionId)).subscribe({
        next: (transactionAPI: TransactionAPI) => {
          this.selectedTransaction = transactionAPI;
          this.showDetailModal = true;
        },
        error: (error) => {
          console.error('Erreur lors du chargement de la transaction:', error);
          // Si l'API échoue, utiliser les données de la transaction affichée
          // Convertir la transaction Display en TransactionAPI approximative
          this.selectedTransaction = {
            id: parseInt(transaction.id),
            type: this.getTransactionTypeFromDisplay(transaction.type),
            montant: transaction.montant,
            date: transaction.date,
            statut: this.getStatutFromDisplay(transaction.statut),
            description: transaction.description,
            reference: transaction.reference,
            compteId: undefined,
            caisseId: undefined
          } as TransactionAPI;
          this.showDetailModal = true;
        }
      });
    }
  }

  viewTransactionFromPage(transaction: TransactionAPI) {
    // Récupérer la transaction complète depuis l'API
    const transactionId = transaction.id;
    if (transactionId) {
      this.transactionsService.getTransactionById(transactionId).subscribe({
        next: (transactionAPI: TransactionAPI) => {
          this.selectedTransaction = transactionAPI;
          this.showDetailModal = true;
        },
        error: (error) => {
          console.error('Erreur lors du chargement de la transaction:', error);
          // Si l'API échoue, utiliser directement la transaction de la page
          this.selectedTransaction = transaction;
          this.showDetailModal = true;
        }
      });
    } else {
      // Si pas d'ID, utiliser directement la transaction de la page
      this.selectedTransaction = transaction;
      this.showDetailModal = true;
    }
  }

  closeDetailModal() {
    this.showDetailModal = false;
    this.selectedTransaction = null;
  }

  private getTransactionTypeFromDisplay(type: string): string {
    if (type === 'entree') {
      return 'VIREMENT_ENTRANT';
    } else if (type === 'sortie') {
      return 'VIREMENT_SORTANT';
    }
    return type.toUpperCase();
  }

  private getStatutFromDisplay(statut: string): string {
    const mapping: { [key: string]: string } = {
      'valide': 'VALIDE',
      'en-attente': 'EN_ATTENTE',
      'rejete': 'REJETE',
      'annule': 'ANNULE'
    };
    return mapping[statut] || statut.toUpperCase();
  }

  getStatutLabel(statut: string): string {
    const labels: { [key: string]: string } = {
      'VALIDE': 'Validé',
      'EN_ATTENTE': 'En attente',
      'REJETE': 'Rejeté',
      'ANNULE': 'Annulé'
    };
    return labels[statut] || statut;
  }

  getStatutClass(statut: string): string {
    const classes: { [key: string]: string } = {
      'VALIDE': 'badge-green',
      'EN_ATTENTE': 'badge-orange',
      'REJETE': 'badge-red',
      'ANNULE': 'badge-grey'
    };
    return classes[statut] || 'badge-grey';
  }

  editTransaction(transaction: Transaction) {
  }

  closeEditResponsablesModal(): void {
    this.showEditResponsablesModal = false;
    this.editResponsablesLoading = false;
    this.editResponsablesReady = false;
    this.editResponsablesMode = null;
    this.editFinanceEntitySnapshot = null;
    this.editResponsablesTitle = '';
    this.editResponsableIds = [];
  }

  openEditResponsablesBanque(compteId: number | undefined): void {
    if (!compteId) {
      return;
    }
    if (!this.isAdmin()) {
      this.toastService.warning('Seuls les administrateurs peuvent modifier les responsables.');
      return;
    }
    this.editResponsablesMode = 'banque';
    this.editFinanceEntitySnapshot = null;
    this.editResponsablesReady = false;
    this.editResponsablesTitle = '';
    this.editResponsableIds = [];
    this.showEditResponsablesModal = true;
    this.editResponsablesLoading = true;
    this.comptesBancairesService.getCompteById(compteId).subscribe({
      next: (c) => {
        this.editFinanceEntitySnapshot = { ...c };
        this.editResponsablesTitle = `${c.banque} — ${c.numero}`;
        this.editResponsableIds = [...(c.responsableIds ?? [])];
        this.editResponsablesReady = true;
        this.editResponsablesLoading = false;
      },
      error: (err: any) => {
        this.editResponsablesLoading = false;
        this.closeEditResponsablesModal();
        this.toastService.error(
          err?.error?.message || err?.message || 'Impossible de charger le compte bancaire.'
        );
      }
    });
  }

  openEditResponsablesCaisse(caisseId: number | undefined): void {
    if (!caisseId) {
      return;
    }
    if (!this.isAdmin()) {
      this.toastService.warning('Seuls les administrateurs peuvent modifier les responsables.');
      return;
    }
    this.editResponsablesMode = 'caisse';
    this.editFinanceEntitySnapshot = null;
    this.editResponsablesReady = false;
    this.editResponsablesTitle = '';
    this.editResponsableIds = [];
    this.showEditResponsablesModal = true;
    this.editResponsablesLoading = true;
    this.caissesService.getCaisseById(caisseId).subscribe({
      next: (c) => {
        this.editFinanceEntitySnapshot = { ...c };
        this.editResponsablesTitle = c.nom;
        this.editResponsableIds = [...(c.responsableIds ?? [])];
        this.editResponsablesReady = true;
        this.editResponsablesLoading = false;
      },
      error: (err: any) => {
        this.editResponsablesLoading = false;
        this.closeEditResponsablesModal();
        this.toastService.error(err?.error?.message || err?.message || 'Impossible de charger la caisse.');
      }
    });
  }

  saveEditResponsables(): void {
    if (!this.editFinanceEntitySnapshot || !this.editResponsablesMode) {
      return;
    }
    const ids = this.editResponsableIds.filter((id) => id != null && id > 0);
    if (ids.length === 0) {
      this.toastService.warning('Sélectionnez au moins un responsable comptable.');
      return;
    }
    this.editResponsablesLoading = true;
    if (this.editResponsablesMode === 'banque') {
      const b = this.editFinanceEntitySnapshot as CompteBancaire;
      const payload: CompteBancaire = { ...b, responsableIds: ids };
      this.comptesBancairesService.updateCompte(b.id!, payload).subscribe({
        next: () => {
          this.editResponsablesLoading = false;
          this.toastService.success('Responsables du compte mis à jour.');
          this.closeEditResponsablesModal();
          this.loadComptesBancaires();
        },
        error: (err: any) => {
          this.editResponsablesLoading = false;
          this.toastService.error(
            err?.error?.message || err?.message || 'Erreur lors de la mise à jour des responsables.'
          );
        }
      });
      return;
    }
    const c = this.editFinanceEntitySnapshot as Caisse;
    const payload: Caisse = { ...c, responsableIds: ids };
    this.caissesService.updateCaisse(c.id!, payload).subscribe({
      next: () => {
        this.editResponsablesLoading = false;
        this.toastService.success('Responsables de la caisse mis à jour.');
        this.closeEditResponsablesModal();
        this.loadCaisses();
      },
      error: (err: any) => {
        this.editResponsablesLoading = false;
        this.toastService.error(
          err?.error?.message || err?.message || 'Erreur lors de la mise à jour des responsables.'
        );
      }
    });
  }

  nouvelleCaisse() {
    if (!this.isAdmin()) {
      this.toastService.warning('Seuls les administrateurs peuvent créer une caisse.');
      return;
    }
    this.newCaisse = {
      nom: '',
      solde: 0,
      statut: 'ACTIF',
      description: '',
      responsableIds: []
    };
    this.showAddCaisseModal = true;
  }

  closeAddCaisseModal() {
    this.showAddCaisseModal = false;
    this.newCaisse = {
      nom: '',
      solde: 0,
      statut: 'ACTIF',
      description: '',
      responsableIds: []
    };
  }

  saveCaisse() {
    const nom = (this.newCaisse.nom || '').trim();
    if (!nom) {
      this.toastService.warning('Veuillez saisir un nom de caisse (ex. Caisse 1)');
      return;
    }
    if (this.newCaisse.solde === undefined || this.newCaisse.solde < 0) {
      this.toastService.warning('Veuillez saisir un solde initial valide');
      return;
    }
    const resp = (this.newCaisse.responsableIds || []).filter((id: number) => id != null && id > 0);
    if (resp.length === 0) {
      this.toastService.warning('Sélectionnez au moins un responsable comptable.');
      return;
    }
    const payload: Caisse = {
      nom,
      solde: this.newCaisse.solde!,
      statut: this.newCaisse.statut!,
      description: this.newCaisse.description?.trim() || undefined,
      responsableIds: resp
    };
    this.isLoading = true;
    this.caissesService.createCaisse(payload).subscribe({
      next: () => {
        this.isLoading = false;
        this.toastService.success('Caisse créée avec succès');
        this.closeAddCaisseModal();
        this.loadCaisses();
        this.loadStats();
      },
      error: (err: any) => {
        console.error(err);
        this.isLoading = false;
        this.toastService.error(err?.error?.message || err?.message || 'Erreur lors de la création de la caisse');
      }
    });
  }

  nouvelleBanque() {
    if (!this.isAdmin()) {
      this.toastService.warning('Seuls les administrateurs peuvent créer un compte bancaire.');
      return;
    }
    this.newCompteBancaire = {
      numero: '',
      type: 'BANQUE',
      solde: 0,
      banque: '',
      numeroCompteBancaire: '',
      statut: 'ACTIF',
      description: '',
      responsableIds: []
    };
    this.showAddBanqueModal = true;
  }

  closeAddBanqueModal() {
    this.showAddBanqueModal = false;
    this.newCompteBancaire = {
      numero: '',
      type: 'BANQUE',
      solde: 0,
      banque: '',
      numeroCompteBancaire: '',
      statut: 'ACTIF',
      description: '',
      responsableIds: []
    };
  }

  saveBanque() {
    if (!this.validateBanque()) {
      return;
    }

    const resp = (this.newCompteBancaire.responsableIds || []).filter((id) => id != null && id > 0);
    if (resp.length === 0) {
      this.toastService.warning('Sélectionnez au moins un responsable comptable.');
      return;
    }
    this.isLoading = true;
    const compteToSave: CompteBancaire = {
      numero: this.newCompteBancaire.numero!,
      type: this.newCompteBancaire.type!,
      solde: this.newCompteBancaire.solde!,
      banque: this.newCompteBancaire.banque!,
      numeroCompteBancaire: this.newCompteBancaire.numeroCompteBancaire,
      statut: this.newCompteBancaire.statut!,
      description: this.newCompteBancaire.description,
      responsableIds: resp
    };

    this.comptesBancairesService.createCompte(compteToSave).subscribe({
      next: () => {
        this.isLoading = false;
        this.toastService.success('Compte bancaire créé avec succès!');
        this.closeAddBanqueModal();
        this.loadComptesBancaires();
        this.loadStats();
      },
      error: (error: any) => {
        console.error('Erreur lors de la création du compte bancaire:', error);
        this.isLoading = false;
        this.toastService.error(
          error?.error?.message || error?.message || 'Erreur lors de la création du compte bancaire. Veuillez réessayer.'
        );
      }
    });
  }

  validateBanque(): boolean {
    if (!this.newCompteBancaire.numero || this.newCompteBancaire.numero.trim() === '') {
      this.toastService.warning('Veuillez saisir un numéro de compte');
      return false;
    }
    if (!this.newCompteBancaire.banque || this.newCompteBancaire.banque.trim() === '') {
      this.toastService.warning('Veuillez saisir le nom de la banque');
      return false;
    }
    if (this.newCompteBancaire.solde === undefined || this.newCompteBancaire.solde < 0) {
      this.toastService.warning('Veuillez saisir un solde valide');
      return false;
    }
    return true;
  }

  loadTransactionsRecentes() {
    this.isLoadingTransactions = true;
    // Charger les transactions récentes filtrées selon l'onglet actif
    if (this.activeTab === 'banque') {
      // Charger uniquement les transactions des comptes bancaires
      this.transactionsService.getTransactionsByComptesBancairesOnly(0, 10).subscribe({
        next: (data: TransactionPage) => {
          if (data && data.transactions && data.transactions.length > 0) {
            this.transactionsRecentes = data.transactions.map((t: TransactionAPI) => this.mapTransactionForDisplay(t));
          } else {
            this.transactionsRecentes = [];
          }
          this.isLoadingTransactions = false;
        },
        error: (error: any) => {
          console.error('Erreur lors du chargement des transactions récentes:', error);
          this.isLoadingTransactions = false;
          this.transactionsRecentes = [];
        }
      });
    } else {
      // Charger uniquement les transactions des caisses
      this.transactionsService.getTransactionsByCaissesOnly(0, 10).subscribe({
        next: (data: TransactionPage) => {
          if (data && data.transactions && data.transactions.length > 0) {
            this.transactionsRecentes = data.transactions.map((t: TransactionAPI) => this.mapTransactionForDisplay(t));
          } else {
            this.transactionsRecentes = [];
          }
          this.isLoadingTransactions = false;
        },
        error: (error: any) => {
          console.error('Erreur lors du chargement des transactions récentes:', error);
          this.isLoadingTransactions = false;
          this.transactionsRecentes = [];
        }
      });
    }
  }

  voirTousTransactions() {
    this.showAllTransactionsModal = true;
    this.currentPage = 0;
    this.filterType = 'all';
    this.loadTransactionsPaginated();
  }

  closeAllTransactionsModal() {
    this.showAllTransactionsModal = false;
    this.transactionsPage = null;
    this.currentPage = 0;
    this.filterDate = '';
    this.filterStartDate = '';
    this.filterEndDate = '';
    this.filterType = 'all';
  }

  loadTransactionsPaginated() {
    this.isLoadingTransactions = true;
    let request: Observable<TransactionPage>;

    // Filtrer selon l'onglet actif (banque ou caisse)
    if (this.activeTab === 'banque') {
      // Filtrer uniquement les transactions des comptes bancaires
      if (this.filterType === 'date' && this.filterDate) {
        // Pour les comptes bancaires, on ne peut pas filtrer par date sans compte spécifique
        // On utilise donc l'endpoint général avec filtre banque uniquement
        request = this.transactionsService.getTransactionsByComptesBancairesOnly(
          this.currentPage,
          this.pageSize
        );
      } else if (this.filterType === 'range' && this.filterStartDate && this.filterEndDate) {
        // Même chose pour la plage de dates
        request = this.transactionsService.getTransactionsByComptesBancairesOnly(
          this.currentPage,
          this.pageSize
        );
      } else {
        request = this.transactionsService.getTransactionsByComptesBancairesOnly(
          this.currentPage,
          this.pageSize
        );
      }
    } else {
      // Filtrer uniquement les transactions des caisses
      if (this.filterType === 'date' && this.filterDate) {
        // Pour les caisses, on ne peut pas filtrer par date sans caisse spécifique
        request = this.transactionsService.getTransactionsByCaissesOnly(
          this.currentPage,
          this.pageSize
        );
      } else if (this.filterType === 'range' && this.filterStartDate && this.filterEndDate) {
        request = this.transactionsService.getTransactionsByCaissesOnly(
          this.currentPage,
          this.pageSize
        );
      } else {
        request = this.transactionsService.getTransactionsByCaissesOnly(
          this.currentPage,
          this.pageSize
        );
      }
    }

    request.subscribe({
      next: (data: TransactionPage) => {
        this.transactionsPage = data;
        // Mapper les transactions pour l'affichage avec les propriétés categorie et compte
        this.transactionsPage.transactions = data.transactions.map((t: TransactionAPI) => this.mapTransactionForDisplay(t)) as any[];
        this.isLoadingTransactions = false;
      },
      error: (error: any) => {
        console.error('Erreur lors du chargement des transactions:', error);
        this.isLoadingTransactions = false;
      }
    });
  }

  applyFilter() {
    this.currentPage = 0;
    this.loadTransactionsPaginated();
  }

  changePage(page: number) {
    this.currentPage = page;
    this.loadTransactionsPaginated();
  }

  mapTransactionForDisplay(transaction: TransactionAPI): any {
    let compteNom = 'N/A';
    if (transaction.compteId) {
      compteNom = this.getCompteNom(transaction.compteId);
    } else if (transaction.caisseId) {
      const caisse = this.caisses.find((c) => c.id === transaction.caisseId);
      compteNom = caisse ? caisse.nom : 'Caisse inconnue';
    }

    return {
      ...transaction,
      compte: compteNom,
      categorie: this.getTransactionCategory(transaction.type),
      type: this.getTransactionTypeForDisplay(transaction.type, transaction)
    };
  }

  getTransactionCategory(type: string): string {
    switch (type) {
      case 'DEPOT': return 'Dépôt';
      case 'RETRAIT': return 'Retrait';
      case 'VIREMENT_ENTRANT': return 'Virement';
      case 'VIREMENT_SORTANT': return 'Virement';
      case 'FRAIS': return 'Frais';
      case 'INTERET': return 'Intérêt';
      case 'FRAIS_LOCATION': return 'Frais de location';
      case 'FRAIS_FRONTIERE': return 'Frais frontière';
      case 'TS_FRAIS_PRESTATIONS': return 'Frais prestations';
      case 'FRAIS_REPERTOIRE': return 'Frais répertoire';
      case 'FRAIS_CHAMBRE_COMMERCE': return 'Frais chambre de commerce';
      case 'SALAIRE': return 'Salaire';
      default: return 'Autre';
    }
  }

  getTransactionTypeForDisplay(type: string, transaction?: TransactionAPI): string {
    if (type === 'VIREMENT_ENTRANT' || type === 'DEPOT') {
      return 'entree';
    } else if (type === 'VIREMENT_SORTANT' || type === 'RETRAIT') {
      return 'sortie';
    } else if (type === 'VIREMENT_SIMPLE') {
      // VIREMENT_SIMPLE est une entrée si elle a un compteId ou caisseId (destination)
      // Sinon, c'est une sortie
      if (transaction && (transaction.compteId || transaction.caisseId)) {
        return 'entree';
      }
      return 'sortie';
    }
    // Pour les autres types (FRAIS, INTERET, etc.), on considère comme sortie par défaut
    // sauf si le montant est positif et qu'il y a un compteId/caisseId
    if (transaction && transaction.montant > 0 && (transaction.compteId || transaction.caisseId)) {
      return 'entree';
    }
    return 'sortie';
  }

  formatDate(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    if (date.toDateString() === today.toDateString()) {
      return "Aujourd'hui";
    } else if (date.toDateString() === yesterday.toDateString()) {
      return 'Hier';
    } else {
      return date.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
    }
  }

  formatTime(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }

  getTransactionCategorie(transaction: TransactionAPI): string {
    return this.getTransactionCategory(transaction.type);
  }

  getTransactionCompte(transaction: TransactionAPI): string {
    if (transaction.compteId) {
      return this.getCompteNom(transaction.compteId);
    } else if (transaction.caisseId) {
      const caisse = this.caisses.find((c) => c.id === transaction.caisseId);
      return caisse ? caisse.nom : 'Caisse inconnue';
    }
    return 'N/A';
  }
}
