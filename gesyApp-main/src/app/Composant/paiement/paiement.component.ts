import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { ComptesBancairesService, CompteBancaire } from '../../services/comptes-bancaires.service';
import { CaissesService, Caisse } from '../../services/caisses.service';
import { TransactionsService, Transaction, TransactionPage, TransactionStats, TransactionFilterResult } from '../../services/transactions.service';
import { PaiementService, Paiement as PaiementBackend } from '../../services/paiement.service';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import { AlertService } from '../../nativeComp/alert/alert.service';
import { ToastService } from '../../nativeComp/toast/toast.service';
import { PdfService } from '../../services/pdf.service';

interface Paiement {
  id: string;
  numero: string;
  facture: string;
  beneficiaire: {
    nom: string;
    email: string;
    initiales: string;
    couleur: string;
  };
  date: string;
  montant: number;
  methode: string;
  statut: 'effectue' | 'en-attente' | 'echec' | 'annule';
  reference: string;
}

@Component({
  selector: 'app-paiement',
  templateUrl: './paiement.component.html',
  styleUrls: ['./paiement.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class PaiementComponent implements OnInit {
  activeTab: 'transactions' | 'paiements-non-effectues' = 'transactions';
  activeFilter: string = 'tous';
  searchTerm: string = '';
  searchTermPaiements: string = '';
  showAddModal: boolean = false;
  showDetailModal: boolean = false;
  showPaiementModal: boolean = false;
  showDetailPaiementModal: boolean = false;
  selectedPaiement: Paiement | null = null;
  selectedPaiementToPay: PaiementBackend | null = null;
  selectedPaiementDetail: PaiementBackend | null = null;
  selectedTransaction: Transaction | null = null;
  isLoading: boolean = false;
  isLoadingTransactions: boolean = false;
  isLoadingPaiements: boolean = false;
  comptesBancaires: CompteBancaire[] = [];
  caisses: Caisse[] = [];
  paiementsNonEffectues: PaiementBackend[] = [];
  filteredPaiementsNonEffectues: PaiementBackend[] = [];
  selectedCompteId?: number;
  selectedCaisseId?: number;
  compteTypePaiement: 'banque' | 'caisse' = 'banque';

  newPaiement: Partial<Transaction> = {
    type: 'VIREMENT_SORTANT',
    montant: 0,
    date: new Date().toISOString().split('T')[0],
    statut: 'VALIDE',
    description: '',
    compteId: undefined,
    caisseId: undefined
  };

  compteType: 'banque' | 'caisse' = 'banque';

  stats: TransactionStats = {
    paiementsEffectues: {
      total: 0,
      montant: 0,
      periode: 'Ce mois',
      evolution: '0%'
    },
    paiementsEnAttente: {
      total: 0,
      montant: 0,
      pourcentage: '0%'
    },
    paiementsEchec: {
      total: 0,
      montant: 0,
      urgent: false
    }
  };

  transactionsPage: TransactionPage | null = null;
  currentPage: number = 0;
  pageSize: number = 10;
  filterType: 'tous' | 'date' | 'range' = 'tous';
  filterDate: string = '';
  filterStartDate: string = '';
  filterEndDate: string = '';

  // Filtre personnalisé (type + date / intervalle)
  customFilterActive: boolean = false;
  customFilterType: string = '';
  customFilterDate: string = '';
  customFilterStartDate: string = '';
  customFilterEndDate: string = '';
  customFilterDateMode: 'tous' | 'date' | 'range' = 'tous';
  filterResult: TransactionFilterResult | null = null;
  readonly transactionTypes: { value: string; label: string }[] = [
    { value: '', label: 'Tous les types' },
    { value: 'DEPOT', label: 'Dépôt' },
    { value: 'RETRAIT', label: 'Retrait' },
    { value: 'VIREMENT_ENTRANT', label: 'Virement entrant' },
    { value: 'VIREMENT_SORTANT', label: 'Virement sortant' },
    { value: 'VIREMENT_SIMPLE', label: 'Virement simple' },
    { value: 'FRAIS', label: 'Frais' },
    { value: 'INTERET', label: 'Intérêt' },
    { value: 'FRAIS_LOCATION', label: 'Frais de location' },
    { value: 'FRAIS_FRONTIERE', label: 'Frais frontière' },
    { value: 'TS_FRAIS_PRESTATIONS', label: 'Frais prestations' },
    { value: 'FRAIS_REPERTOIRE', label: 'Frais répertoire' },
    { value: 'FRAIS_CHAMBRE_COMMERCE', label: 'Frais chambre de commerce' },
    { value: 'SALAIRE', label: 'Salaire' },
    { value: 'FRAIS_DOUANE', label: 'Frais douane' },
    { value: 'FRAIS_T1', label: 'Frais T1' }
  ];

  paiements: Paiement[] = [
    {
      id: '1',
      numero: 'PAY-2024-0456',
      facture: 'INV-2024-0342',
      beneficiaire: {
        nom: 'Petrole SA',
        email: 'contact@petrole.ml',
        initiales: 'PS',
        couleur: 'blue'
      },
      date: '15 Mars 2024',
      montant: 450000,
      methode: 'Virement bancaire',
      statut: 'effectue',
      reference: 'VIR-2024-0456'
    },
    {
      id: '2',
      numero: 'PAY-2024-0455',
      facture: 'INV-2024-0341',
      beneficiaire: {
        nom: 'Total Mali',
        email: 'admin@total.ml',
        initiales: 'TM',
        couleur: 'purple'
      },
      date: '14 Mars 2024',
      montant: 325000,
      methode: 'Chèque',
      statut: 'en-attente',
      reference: 'CHQ-2024-0455'
    },
    {
      id: '3',
      numero: 'PAY-2024-0454',
      facture: 'INV-2024-0340',
      beneficiaire: {
        nom: 'Shell Mali',
        email: 'info@shell.ml',
        initiales: 'SM',
        couleur: 'red'
      },
      date: '13 Mars 2024',
      montant: 280000,
      methode: 'Espèces',
      statut: 'effectue',
      reference: 'ESP-2024-0454'
    },
    {
      id: '4',
      numero: 'PAY-2024-0453',
      facture: 'INV-2024-0339',
      beneficiaire: {
        nom: 'Oryx Energy',
        email: 'contact@oryx.ml',
        initiales: 'OE',
        couleur: 'green'
      },
      date: '12 Mars 2024',
      montant: 520000,
      methode: 'Virement bancaire',
      statut: 'echec',
      reference: 'VIR-2024-0453'
    },
    {
      id: '5',
      numero: 'PAY-2024-0452',
      facture: 'INV-2024-0338',
      beneficiaire: {
        nom: 'Vivo Energy',
        email: 'admin@vivo.ml',
        initiales: 'VE',
        couleur: 'orange'
      },
      date: '11 Mars 2024',
      montant: 375000,
      methode: 'Carte bancaire',
      statut: 'effectue',
      reference: 'CART-2024-0452'
    },
    {
      id: '6',
      numero: 'PAY-2024-0451',
      facture: 'INV-2024-0337',
      beneficiaire: {
        nom: 'Engen Mali',
        email: 'contact@engen.ml',
        initiales: 'EM',
        couleur: 'teal'
      },
      date: '10 Mars 2024',
      montant: 290000,
      methode: 'Virement bancaire',
      statut: 'annule',
      reference: 'VIR-2024-0451'
    }
  ];

  constructor(
    private comptesBancairesService: ComptesBancairesService,
    private caissesService: CaissesService,
    private transactionsService: TransactionsService,
    private paiementService: PaiementService,
    private alertService: AlertService,
    private toastService: ToastService,
    private pdfService: PdfService
  ) { }

  ngOnInit() {
    this.loadComptesBancaires();
    this.loadCaisses();
    this.loadStats();
    this.loadTransactionsPaginated();
    this.loadPaiementsNonEffectues();
  }

  onTabChange(tab: 'transactions' | 'paiements-non-effectues') {
    this.activeTab = tab;
    if (tab === 'paiements-non-effectues') {
      this.loadPaiementsNonEffectues();
    }
  }

  loadPaiementsNonEffectues() {
    this.isLoadingPaiements = true;
    this.paiementService.getPaiementsByStatut('EN_ATTENTE').subscribe({
      next: (data: PaiementBackend[]) => {
        this.paiementsNonEffectues = data;
        this.updateFilteredPaiements();
        this.isLoadingPaiements = false;
      },
      error: (error: any) => {
        console.error('Erreur lors du chargement des paiements non effectués:', error);
        this.toastService.error('Erreur lors du chargement des paiements');
        this.isLoadingPaiements = false;
      }
    });
  }

  updateFilteredPaiements() {
    let filtered = this.paiementsNonEffectues;
    if (this.searchTermPaiements) {
      const term = this.searchTermPaiements.toLowerCase();
      filtered = filtered.filter(p =>
        (p.reference && p.reference.toLowerCase().includes(term)) ||
        (p.notes && p.notes.toLowerCase().includes(term)) ||
        (p.reference && p.reference.toLowerCase().includes(term))
      );
    }
    this.filteredPaiementsNonEffectues = filtered;
  }

  onSearchPaiementsChange() {
    this.updateFilteredPaiements();
  }

  openPaiementModal(paiement: PaiementBackend) {
    this.selectedPaiementToPay = paiement;
    this.selectedCompteId = undefined;
    this.selectedCaisseId = undefined;
    this.compteTypePaiement = 'banque';
    this.showPaiementModal = true;
  }

  closePaiementModal() {
    this.showPaiementModal = false;
    this.selectedPaiementToPay = null;
    this.selectedCompteId = undefined;
    this.selectedCaisseId = undefined;
  }

  viewPaiementDetail(paiement: PaiementBackend) {
    this.selectedPaiementDetail = paiement;
    this.showDetailPaiementModal = true;
  }

  closeDetailPaiementModal() {
    this.showDetailPaiementModal = false;
    this.selectedPaiementDetail = null;
  }

  validerPaiement() {
    if (!this.selectedPaiementToPay) {
      this.alertService.error('Erreur', 'Aucun paiement sélectionné');
      return;
    }

    // Vérifier qu'un compte ou une caisse est sélectionné
    if (this.compteTypePaiement === 'banque' && !this.selectedCompteId) {
      this.alertService.warning(
        'Compte requis',
        'Veuillez sélectionner un compte bancaire pour débiter le paiement.'
      );
      return;
    }

    if (this.compteTypePaiement === 'caisse' && !this.selectedCaisseId) {
      this.alertService.warning(
        'Caisse requise',
        'Veuillez sélectionner une caisse pour débiter le paiement.'
      );
      return;
    }

    // Confirmation avant validation
    this.alertService.confirm(
      `Êtes-vous sûr de vouloir valider ce paiement de ${this.selectedPaiementToPay.montant.toLocaleString()} FCFA ?`,
      'Confirmer la validation'
    ).subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.isLoading = true;
        this.paiementService.validerPaiement(
          Number(this.selectedPaiementToPay!.id),
          this.selectedCompteId,
          this.selectedCaisseId
        ).subscribe({
          next: () => {
            this.toastService.success('Paiement validé avec succès');
            this.closePaiementModal();
            this.loadPaiementsNonEffectues();
            this.loadStats();
            this.isLoading = false;
          },
          error: (error: any) => {
            console.error('Erreur lors de la validation du paiement:', error);
            const errorMessage = error?.error?.message ||
                               error?.error?.error ||
                               error?.message ||
                               'Erreur lors de la validation du paiement';
            this.alertService.error(errorMessage, 'Erreur de validation');
            this.isLoading = false;
          }
        });
      }
    });
  }

  loadComptesBancaires() {
    this.comptesBancairesService.getAllComptes().subscribe({
      next: (data) => {
        this.comptesBancaires = data.filter(c => c.statut === 'ACTIF');
      },
      error: (error) => {
        console.error('Erreur lors du chargement des comptes bancaires:', error);
      }
    });
  }

  loadCaisses() {
    this.caissesService.getAllCaisses().subscribe({
      next: (data) => {
        this.caisses = data.filter(c => c.statut === 'ACTIF');
      },
      error: (error) => {
        console.error('Erreur lors du chargement des caisses:', error);
      }
    });
  }

  loadStats() {
    this.transactionsService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des statistiques:', error);
      }
    });
  }

  loadTransactionsPaginated() {
    if (this.customFilterActive) {
      this.loadCustomFilter();
      return;
    }
    this.isLoadingTransactions = true;
    this.filterResult = null;

    let request: Observable<TransactionPage>;

    if (this.filterType === 'date' && this.filterDate) {
      request = this.transactionsService.getTransactionsByDate(this.filterDate, this.currentPage, this.pageSize);
    } else if (this.filterType === 'range' && this.filterStartDate && this.filterEndDate) {
      request = this.transactionsService.getTransactionsByDateRange(this.filterStartDate, this.filterEndDate, this.currentPage, this.pageSize);
    } else {
      request = this.transactionsService.getTransactionsPaginated(this.currentPage, this.pageSize);
    }

    request.subscribe({
      next: (data: TransactionPage) => {
        this.transactionsPage = data;
        this.isLoadingTransactions = false;
      },
      error: (error: any) => {
        console.error('Erreur lors du chargement des transactions:', error);
        this.isLoadingTransactions = false;
      }
    });
  }

  toggleCustomFilter() {
    this.customFilterActive = !this.customFilterActive;
    if (!this.customFilterActive) {
      this.filterResult = null;
      this.loadTransactionsPaginated();
    } else {
      this.applyCustomFilter();
    }
  }

  applyCustomFilter() {
    if (!this.customFilterActive) return;
    this.currentPage = 0;
    this.loadCustomFilter();
  }

  loadCustomFilter() {
    if (!this.customFilterActive) return;
    this.isLoadingTransactions = true;

    const params: { type?: string; date?: string; startDate?: string; endDate?: string; page: number; size: number } = {
      page: this.currentPage,
      size: this.pageSize
    };
    if (this.customFilterType) params.type = this.customFilterType;
    if (this.customFilterDateMode === 'date' && this.customFilterDate) params.date = this.customFilterDate;
    if (this.customFilterDateMode === 'range' && this.customFilterStartDate && this.customFilterEndDate) {
      params.startDate = this.customFilterStartDate;
      params.endDate = this.customFilterEndDate;
    }

    this.transactionsService.getTransactionsFilter(params).subscribe({
      next: (data: TransactionFilterResult) => {
        this.filterResult = data;
        this.transactionsPage = {
          transactions: data.transactions,
          currentPage: data.currentPage,
          totalPages: data.totalPages,
          totalElements: data.totalCount,
          size: data.size
        };
        this.isLoadingTransactions = false;
      },
      error: (error: any) => {
        console.error('Erreur lors du chargement du filtre personnalisé:', error);
        this.isLoadingTransactions = false;
      }
    });
  }

  changePageCustomFilter(page: number) {
    this.currentPage = page;
    if (this.customFilterActive) {
      this.loadCustomFilter();
    } else {
      this.loadTransactionsPaginated();
    }
  }

  onFilterChange() {
    this.currentPage = 0;
    this.loadTransactionsPaginated();
  }

  changePage(page: number) {
    this.currentPage = page;
    this.changePageCustomFilter(page);
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

  getStatutLabel(statut: string): string {
    const labels: { [key: string]: string } = {
      'VALIDE': 'Effectué',
      'EN_ATTENTE': 'En attente',
      'REJETE': 'Échec',
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

  getTransactionTypeLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'VIREMENT_SORTANT': 'Virement sortant',
      'VIREMENT_ENTRANT': 'Virement entrant',
      'VIREMENT_SIMPLE': 'Virement simple',
      'RETRAIT': 'Retrait',
      'DEPOT': 'Dépôt',
      'FRAIS': 'Frais',
      'FRAIS_LOCATION': 'Frais de location',
      'FRAIS_FRONTIERE': 'Frais frontière',
      'FRAIS_DOUANE': 'Frais douane',
      'FRAIS_T1': 'Frais T1',
      'TS_FRAIS_PRESTATIONS': 'Frais prestations',
      'FRAIS_REPERTOIRE': 'Frais répertoire',
      'FRAIS_CHAMBRE_COMMERCE': 'Frais chambre de commerce',
      'INTERET': 'Intérêt',
      'SALAIRE': 'Salaire'
    };
    return labels[type] || type;
  }

  setFilter(filter: string) {
    this.activeFilter = filter;
  }

  get filteredPaiements(): Paiement[] {
    // Utiliser les transactions paginées si disponibles
    if (this.transactionsPage && this.transactionsPage.transactions) {
      let transactions = this.transactionsPage.transactions;

      // Filtrer par recherche si nécessaire
      if (this.searchTerm) {
        const term = this.searchTerm.toLowerCase();
        transactions = transactions.filter(t =>
          (t.reference && t.reference.toLowerCase().includes(term)) ||
          (t.description && t.description.toLowerCase().includes(term)) ||
          (t.id && t.id.toString().includes(term))
        );
      }

      return transactions.map(t => this.mapTransactionToPaiement(t));
    }
    return [];
  }

  mapTransactionToPaiement(transaction: Transaction): Paiement {
    // Extraire les initiales du bénéficiaire ou de la description
    const beneficiaireNom = transaction.beneficiaire || transaction.description || 'N/A';
    const initiales = beneficiaireNom.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2) || '??';
    const couleurs = ['blue', 'purple', 'red', 'green', 'orange', 'teal', 'pink'];
    const hash = beneficiaireNom.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    const couleur = couleurs[hash % couleurs.length];

    return {
      id: transaction.id?.toString() || '',
      numero: transaction.reference || `TXN-${transaction.id}`,
      facture: transaction.factureId ? `INV-${transaction.factureId}` : 'N/A',
      beneficiaire: {
        nom: beneficiaireNom,
        email: '',
        initiales: initiales,
        couleur: couleur
      },
      date: this.formatDate(transaction.date),
      montant: transaction.montant || 0,
      methode: this.getTransactionTypeLabel(transaction.type),
      statut: this.mapStatutToPaiementStatut(transaction.statut),
      reference: transaction.reference || ''
    };
  }

  mapStatutToPaiementStatut(statut: string): 'effectue' | 'en-attente' | 'echec' | 'annule' {
    const mapping: { [key: string]: 'effectue' | 'en-attente' | 'echec' | 'annule' } = {
      'VALIDE': 'effectue',
      'EN_ATTENTE': 'en-attente',
      'REJETE': 'echec',
      'ANNULE': 'annule'
    };
    return mapping[statut] || 'en-attente';
  }

  getTransactionDescription(paiementId: string): string {
    if (!this.transactionsPage) return 'N/A';
    const transaction = this.transactionsPage.transactions.find(t => t.id?.toString() === paiementId);
    return transaction?.description || 'N/A';
  }

  getTransactionStatut(paiementId: string): string {
    if (!this.transactionsPage) return 'EN_ATTENTE';
    const transaction = this.transactionsPage.transactions.find(t => t.id?.toString() === paiementId);
    return transaction?.statut || 'EN_ATTENTE';
  }

  onSearchChange() {
    // La recherche est gérée côté client dans le getter filteredPaiements
    // Pas besoin de recharger les données
  }

  viewPaiement(paiement: Paiement) {
    // Trouver la transaction correspondante
    if (this.transactionsPage && this.transactionsPage.transactions) {
      const transaction = this.transactionsPage.transactions.find(t => t.id?.toString() === paiement.id);
      if (transaction) {
        this.selectedTransaction = transaction;
        this.selectedPaiement = paiement;
        this.showDetailModal = true;
      }
    }
  }

  closeDetailModal() {
    this.showDetailModal = false;
    this.selectedPaiement = null;
    this.selectedTransaction = null;
  }

  downloadPaiement(paiement: Paiement) {
    // Trouver la transaction correspondante
    if (this.transactionsPage && this.transactionsPage.transactions) {
      const transaction = this.transactionsPage.transactions.find(t => t.id?.toString() === paiement.id);
      if (transaction) {
        this.generateReçuPdf(paiement, transaction);
      }
    }
  }

  private generateReçuPdf(paiement: Paiement, transaction: Transaction) {
    const doc = new jsPDF('p', 'mm', 'a4');
    const marginLeft = 20;
    const pageWidth = doc.internal.pageSize.getWidth();
    const pageHeight = doc.internal.pageSize.getHeight();
    let y = 20;

    // ---------- En-tête entreprise ----------
    doc.setFontSize(18);
    doc.setFont('helvetica', 'bold');
    doc.text('SFB', marginLeft, y);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(10);
    y += 6;
    doc.text('Gestion logistique & facturation', marginLeft, y);
    y += 4;
    doc.text('Adresse: Torokorobougou immeuble Wad motors', marginLeft, y);
    y += 4;
    doc.text('Email: hyattassaye87@gmail.com', marginLeft, y);
    y += 4;
    doc.text('Téléphone: (+223) 70 90 28 28', marginLeft, y);

    // ---------- Titre "REÇU DE PAIEMENT" ----------
    y += 15;
    doc.setFontSize(16);
    doc.setFont('helvetica', 'bold');
    doc.text('REÇU DE PAIEMENT', pageWidth - marginLeft, y, { align: 'right' });

    // ---------- Informations du reçu (à droite) ----------
    y += 10;
    const infoX = pageWidth - 75;
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.text('N° Reçu:', infoX, y);
    doc.setFont('helvetica', 'bold');
    doc.text(paiement.numero || transaction.reference || `TXN-${transaction.id}`, pageWidth - marginLeft, y, { align: 'right' });

    y += 5;
    doc.setFont('helvetica', 'normal');
    doc.text('Date:', infoX, y);
    doc.setFont('helvetica', 'bold');
    doc.text(this.formatDate(transaction.date) || '-', pageWidth - marginLeft, y, { align: 'right' });

    y += 5;
    doc.setFont('helvetica', 'normal');
    doc.text('Statut:', infoX, y);
    doc.setFont('helvetica', 'bold');
    doc.text(this.getStatutLabel(transaction.statut || ''), pageWidth - marginLeft, y, { align: 'right' });

    // ---------- Informations du paiement ----------
    y += 15;
    doc.setFontSize(12);
    doc.setFont('helvetica', 'bold');
    doc.text('Détails du Paiement', marginLeft, y);
    y += 8;

    doc.setFont('helvetica', 'normal');
    doc.setFontSize(10);

    // Montant
    doc.setFont('helvetica', 'bold');
    doc.text('Montant:', marginLeft, y);
    doc.setFont('helvetica', 'normal');
    doc.text(`${this.formatMontant(transaction.montant || 0)} FCFA`, marginLeft + 40, y);
    y += 6;

    // Type de transaction
    doc.setFont('helvetica', 'bold');
    doc.text('Type:', marginLeft, y);
    doc.setFont('helvetica', 'normal');
    doc.text(this.getTransactionTypeLabel(transaction.type || ''), marginLeft + 40, y);
    y += 6;

    // Description
    if (transaction.description) {
      doc.setFont('helvetica', 'bold');
      doc.text('Description:', marginLeft, y);
      doc.setFont('helvetica', 'normal');
      const descriptionLines = doc.splitTextToSize(transaction.description, pageWidth - 2 * marginLeft - 40);
      doc.text(descriptionLines, marginLeft + 40, y);
      y += descriptionLines.length * 5;
    }

    // Compte/Caisse utilisé
    y += 6;
    doc.setFont('helvetica', 'bold');
    doc.text('Compte débité:', marginLeft, y);
    doc.setFont('helvetica', 'normal');
    let compteInfo = 'N/A';
    if (transaction.compteId) {
      const compte = this.comptesBancaires.find(c => c.id === transaction.compteId);
      if (compte) {
        compteInfo = `${compte.banque} - ${compte.numero}`;
      }
    } else if (transaction.caisseId) {
      const caisse = this.caisses.find(c => c.id === transaction.caisseId);
      if (caisse) {
        compteInfo = caisse.nom;
      }
    }
    doc.text(compteInfo, marginLeft + 40, y);
    y += 6;

    // Référence
    if (transaction.reference) {
      doc.setFont('helvetica', 'bold');
      doc.text('Référence:', marginLeft, y);
      doc.setFont('helvetica', 'normal');
      doc.text(transaction.reference, marginLeft + 40, y);
      y += 6;
    }

    // ---------- Ligne de séparation ----------
    y += 10;
    doc.setDrawColor(200, 200, 200);
    doc.line(marginLeft, y, pageWidth - marginLeft, y);
    y += 10;

    // ---------- Message de confirmation ----------
    doc.setFontSize(11);
    doc.setFont('helvetica', 'italic');
    doc.setTextColor(100, 100, 100);
    doc.text('Ce document certifie que le paiement ci-dessus a été effectué et enregistré.', marginLeft, y);
    y += 6;
    doc.text('Merci pour votre confiance.', marginLeft, y);

    // ---------- Pied de page ----------
    let footerY = pageHeight - 20;

    // Ligne de séparation
    doc.setDrawColor(200, 200, 200);
    doc.line(marginLeft, footerY, pageWidth - marginLeft, footerY);
    footerY += 5;

    // Informations du pied de page
    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(100, 100, 100);
    doc.text('Siège social: Torokorobougou immeuble Wad motors', marginLeft, footerY);
    footerY += 4;
    doc.text('RCCM: Ma bko 2024-M-3968', marginLeft, footerY);
    footerY += 4;
    doc.text('Email: hyattassaye87@gmail.com', marginLeft, footerY);
    footerY += 4;
    doc.setFontSize(7);
    doc.setTextColor(150, 150, 150);
    doc.text('SFB - Reçu généré automatiquement par le système de gestion de la société SFB', marginLeft, footerY);

    const fileName = `Recu_Paiement_${paiement.numero || transaction.reference || transaction.id}.pdf`;
    doc.save(fileName);
  }

  private formatMontant(value: number | undefined | null): string {
    const n = Math.round(value || 0);
    const str = n.toString();
    return str.replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
  }

  moreOptions(paiement: Paiement) {
  }

  nouveauPaiement() {
    this.newPaiement = {
      type: 'VIREMENT_SORTANT',
      montant: 0,
      date: new Date().toISOString().split('T')[0],
      statut: 'VALIDE',
      description: '',
      compteId: undefined,
      caisseId: undefined
    };
    this.compteType = 'banque';
    this.showAddModal = true;
  }

  onCompteTypeChange() {
    // Réinitialiser les sélections quand on change de type
    if (this.compteType === 'banque') {
      this.newPaiement.caisseId = undefined;
      this.newPaiement.type = 'VIREMENT_SORTANT';
    } else {
      this.newPaiement.compteId = undefined;
      this.newPaiement.type = 'RETRAIT';
    }
  }

  closeAddModal() {
    this.showAddModal = false;
    this.newPaiement = {
      type: 'VIREMENT_SORTANT',
      montant: 0,
      date: new Date().toISOString().split('T')[0],
      statut: 'VALIDE',
      description: '',
      compteId: undefined,
      caisseId: undefined
    };
    this.compteType = 'banque';
  }

  savePaiement() {
    if (!this.validatePaiement()) {
      return;
    }

    this.isLoading = true;

    // Déterminer le type de transaction selon le compte sélectionné
    let typeTransaction = 'VIREMENT_SORTANT';
    if (this.compteType === 'caisse') {
      typeTransaction = 'RETRAIT';
    }

    const transaction: Transaction = {
      type: typeTransaction,
      montant: this.newPaiement.montant || 0,
      date: new Date(this.newPaiement.date!).toISOString(),
      statut: 'VALIDE',
      description: this.newPaiement.description || '',
      compteId: (this.compteType === 'banque' && this.newPaiement.compteId && this.newPaiement.compteId > 0) ? this.newPaiement.compteId : undefined,
      caisseId: (this.compteType === 'caisse' && this.newPaiement.caisseId && this.newPaiement.caisseId > 0) ? this.newPaiement.caisseId : undefined
    };

    this.transactionsService.createPaiement(transaction).subscribe({
      next: () => {
        this.isLoading = false;
        this.toastService.success('Paiement créé avec succès!');
        this.closeAddModal();
        // Recharger les comptes et caisses pour mettre à jour les soldes
        this.loadComptesBancaires();
        this.loadCaisses();
      },
      error: (error) => {
        console.error('Erreur lors de la création du paiement:', error);
        this.isLoading = false;
        const errorMessage = error.error?.message || error.message || 'Erreur lors de la création du paiement. Veuillez réessayer.';
        this.toastService.error(errorMessage);
      }
    });
  }

  validatePaiement(): boolean {
    if (!this.newPaiement.montant || this.newPaiement.montant <= 0) {
      this.toastService.warning('Veuillez saisir un montant valide');
      return false;
    }
    if (!this.newPaiement.description || this.newPaiement.description.trim() === '') {
      this.toastService.warning('Veuillez saisir une description');
      return false;
    }
    if (!this.newPaiement.compteId && !this.newPaiement.caisseId) {
      this.toastService.warning('Veuillez sélectionner un compte bancaire ou une caisse');
      return false;
    }
    if (this.newPaiement.compteId && this.newPaiement.caisseId) {
      this.toastService.warning('Veuillez sélectionner soit un compte bancaire, soit une caisse, pas les deux');
      return false;
    }
    return true;
  }

  exportTransactionsToPdf() {
    this.isLoading = true;

    // Récupérer toutes les transactions selon les filtres (sans pagination)
    let request: Observable<Transaction[]>;

    if (this.filterType === 'date' && this.filterDate) {
      request = this.transactionsService.getTransactionsByDateAll(this.filterDate);
    } else if (this.filterType === 'range' && this.filterStartDate && this.filterEndDate) {
      request = this.transactionsService.getTransactionsByDateRangeAll(this.filterStartDate, this.filterEndDate);
    } else {
      request = this.transactionsService.getAllTransactions();
    }

    request.subscribe({
      next: (transactions: Transaction[]) => {
        this.isLoading = false;

        // Filtrer par recherche si nécessaire
        let filteredTransactions = transactions;
        if (this.searchTerm) {
          const term = this.searchTerm.toLowerCase();
          filteredTransactions = transactions.filter(t =>
            (t.reference && t.reference.toLowerCase().includes(term)) ||
            (t.description && t.description.toLowerCase().includes(term)) ||
            (t.id && t.id.toString().includes(term))
          );
        }

        if (filteredTransactions.length === 0) {
          this.toastService.warning('Aucune transaction à exporter');
          return;
        }

        // Préparer les données pour le PDF
        const pdfData = filteredTransactions.map(t => ({
          numero: t.id?.toString() || 'N/A',
          reference: t.reference || '-',
          description: t.description || '-',
          date: this.formatDate(t.date),
          montant: this.formatMontant(t.montant),
          type: this.getTransactionTypeLabel(t.type || ''),
          statut: this.getStatutLabel(t.statut || '')
        }));

        // Préparer les options de date
        const dateRange: any = {};
        if (this.filterType === 'date' && this.filterDate) {
          dateRange.date = this.filterDate;
        } else if (this.filterType === 'range' && this.filterStartDate && this.filterEndDate) {
          dateRange.startDate = this.filterStartDate;
          dateRange.endDate = this.filterEndDate;
        }

        // Exporter en PDF
        this.pdfService.exportTable({
          title: 'Liste des Transactions',
          subtitle: `Total: ${filteredTransactions.length} transaction(s)`,
          filename: `transactions_${new Date().toISOString().split('T')[0]}.pdf`,
          dateRange: dateRange,
          columns: [
            { header: 'N°', dataKey: 'numero', width: 30 },
            { header: 'Référence', dataKey: 'reference', width: 50 },
            { header: 'Description', dataKey: 'description', width: 80 },
            { header: 'Date', dataKey: 'date', width: 50 },
            { header: 'Montant', dataKey: 'montant', width: 40 },
            { header: 'Type', dataKey: 'type', width: 50 },
            { header: 'Statut', dataKey: 'statut', width: 40 }
          ],
          data: pdfData
        });

        this.toastService.success('Export PDF réussi!');
      },
      error: (error: any) => {
        console.error('Erreur lors de l\'export PDF:', error);
        this.isLoading = false;
        this.toastService.error('Erreur lors de l\'export PDF');
      }
    });
  }

  formatMontantPdf(montant: number | undefined | null): string {
    if (!montant && montant !== 0) return '0 FCFA';
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'XOF',
      minimumFractionDigits: 0
    }).format(montant);
  }
}
