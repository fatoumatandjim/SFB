import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FacturesService, Facture, LigneFacture, FactureStats } from '../../services/factures.service';
import { ClientsService, Client } from '../../services/clients.service';
import { ProduitsService, Produit } from '../../services/produits.service';
import { TransactionsService, Transaction } from '../../services/transactions.service';
import { ComptesBancairesService, CompteBancaire } from '../../services/comptes-bancaires.service';
import { CaissesService, Caisse } from '../../services/caisses.service';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';
import { AlertService } from '../../nativeComp/alert/alert.service';
import { ToastService } from '../../nativeComp/toast/toast.service';

@Component({
  selector: 'app-facturation',
  templateUrl: './facturation.component.html',
  styleUrls: ['./facturation.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class FacturationComponent implements OnInit {
  activeFilter: string = 'toutes';
  searchTerm: string = '';
  showAddModal: boolean = false;
  showDetailModal: boolean = false;
  selectedFacture: Facture | null = null;
  activeTab: 'details' | 'transactions' | 'telecharger' = 'details';
  isLoading: boolean = false;
  showAddTransactionForm: boolean = false;

  clients: Client[] = [];
  produits: Produit[] = [];
  factures: Facture[] = [];
  transactions: Transaction[] = [];
  comptesBancaires: CompteBancaire[] = [];
  caisses: Caisse[] = [];
  compteTypeTransaction: 'banque' | 'caisse' = 'banque';

  private readonly FACTURE_TRANSACTION_TYPE: string = 'VIREMENT_ENTRANT';

  readonly transactionTypes: { value: string; label: string }[] = [
    { value: 'VIREMENT_ENTRANT', label: 'Virement entrant' },
    { value: 'DEPOT', label: 'Dépôt' },
    { value: 'RETRAIT', label: 'Retrait' },
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

  // Pagination
  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;

  newFacture: Partial<Facture> = {
    clientId: undefined,
    date: new Date().toISOString().split('T')[0],
    dateEcheance: '',
    montantHT: 0,
    montantPaye: 0,
    tauxTVA: 18,
    statut: 'BROUILLON',
    description: '',
    notes: '',
    lignes: []
  };

  newTransaction: Partial<Transaction> = {
    type: this.FACTURE_TRANSACTION_TYPE,
    montant: 0,
    date: new Date().toISOString().split('T')[0],
    statut: 'EN_ATTENTE',
    description: '',
    reference: '',
    beneficiaire: '',
    compteId: undefined,
    caisseId: undefined,
    factureId: undefined
  };

  stats: FactureStats = {
    facturesEmises: {
      total: 0,
      montant: 0,
      periode: 'Ce mois',
      evolution: '0%'
    },
    facturesPayees: {
      total: 0,
      montant: 0,
      pourcentage: '0%'
    },
    facturesImpayees: {
      total: 0,
      montant: 0,
      enRetard: 0,
      urgent: false
    }
  };

  constructor(
    private facturesService: FacturesService,
    private clientsService: ClientsService,
    private produitsService: ProduitsService,
    private transactionsService: TransactionsService,
    private comptesBancairesService: ComptesBancairesService,
    private caissesService: CaissesService,
    private alertService: AlertService,
    private toastService: ToastService
  ) { }

  ngOnInit() {
    this.loadFactures();
    this.loadClients();
    this.loadProduits();
    this.loadStats();
    this.loadComptesBancaires();
    this.loadCaisses();
  }

  loadCaisses() {
    this.caissesService.getAllCaisses().subscribe({
      next: (data) => {
        this.caisses = data.filter((c: Caisse) => c.statut === 'ACTIF');
      },
      error: (error) => {
        console.error('Erreur lors du chargement des caisses:', error);
      }
    });
  }

  loadComptesBancaires() {
    this.comptesBancairesService.getAllComptes().subscribe({
      next: (data) => {
        this.comptesBancaires = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des comptes bancaires:', error);
      }
    });
  }

  loadFactures() {
    this.isLoading = true;
    this.facturesService.getAllFacturesPaginated(this.currentPage, this.pageSize).subscribe({
      next: (data) => {
        this.factures = data.factures;
        this.currentPage = data.currentPage;
        this.totalPages = data.totalPages;
        this.totalElements = data.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des factures:', error);
        this.isLoading = false;
      }
    });
  }

  changePage(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadFactures();
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPages = 5;
    let startPage = Math.max(0, this.currentPage - Math.floor(maxPages / 2));
    let endPage = Math.min(this.totalPages - 1, startPage + maxPages - 1);
    
    if (endPage - startPage < maxPages - 1) {
      startPage = Math.max(0, endPage - maxPages + 1);
    }
    
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    return pages;
  }

  loadClients() {
    this.clientsService.getAllClients().subscribe({
      next: (data) => {
        this.clients = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des clients:', error);
      }
    });
  }

  loadProduits() {
    this.produitsService.getAllProduits().subscribe({
      next: (data) => {
        this.produits = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des produits:', error);
      }
    });
  }

  loadStats() {
    this.facturesService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des statistiques:', error);
      }
    });
  }

  setFilter(filter: string) {
    this.activeFilter = filter;
  }

  get filteredFactures(): Facture[] {
    let filtered = this.factures;

    if (this.activeFilter === 'payees') {
      // Factures complètement payées
      filtered = filtered.filter(f => f.statut === 'PAYEE');
    } else if (this.activeFilter === 'impayees') {
      // Factures non complètement payées (statut != PAYEE ou montantPaye < montantTTC)
      // Exclure les factures annulées
      filtered = filtered.filter(f => {
        if (f.statut === 'ANNULEE') {
          return false;
        }
        const isUnpaid = f.statut !== 'PAYEE';
        const isPartiallyPaid = f.montantPaye !== undefined &&
                                f.montantTTC !== undefined &&
                                f.montantPaye < f.montantTTC;
        return isUnpaid || isPartiallyPaid;
      });
    } else if (this.activeFilter === 'en-retard') {
      // Factures en retard
      filtered = filtered.filter(f => {
        // Exclure les factures annulées
        if (f.statut === 'ANNULEE') {
          return false;
        }
        if (f.statut === 'EN_RETARD') {
          return true;
        }
        // Vérifier aussi si la date d'échéance est dépassée et la facture n'est pas payée
        if (f.dateEcheance) {
          const echeance = new Date(f.dateEcheance);
          const today = new Date();
          today.setHours(0, 0, 0, 0);
          echeance.setHours(0, 0, 0, 0);
          const isOverdue = echeance < today;
          const isUnpaid = f.statut !== 'PAYEE' &&
                          (f.montantPaye === undefined ||
                           f.montantTTC === undefined ||
                           (f.montantPaye < f.montantTTC));
          return isOverdue && isUnpaid;
        }
        return false;
      });
    }
    // 'toutes' affiche toutes les factures sans filtre

    if (this.searchTerm) {
      filtered = filtered.filter(f =>
        (f.numero && f.numero.toLowerCase().includes(this.searchTerm.toLowerCase())) ||
        (f.clientNom && f.clientNom.toLowerCase().includes(this.searchTerm.toLowerCase())) ||
        (f.clientEmail && f.clientEmail.toLowerCase().includes(this.searchTerm.toLowerCase()))
      );
    }

    return filtered;
  }

  viewFacture(facture: Facture) {
    this.selectedFacture = facture;
    this.activeTab = 'details';
    this.showDetailModal = true;
    this.showAddTransactionForm = false;
    if (facture.id) {
      this.loadTransactions(facture.id);
    }
    this.resetNewTransaction();
  }

  closeDetailModal() {
    this.showDetailModal = false;
    this.selectedFacture = null;
    this.activeTab = 'details';
    this.transactions = [];
    this.showAddTransactionForm = false;
    this.resetNewTransaction();
  }

  setTab(tab: 'details' | 'transactions' | 'telecharger') {
    this.activeTab = tab;
  }

  loadTransactions(factureId: number) {
    this.transactionsService.getTransactionsByFacture(factureId).subscribe({
      next: (data) => {
        this.transactions = data;
        this.checkAndUpdateFactureStatutAuto();
      },
      error: (error) => {
        console.error('Erreur lors du chargement des transactions:', error);
        this.transactions = [];
      }
    });
  }

  private checkAndUpdateFactureStatutAuto(): void {
    if (!this.selectedFacture || !this.selectedFacture.id) {
      return;
    }

    const montantFacture = this.selectedFacture.montantTTC ?? this.selectedFacture.montant;
    if (!montantFacture || montantFacture <= 0) {
      return;
    }

    const totalPaye = this.transactions
      .filter(t => t.type === this.FACTURE_TRANSACTION_TYPE && t.statut === 'VALIDE')
      .reduce((sum, t) => sum + (t.montant || 0), 0);

    if (totalPaye < montantFacture || this.selectedFacture.statut === 'PAYEE') {
      return;
    }

    this.facturesService.updateStatut(this.selectedFacture.id, 'PAYEE').subscribe({
      next: (updatedFacture) => {
        this.selectedFacture = updatedFacture;
        this.toastService.success('Facture marquée comme payée automatiquement');
        this.loadFactures();
        this.loadStats();
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour automatique du statut de la facture:', error);
      }
    });
  }

  formatDateTime(dateString: string): string {
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
      'FRAIS_CHAMBRE_COMMERCE': 'Frais chambre de commerce',
      'SALAIRE': 'Salaire'
    };
    return labels[type] || type;
  }

  getTransactionStatutLabel(statut: string): string {
    const labels: { [key: string]: string } = {
      'EN_ATTENTE': 'En attente',
      'VALIDE': 'Validé',
      'REJETE': 'Rejeté',
      'ANNULE': 'Annulé'
    };
    return labels[statut] || statut;
  }

  getTotalTransactions(): number {
    return this.transactions.reduce((sum, t) => sum + (t.montant || 0), 0);
  }

  downloadFacture(facture?: Facture) {
    const factureToDownload = facture || this.selectedFacture;
    if (!factureToDownload) {
      return;
    }

    if (factureToDownload.id) {
      this.transactionsService.getTransactionsByFacture(factureToDownload.id).subscribe({
        next: async (transactions) => {
          await this.generateFacturePdf(factureToDownload, transactions);
        },
        error: async (error) => {
          console.error('Erreur lors du chargement des transactions pour la génération du PDF:', error);
          await this.generateFacturePdf(factureToDownload, []);
        }
      });
    } else {
      this.generateFacturePdf(factureToDownload, []);
    }
  }

  updateStatutFacture() {
    if (!this.selectedFacture || !this.selectedFacture.id || !this.selectedFacture.statut) return;

    // Ne pas permettre de changer le statut si la facture est payée
    if (this.selectedFacture.statut === 'PAYEE') {
      this.toastService.warning('Le statut d\'une facture payée ne peut pas être modifié');
      return;
    }

    this.isLoading = true;
    this.facturesService.updateStatut(this.selectedFacture.id, this.selectedFacture.statut).subscribe({
      next: (updatedFacture) => {
        this.isLoading = false;
        this.selectedFacture = updatedFacture;
        this.toastService.success('Statut de la facture mis à jour avec succès!');
        this.loadFactures();
        this.loadStats();
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour du statut:', error);
        this.isLoading = false;
        this.toastService.error('Erreur lors de la mise à jour du statut de la facture');
      }
    });
  }

  moreOptions(facture: Facture) {
  }

  nouvelleFacture() {
    this.newFacture = {
      clientId: undefined,
      date: new Date().toISOString().split('T')[0],
      dateEcheance: '',
      montantHT: 0,
      montantPaye: 0,
      tauxTVA: 18,
      statut: 'BROUILLON',
      description: '',
      notes: '',
      lignes: []
    };
    this.calculateDateEcheance();
    this.showAddModal = true;
  }

  closeAddModal() {
    this.showAddModal = false;
    this.newFacture = {
      clientId: undefined,
      date: new Date().toISOString().split('T')[0],
      dateEcheance: '',
      montantHT: 0,
      montantPaye: 0,
      tauxTVA: 18,
      statut: 'BROUILLON',
      description: '',
      notes: '',
      lignes: []
    };
  }

  addLigneFacture() {
    if (!this.newFacture.lignes) {
      this.newFacture.lignes = [];
    }
    this.newFacture.lignes.push({
      produitId: undefined,
      quantite: 0,
      prixUnitaire: 0,
      total: 0
    });
  }

  removeLigneFacture(index: number) {
    if (this.newFacture.lignes) {
      this.newFacture.lignes.splice(index, 1);
      this.calculateMontantTotal();
    }
  }

  onProduitChange(index: number) {
    if (!this.newFacture.lignes) return;
    const ligne = this.newFacture.lignes[index];
    if (ligne && ligne.produitId) {
      const produit = this.produits.find(p => p.id === ligne.produitId);
      // Le prix unitaire sera saisi manuellement ou récupéré depuis le stock
      // Pour l'instant, on laisse l'utilisateur le saisir
    }
  }

  calculateLigneTotal(index: number) {
    if (!this.newFacture.lignes) return;
    const ligne = this.newFacture.lignes[index];
    if (ligne) {
      ligne.total = ligne.quantite * ligne.prixUnitaire;
      this.calculateMontantTotal();
    }
  }

  calculateMontantTotal() {
    // Le montant HT est la somme des totaux des lignes
    this.newFacture.montantHT = this.newFacture.lignes?.reduce((sum, ligne) => sum + ligne.total, 0) || 0;
  }

  getTotalHT(): number {
    return this.newFacture.montantHT || 0;
  }

  getTotalTVA(): number {
    return (this.getTotalHT() * (this.newFacture.tauxTVA || 0)) / 100;
  }

  getTotalTTC(): number {
    return this.getTotalHT() + this.getTotalTVA();
  }

  getResteAPayer(): number {
    return this.getTotalTTC() - (this.newFacture.montantPaye || 0);
  }

  getProduitNom(produitId: number | undefined): string {
    if (!produitId) return '';
    const produit = this.produits.find(p => p.id === produitId);
    return produit ? produit.nom : '';
  }

  getClientColor(clientNom: string | undefined): string {
    if (!clientNom) return 'blue';
    const colors = ['blue', 'green', 'purple', 'orange', 'red', 'teal', 'pink', 'indigo'];
    const index = clientNom.charCodeAt(0) % colors.length;
    return colors[index];
  }

  getClientInitiales(clientNom: string | undefined): string {
    if (!clientNom) return 'N/A';
    const words = clientNom.split(' ');
    if (words.length >= 2) {
      return (words[0][0] + words[1][0]).toUpperCase();
    }
    return clientNom.substring(0, 2).toUpperCase();
  }

  formatDate(date: string | undefined): string {
    if (!date) return '';
    try {
      const d = new Date(date);
      if (isNaN(d.getTime())) return date;
      return d.toLocaleDateString('fr-FR', {
        day: 'numeric',
        month: 'long',
        year: 'numeric'
      });
    } catch {
      return date;
    }
  }

  calculateDateEcheance() {
    if (this.newFacture.date) {
      const date = new Date(this.newFacture.date);
      // Ajouter 15 jours par défaut pour l'échéance
      date.setDate(date.getDate() + 15);
      this.newFacture.dateEcheance = date.toISOString().split('T')[0];
    }
  }

  validateFacture(): boolean {
    if (!this.newFacture.clientId) {
      this.toastService.warning('Veuillez sélectionner un client');
      return false;
    }
    if (!this.newFacture.date) {
      this.toastService.warning('Veuillez saisir une date de facture');
      return false;
    }
    if (!this.newFacture.dateEcheance) {
      this.toastService.warning('Veuillez saisir une date d\'échéance');
      return false;
    }
    if (!this.newFacture.lignes || this.newFacture.lignes.length === 0) {
      this.toastService.warning('Veuillez ajouter au moins un produit à la facture');
      return false;
    }
    for (let i = 0; i < this.newFacture.lignes.length; i++) {
      const ligne = this.newFacture.lignes[i];
      if (!ligne.produitId) {
        this.toastService.warning(`Veuillez sélectionner un produit pour la ligne ${i + 1}`);
        return false;
      }
      if (!ligne.quantite || ligne.quantite <= 0) {
        this.toastService.warning(`Veuillez saisir une quantité valide pour la ligne ${i + 1}`);
        return false;
      }
      if (!ligne.prixUnitaire || ligne.prixUnitaire <= 0) {
        this.toastService.warning(`Veuillez saisir un prix unitaire valide pour la ligne ${i + 1}`);
        return false;
      }
    }
    if (this.getTotalHT() <= 0) {
      this.toastService.warning('Le montant total HT doit être supérieur à 0');
      return false;
    }
    if ((this.newFacture.montantPaye || 0) < 0) {
      this.toastService.warning('Le montant payé ne peut pas être négatif');
      return false;
    }
    if ((this.newFacture.montantPaye || 0) > this.getTotalTTC()) {
      this.toastService.warning('Le montant payé ne peut pas dépasser le montant TTC');
      return false;
    }
    return true;
  }

  saveFacture() {
    if (!this.validateFacture()) {
      return;
    }

    this.isLoading = true;

    // Préparer la facture pour l'envoi
    const factureToSave: Facture = {
      numero: '', // Sera généré par le backend
      clientId: this.newFacture.clientId!,
      date: this.newFacture.date!,
      dateEcheance: this.newFacture.dateEcheance!,
      montant: this.getTotalTTC(), // Montant TTC
      montantHT: this.getTotalHT(),
      tauxTVA: this.newFacture.tauxTVA || 18,
      montantPaye: this.newFacture.montantPaye || 0,
      statut: this.newFacture.statut || 'BROUILLON',
      description: this.newFacture.description || '',
      notes: this.newFacture.notes || '',
      lignes: this.newFacture.lignes!.filter(l => l.produitId).map(l => ({
        produitId: l.produitId!,
        quantite: l.quantite,
        prixUnitaire: l.prixUnitaire,
        total: l.total
      }))
    };

        this.facturesService.createFacture(factureToSave).subscribe({
          next: () => {
            this.isLoading = false;
            this.toastService.success('Facture créée avec succès!');
            this.closeAddModal();
            this.loadFactures();
            this.loadStats();
          },
      error: (error) => {
        console.error('Erreur lors de la création de la facture:', error);
        this.isLoading = false;
        this.toastService.error('Erreur lors de la création de la facture. Veuillez réessayer.');
      }
    });
  }

  toggleAddTransactionForm() {
    this.showAddTransactionForm = !this.showAddTransactionForm;
    if (this.showAddTransactionForm) {
      this.resetNewTransaction();
    }
  }

  resetNewTransaction() {
    this.compteTypeTransaction = 'banque';
    this.newTransaction = {
      type: this.FACTURE_TRANSACTION_TYPE,
      montant: 0,
      date: new Date().toISOString().split('T')[0],
      statut: 'EN_ATTENTE',
      description: '',
      reference: '',
      beneficiaire: '',
      compteId: undefined,
      caisseId: undefined,
      factureId: this.selectedFacture?.id
    };
  }

  addTransaction() {
    if (!this.selectedFacture || !this.selectedFacture.id) {
      this.toastService.warning('Aucune facture sélectionnée');
      return;
    }

    if (!this.validateTransaction()) {
      return;
    }

    this.isLoading = true;
    const transactionToSave: Transaction = {
      type: this.newTransaction.type || this.FACTURE_TRANSACTION_TYPE,
      montant: this.newTransaction.montant!,
      date: new Date(this.newTransaction.date!).toISOString(),
      statut: this.newTransaction.statut!,
      description: this.newTransaction.description || '',
      reference: this.newTransaction.reference || '',
      beneficiaire: this.newTransaction.beneficiaire || '',
      compteId: this.compteTypeTransaction === 'banque' ? this.newTransaction.compteId : undefined,
      caisseId: this.compteTypeTransaction === 'caisse' ? this.newTransaction.caisseId : undefined,
      factureId: this.selectedFacture.id
    };

    this.transactionsService.createTransaction(transactionToSave).subscribe({
      next: () => {
        // Recharger les transactions pour calculer le nouveau montant payé
        this.loadTransactions(this.selectedFacture!.id!);
        this.isLoading = false;
        this.showAddTransactionForm = false;

        // Le montant payé sera mis à jour après le chargement des transactions
      },
      error: (error) => {
        console.error('Erreur lors de l\'ajout de la transaction:', error);
        this.isLoading = false;
        this.toastService.error('Erreur lors de l\'ajout de la transaction. Veuillez réessayer.');
      }
    });
  }

  validateTransaction(): boolean {
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
    if (this.compteTypeTransaction === 'banque' && !this.newTransaction.compteId) {
      this.toastService.warning('Veuillez sélectionner un compte bancaire');
      return false;
    }
    if (this.compteTypeTransaction === 'caisse' && !this.newTransaction.caisseId) {
      this.toastService.warning('Veuillez sélectionner une caisse');
      return false;
    }
    if (!this.newTransaction.description || this.newTransaction.description.trim() === '') {
      this.toastService.warning('Veuillez saisir une description');
      return false;
    }
    return true;
  }

  private async generateFacturePdf(facture: Facture, transactions: Transaction[]) {
    const doc = new jsPDF('p', 'mm', 'a4');

    const marginLeft = 15;
    const marginRight = 15;
    const pageWidth = doc.internal.pageSize.getWidth();
    const pageHeight = doc.internal.pageSize.getHeight();
    let y = 15;

    // ---------- Logo à gauche ----------
    try {
      const logoImg = await this.loadImage('/assets/logo.jpeg');
      if (logoImg) {
        doc.addImage(logoImg, 'JPEG', marginLeft, y, 30, 15);
      }
    } catch (error) {
      console.error('Erreur lors du chargement du logo:', error);
    }

    // ---------- Informations facture à droite ----------
    const rightX = pageWidth - marginRight;
    const statutLabel = this.getFactureStatutLabel(facture.statut || '');
    doc.setFontSize(12);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(139, 69, 19); // Couleur rouge-brun
    doc.text('Facture ' + statutLabel, rightX, y, { align: 'right' });

    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(0, 0, 0);
    let infoY = y + 5;
    doc.text('Correction facture : ' + (facture.numero || '-'), rightX, infoY, { align: 'right' });
    infoY += 4;
    doc.text('Date facturation : ' + (this.formatDate(facture.date) || '-'), rightX, infoY, { align: 'right' });
    infoY += 4;
    const clientCode = (facture as any).clientCode || 'CU' + String(facture.clientId || '00000').padStart(5, '0');
    doc.text('Code client : ' + clientCode, rightX, infoY, { align: 'right' });

    // ---------- Bloc Émetteur (gauche, avec fond gris) ----------
    // Descendre pour éviter le chevauchement avec SFB
    let emitterY = y + 25;
    const emitterX = marginLeft;
    const emitterWidth = 80;
    const emitterHeight = 35;

    // Fond gris pour Émetteur
    doc.setFillColor(240, 240, 240);
    doc.rect(emitterX, emitterY, emitterWidth, emitterHeight, 'F');

    doc.setFontSize(10);
    doc.setFont('helvetica', 'bold');
    doc.text('Émetteur', emitterX + 2, emitterY + 5);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    let emitterTextY = emitterY + 10; // Augmenter l'espacement pour éviter le chevauchement
    doc.text('SFB', emitterX + 2, emitterTextY);
    emitterTextY += 4;
    doc.text('Compte AFG : 80080515001', emitterX + 2, emitterTextY);
    emitterTextY += 4;
    doc.text('Bamako', emitterX + 2, emitterTextY);
    emitterTextY += 4;
    doc.text('Tél.: TÉL (+223) 70 90 28 28 -', emitterX + 2, emitterTextY);
    emitterTextY += 4;
    doc.text('Email: hyattassaye87@gmail.com', emitterX + 2, emitterTextY);

    // ---------- Bloc Adressé à (droite, sans fond) ----------
    let addressedY = emitterY;
    const addressedX = marginLeft + emitterWidth + 15;
    doc.setFontSize(10);
    doc.setFont('helvetica', 'bold');
    doc.text('Adressé à', addressedX, addressedY + 5);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(9);
    let addressedTextY = addressedY + 10; // Augmenter l'espacement pour éviter le chevauchement
    doc.setFont('helvetica', 'bold');
    doc.text(facture.clientNom || 'Client', addressedX, addressedTextY);
    doc.setFont('helvetica', 'normal');
    addressedTextY += 4;
    if ((facture as any).clientAdresse) {
      doc.text((facture as any).clientAdresse, addressedX, addressedTextY);
    } else if (facture.clientEmail) {
      doc.text(facture.clientEmail, addressedX, addressedTextY);
    }

    // ---------- Ligne horizontale de séparation ----------
    y = emitterY + emitterHeight + 5;
    doc.setDrawColor(0, 0, 0);
    doc.setLineWidth(0.5);
    doc.line(marginLeft, y, pageWidth - marginRight, y);
    y += 5;

    // ---------- Tableau des produits ----------
    const tableWidth = pageWidth - marginLeft - marginRight;

    // Texte "Montants exprimés en Francs CFA BCEAO" centré au-dessus du tableau
    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    const textX = pageWidth - marginRight - 10; // Décaler vers la droite
    doc.text('Montants exprimés en Francs CFA BCEAO', textX, y, { align: 'right' });
    y += 5;

    // Préparer les données du tableau
    const tableData: any[] = [];
    if (facture.lignes && facture.lignes.length > 0) {
      facture.lignes.forEach((ligne, index) => {
        const produitNom = (ligne as any).produitNom || this.getProduitNom(ligne.produitId) || '-';
        const typeProduit = (ligne && (ligne as any).typeProduit) || '';
        const designation = produitNom + (typeProduit ? ' - ' + typeProduit : '');
        const isLastLine = facture.lignes && (index === facture.lignes.length - 1);
        tableData.push([
          {
            content: designation,
            styles: {
              minCellHeight: isLastLine ? 120 : 0, // Ajustez 120 selon l'espace voulu
              valign: 'top' // Garde le texte en haut de la grande cellule
            }
          },
          { content: this.formatMontant(ligne.prixUnitaire || 0), styles: { valign: 'top' } },
          { content: String(ligne.quantite || 0), styles: { valign: 'top' } },
          { content: this.formatMontant(ligne.total || 0), styles: { valign: 'top' } }
        ]);
      });
    } else {
      tableData.push(['Aucun produit', '', '', '']);
    }

    // Utiliser autotable pour créer le tableau avec des largeurs réduites
    autoTable(doc, {
      startY: y,
      head: [['Désignation', 'P.U. HT', 'Qté', 'Total HT']],
      body: tableData,
      theme: 'grid',
      headStyles: {
        fillColor: [240, 240, 240],
        fontStyle: 'bold',
        fontSize: 9,
        textColor: [0, 0, 0]
      },
      bodyStyles: {
        fontSize: 9,
        textColor: [0, 0, 0],
      },
      columnStyles: {
        0: { cellWidth: tableWidth * 0.35, halign: 'left' },
        1: { cellWidth: tableWidth * 0.20, halign: 'right' },
        2: { cellWidth: tableWidth * 0.15, halign: 'right' },
        3: { cellWidth: tableWidth * 0.30, halign: 'right' }
      },
      margin: { left: marginLeft, right: marginRight },
      styles: {
        lineColor: [0, 0, 0],
        lineWidth: 0.5
      },
      tableWidth: tableWidth, // Limiter la largeur totale du tableau

    });

    // Obtenir la position Y après le tableau
    const finalY = (doc as any).lastAutoTable.finalY || y + 50;

    // ---------- Total ----------
    const montantTTC = (facture as any).montantTTC || facture.montant || 0;
    const colDesc = marginLeft + 2;
    const colAmount = pageWidth - marginRight - 2;

    doc.setFontSize(10);
    doc.setFont('helvetica', 'bold');
    doc.text('Total', colDesc, finalY + 5);
    doc.text(this.formatMontant(montantTTC), colAmount, finalY + 5, { align: 'right' });

    // ---------- Pied de page (une seule ligne centrée) ----------
    let footerY = pageHeight - 15;

    doc.setFontSize(7);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(0, 0, 0);
    const footerText = 'SIÈGE Torokorobougou immeuble Wad motors, Bamako, MALI CONTACT : TÉL (+223) 70 90 28 28 - NIF : 085129628 K - RC : MA.BKO.2024.7890';
    doc.text(footerText, pageWidth / 2, footerY, { align: 'center' });

    // Numéro de page à droite
    doc.text('1/1', pageWidth - marginRight, footerY, { align: 'right' });

    const fileName = `Facture_SFB_${facture.numero || 'sans_numero'}.pdf`;
    doc.save(fileName);
  }

  private getFactureStatutLabel(statut: string): string {
    const labels: { [key: string]: string } = {
      'PAYEE': 'Payée',
      'EMISE': 'Émise',
      'BROUILLON': 'Brouillon',
      'EN_RETARD': 'En retard',
      'PARTIELLEMENT_PAYEE': 'Partiellement payée',
      'ANNULEE': 'Annulée'
    };
    return labels[statut] || statut;
  }

  private formatMontant(value: number | undefined | null): string {
    const num = value || 0;
    // Format sans décimales, avec espaces pour les milliers
    const formatted = Math.round(num).toString();
    // Ajouter des espaces comme séparateurs de milliers
    return formatted.replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
  }

  private loadImage(src: string): Promise<string> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext('2d');
        if (ctx) {
          ctx.drawImage(img, 0, 0);
          resolve(canvas.toDataURL('image/jpeg'));
        } else {
          reject(new Error('Impossible de créer le contexte canvas'));
        }
      };
      img.onerror = () => reject(new Error('Erreur lors du chargement de l\'image'));
      img.src = src;
    });
  }
}
