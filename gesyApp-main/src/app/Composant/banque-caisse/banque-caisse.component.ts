import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { ComptesBancairesService, CompteBancaire, BanqueCaisseStats } from '../../services/comptes-bancaires.service';
import { TransactionsService, Transaction as TransactionAPI, VirementRequest, TransactionPage } from '../../services/transactions.service';
import { CaissesService } from '../../services/caisses.service';
import { AlertService } from '../../nativeComp/alert/alert.service';
import { ToastService } from '../../nativeComp/toast/toast.service';

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

interface NewCompteBancaire {
  numero: string;
  type: 'BANQUE' | 'CAISSE' | 'MOBILE_MONEY';
  solde: number;
  banque: string;
  numeroCompteBancaire?: string;
  statut: 'ACTIF' | 'FERME' | 'SUSPENDU';
  description?: string;
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
  imports: [CommonModule, FormsModule]
})
export class BanqueCaisseComponent implements OnInit {
  activeTab: 'banque' | 'caisse' = 'banque';
  searchTerm: string = '';
  showAddBanqueModal: boolean = false;
  showAddTransactionModal: boolean = false;
  showAllTransactionsModal: boolean = false;
  showDetailModal: boolean = false;
  selectedTransaction: TransactionAPI | null = null;
  isLoading: boolean = false;
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
  caisses: any[] = [];
  caissePrincipaleId: number | undefined;

  newCompteBancaire: Partial<NewCompteBancaire> = {
    numero: '',
    type: 'BANQUE',
    solde: 0,
    banque: '',
    numeroCompteBancaire: '',
    statut: 'ACTIF',
    description: ''
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
    private alertService: AlertService,
    private toastService: ToastService
  ) { }

  ngOnInit() {
    this.loadComptesBancaires();
    this.loadCaissePrincipale();
    this.loadStats();
    // Charger les caisses d'abord pour le mapping des transactions
    this.caissesService.getAllCaisses().subscribe({
      next: (caisses: any[]) => {
        this.caisses = caisses;
        // Charger les transactions récentes après avoir chargé les caisses
        this.loadTransactionsRecentes();
      },
      error: (err: any) => {
        console.error('Erreur lors du chargement des caisses:', err);
        // Charger quand même les transactions
        this.loadTransactionsRecentes();
      }
    });
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

  loadCaissePrincipale() {
    // Charger la caisse principale (créée automatiquement au démarrage)
    this.caissesService.getCaisseByNom('Caisse Principale').subscribe({
      next: (caisse) => {
        this.caissePrincipaleId = caisse.id;
        if (!this.caisses.find((c: any) => c.id === caisse.id)) {
          this.caisses.push(caisse);
        }
      },
      error: (error) => {
        console.error('Erreur lors du chargement de la caisse principale:', error);
        // Essayer de charger toutes les caisses et prendre la première active
        this.caissesService.getAllCaisses().subscribe({
          next: (caisses: any[]) => {
            this.caisses = caisses;
            const caisseActive = caisses.find((c: any) => c.statut === 'ACTIF');
            if (caisseActive) {
              this.caissePrincipaleId = caisseActive.id;
            }
          },
          error: (err) => {
            console.error('Erreur lors du chargement des caisses:', err);
          }
        });
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
      'CAISSE': 'Caisse',
      'MOBILE_MONEY': 'Mobile Money'
    };
    return labels[type] || type;
  }

  get soldeTotal(): number {
    return this.comptesBancaires.reduce((sum, compte) => sum + compte.solde, 0);
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

  getCaissesDisponibles(): any[] {
    return this.caisses.filter((c: any) => c.statut === 'ACTIF' || c.statut === 'actif');
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
    return this.comptesBancaires.filter(c => c.statut === 'ACTIF' || c.statut === 'actif');
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

  nouvelleBanque() {
    this.newCompteBancaire = {
      numero: '',
      type: 'BANQUE',
      solde: 0,
      banque: '',
      numeroCompteBancaire: '',
      statut: 'ACTIF',
      description: ''
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
      description: ''
    };
  }

  saveBanque() {
    if (!this.validateBanque()) {
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
      description: this.newCompteBancaire.description
    };

    this.comptesBancairesService.createCompte(compteToSave).subscribe({
      next: () => {
        this.isLoading = false;
        this.toastService.success('Compte bancaire créé avec succès!');
        this.closeAddBanqueModal();
        this.loadComptesBancaires();
        this.loadStats();
      },
      error: (error) => {
        console.error('Erreur lors de la création du compte bancaire:', error);
        this.isLoading = false;
        this.toastService.error('Erreur lors de la création du compte bancaire. Veuillez réessayer.');
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
      const caisse = this.caisses.find((c: any) => c.id === transaction.caisseId);
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
      const caisse = this.caisses.find((c: any) => c.id === transaction.caisseId);
      return caisse ? caisse.nom : 'Caisse inconnue';
    }
    return 'N/A';
  }
}
