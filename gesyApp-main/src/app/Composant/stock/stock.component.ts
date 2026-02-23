import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { DepotsService, Depot } from '../../services/depots.service';
import { StocksService, Stock, StockStats } from '../../services/stocks.service';
import { ProduitsService, Produit as ProduitAPI, ProduitAvecStocks } from '../../services/produits.service';
import { MouvementsService, Mouvement as MouvementAPI, MouvementPage } from '../../services/mouvements.service';
import { ManquantsService, Manquant, ManquantPage } from '../../services/manquants.service';
import { AlertService } from '../../nativeComp/alert/alert.service';
import { ToastService } from '../../nativeComp/toast/toast.service';

interface Produit {
  id: string;
  nom: string;
  code: string;
  categorie: string;
  stock: number;
  capacite: number;
  minimum: number;
  niveau: 'optimal' | 'faible' | 'critique';
  derniereEntree: string;
  icon: string;
  couleur: string;
}

interface Mouvement {
  id: string;
  type: 'entree' | 'sortie' | 'transfert' | 'inventaire';
  produit: string;
  quantite: number;
  unite: string;
  date: string;
  heure: string;
}

@Component({
  selector: 'app-stock',
  templateUrl: './stock.component.html',
  styleUrls: ['./stock.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class StockComponent implements OnInit {
  activeTab: 'stocks' | 'mouvements' | 'manquants' = 'stocks';
  selectedDepot: string = 'tous';
  showAddProduitModal: boolean = false;
  showAddStockModal: boolean = false;
  showAllMouvementsModal: boolean = false;
  isLoading: boolean = false;
  isLoadingMouvements: boolean = false;
  isLoadingManquants: boolean = false;
  depots: Depot[] = [];
  produitsAPI: ProduitAPI[] = [];
  produitsAvecStocks: ProduitAvecStocks[] = [];
  mouvementsRecents: MouvementAPI[] = [];
  mouvementsPage: MouvementPage | null = null;
  manquants: Manquant[] = [];
  manquantsPage: ManquantPage | null = null;
  currentPage: number = 0;
  currentPageManquants: number = 0;
  pageSize: number = 10;
  filterDate: string = '';
  filterStartDate: string = '';
  filterEndDate: string = '';
  filterType: 'all' | 'date' | 'range' = 'all';
  filterTypeManquants: 'all' | 'date' | 'range' = 'all';
  filterDateManquants: string = '';
  filterStartDateManquants: string = '';
  filterEndDateManquants: string = '';

  newProduit: {
    nom: string;
    typeProduit: 'ESSENCE' | 'GAZOLE' | 'KEROSENE' | 'GPL' | 'AUTRE';
    description: string;
  } = {
    nom: '',
    typeProduit: 'ESSENCE',
    description: ''
  };

  newStock: {
    produitId: number | undefined;
    typeProduit: 'ESSENCE' | 'GAZOLE' | 'KEROSENE' | 'GPL' | 'AUTRE';
    quantite: number;
    depotId: number | undefined;
    seuilMinimum: number;
    prixUnitaire: number;
    unite: string;
  } = {
    produitId: undefined,
    typeProduit: 'ESSENCE',
    quantite: 0,
    depotId: undefined,
    seuilMinimum: 0,
    prixUnitaire: 0,
    unite: 'L'
  };

  stats: StockStats | null = null;

  produits: Produit[] = [
    {
      id: '1',
      nom: 'Essence Super',
      code: 'ESS-001',
      categorie: 'Carburant',
      stock: 3250,
      capacite: 5000,
      minimum: 1000,
      niveau: 'optimal',
      derniereEntree: 'Hier',
      icon: '⛽',
      couleur: 'blue'
    },
    {
      id: '2',
      nom: 'Diesel',
      code: 'DSL-002',
      categorie: 'Carburant',
      stock: 850,
      capacite: 5000,
      minimum: 1000,
      niveau: 'critique',
      derniereEntree: 'Aujourd\'hui',
      icon: '⛽',
      couleur: 'orange'
    },
    {
      id: '3',
      nom: 'Huile Moteur',
      code: 'HUI-003',
      categorie: 'Lubrifiant',
      stock: 450,
      capacite: 1000,
      minimum: 200,
      niveau: 'faible',
      derniereEntree: 'Il y a 2 jours',
      icon: '🛢️',
      couleur: 'green'
    },
    {
      id: '4',
      nom: 'GPL',
      code: 'GPL-004',
      categorie: 'Carburant',
      stock: 2800,
      capacite: 4000,
      minimum: 800,
      niveau: 'optimal',
      derniereEntree: 'Hier',
      icon: '🔥',
      couleur: 'red'
    }
  ];


  constructor(
    private depotsService: DepotsService,
    private stocksService: StocksService,
    private produitsService: ProduitsService,
    private mouvementsService: MouvementsService,
    private manquantsService: ManquantsService,
    private alertService: AlertService,
    private toastService: ToastService
  ) { }

  ngOnInit() {
    this.loadDepots();
    this.loadProduits();
    this.loadProduitsAvecStocks();
    this.loadStats();
    this.loadMouvementsRecents();
  }

  loadDepots() {
    this.depotsService.getAllDepots().subscribe({
      next: (data) => {
        this.depots = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des dépôts:', error);
      }
    });
  }

  loadProduits() {
    this.produitsService.getAllProduits().subscribe({
      next: (data) => {
        this.produitsAPI = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des produits:', error);
      }
    });
  }

  loadProduitsAvecStocks() {
    this.isLoading = true;
    this.produitsService.getAllProduitsAvecStocks().subscribe({
      next: (data) => {
        this.produitsAvecStocks = data;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des produits avec stocks:', error);
        this.isLoading = false;
      }
    });
  }

  loadStats() {
    this.stocksService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des statistiques:', error);
      }
    });
  }

  loadMouvementsRecents() {
    this.mouvementsService.getRecentMouvements(5).subscribe({
      next: (data) => {
        this.mouvementsRecents = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des mouvements récents:', error);
      }
    });
  }

  voirTousMouvements() {
    this.showAllMouvementsModal = true;
    this.currentPage = 0;
    this.filterType = 'all';
    this.loadMouvementsPaginated();
  }

  closeAllMouvementsModal() {
    this.showAllMouvementsModal = false;
    this.mouvementsPage = null;
    this.currentPage = 0;
    this.filterDate = '';
    this.filterStartDate = '';
    this.filterEndDate = '';
    this.filterType = 'all';
  }

  loadMouvementsPaginated() {
    this.isLoadingMouvements = true;
    let request: Observable<MouvementPage>;

    if (this.filterType === 'date' && this.filterDate) {
      request = this.mouvementsService.getMouvementsByDate(
        this.filterDate,
        this.currentPage,
        this.pageSize
      );
    } else if (this.filterType === 'range' && this.filterStartDate && this.filterEndDate) {
      request = this.mouvementsService.getMouvementsByDateRange(
        this.filterStartDate,
        this.filterEndDate,
        this.currentPage,
        this.pageSize
      );
    } else {
      request = this.mouvementsService.getMouvementsPaginated(
        this.currentPage,
        this.pageSize
      );
    }

    request.subscribe({
      next: (data) => {
        this.mouvementsPage = data;
        this.isLoadingMouvements = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des mouvements:', error);
        this.isLoadingMouvements = false;
      }
    });
  }

  applyFilter() {
    this.currentPage = 0;
    this.loadMouvementsPaginated();
  }

  changePage(page: number) {
    this.currentPage = page;
    this.loadMouvementsPaginated();
  }

  getTypeMouvementLabel(type: string): string {
    switch (type) {
      case 'ENTREE':
        return 'Entrée Stock';
      case 'SORTIE':
        return 'Sortie Stock';
      case 'TRANSFERT':
        return 'Transfert';
      case 'INVENTAIRE':
        return 'Inventaire';
      default:
        return type;
    }
  }

  getTypeMouvementIcon(type: string): string {
    switch (type) {
      case 'ENTREE':
        return '↓';
      case 'SORTIE':
        return '↑';
      case 'TRANSFERT':
        return '⇄';
      case 'INVENTAIRE':
        return '💼';
      default:
        return '📦';
    }
  }

  formatDate(dateString: string): string {
    if (!dateString) return '';
    try {
      const date = new Date(dateString);

      // Vérifier si la date est valide
      if (isNaN(date.getTime())) {
        return '';
      }

      const now = new Date();

      // Normaliser les dates pour comparer uniquement les jours (sans l'heure)
      const dateOnly = new Date(date.getFullYear(), date.getMonth(), date.getDate());
      const nowOnly = new Date(now.getFullYear(), now.getMonth(), now.getDate());

      // Calculer la différence en jours
      const diffTime = nowOnly.getTime() - dateOnly.getTime();
      const diffDays = Math.round(diffTime / (1000 * 60 * 60 * 24));

      if (diffDays === 0) {
        return 'Aujourd\'hui';
      } else if (diffDays === 1) {
        return 'Hier';
      } else if (diffDays === 2) {
        return 'Avant-hier';
      } else if (diffDays > 0 && diffDays <= 7) {
        return `Il y a ${diffDays} jours`;
      } else if (diffDays < 0) {
        // Date dans le futur (ne devrait pas arriver normalement)
        return date.toLocaleDateString('fr-FR', {
          day: 'numeric',
          month: 'long',
          year: 'numeric'
        });
      } else {
        return date.toLocaleDateString('fr-FR', {
          day: 'numeric',
          month: 'long',
          year: 'numeric'
        });
      }
    } catch (error) {
      console.error('Erreur lors du formatage de la date:', error, dateString);
      return '';
    }
  }

  formatTime(dateString: string): string {
    if (!dateString) return '';
    try {
      const date = new Date(dateString);

      // Vérifier si la date est valide
      if (isNaN(date.getTime())) {
        return '';
      }

      return date.toLocaleTimeString('fr-FR', {
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (error) {
      console.error('Erreur lors du formatage de l\'heure:', error, dateString);
      return '';
    }
  }

  getPourcentageStock(produit: ProduitAvecStocks): number {
    // Calculer le pourcentage basé sur la quantité totale
    // Pour l'instant, on utilise une valeur par défaut de 100% si pas de seuil
    if (!produit.stocks || produit.stocks.length === 0) {
      return 0;
    }
    // Trouver le seuil minimum le plus élevé ou utiliser une valeur par défaut
    const maxSeuil = produit.stocks
      .map(s => s.seuilMinimum || 0)
      .reduce((a, b) => Math.max(a, b), 0);
    if (maxSeuil === 0) {
      return 100; // Pas de seuil défini, considérer comme optimal
    }
    const pourcentage = (produit.quantiteTotale / (maxSeuil * 2)) * 100;
    return Math.min(100, Math.max(0, Math.round(pourcentage)));
  }

  getNiveauStock(produit: ProduitAvecStocks): 'optimal' | 'faible' | 'critique' {
    if (!produit.stocks || produit.stocks.length === 0) {
      return 'critique';
    }
    // Trouver le seuil minimum le plus élevé
    const maxSeuil = produit.stocks
      .map(s => s.seuilMinimum || 0)
      .reduce((a, b) => Math.max(a, b), 0);

    if (maxSeuil === 0) {
      return 'optimal'; // Pas de seuil défini
    }

    if (produit.quantiteTotale < maxSeuil) {
      return 'critique';
    } else if (produit.quantiteTotale < maxSeuil * 1.5) {
      return 'faible';
    } else {
      return 'optimal';
    }
  }

  getNiveauStockLabel(produit: ProduitAvecStocks): string {
    const niveau = this.getNiveauStock(produit);
    return niveau === 'optimal' ? 'Optimal' : niveau === 'faible' ? 'Faible' : 'Critique';
  }

  getTypeProduitIcon(typeProduit: string): string {
    switch (typeProduit) {
      case 'ESSENCE':
      case 'GAZOLE':
        return '⛽';
      case 'GPL':
        return '🔥';
      case 'KEROSENE':
        return '🛢️';
      default:
        return '📦';
    }
  }

  getTypeProduitColor(typeProduit: string): string {
    switch (typeProduit) {
      case 'ESSENCE':
        return 'blue';
      case 'GAZOLE':
        return 'orange';
      case 'GPL':
        return 'red';
      case 'KEROSENE':
        return 'green';
      default:
        return 'blue';
    }
  }

  getUniteProduit(produit: ProduitAvecStocks): string {
    if (produit.stocks && produit.stocks.length > 0) {
      return produit.stocks[0].unite || 'L';
    }
    return 'L';
  }

  nouveauProduit() {
    this.newProduit = {
      nom: '',
      typeProduit: 'ESSENCE',
      description: ''
    };
    this.showAddProduitModal = true;
  }

  closeAddProduitModal() {
    this.showAddProduitModal = false;
    this.newProduit = {
      nom: '',
      typeProduit: 'ESSENCE',
      description: ''
    };
  }

  addProduit() {
    if (!this.validateProduit()) {
      return;
    }

    this.isLoading = true;
    const produitToCreate: ProduitAPI = {
      nom: this.newProduit.nom,
      typeProduit: this.newProduit.typeProduit,
      description: this.newProduit.description || undefined
    };

    this.produitsService.createProduit(produitToCreate).subscribe({
      next: (newProduit) => {
        this.isLoading = false;
        this.toastService.success('Produit créé avec succès!');
        this.closeAddProduitModal();
        this.loadProduits(); // Recharger la liste des produits
        this.loadProduitsAvecStocks(); // Recharger la liste des produits avec stocks
      },
      error: (error) => {
        console.error('Erreur lors de la création du produit:', error);
        this.toastService.error('Erreur lors de la création du produit. Veuillez réessayer.');
        this.isLoading = false;
      }
    });
  }

  validateProduit(): boolean {
    if (!this.newProduit.nom || this.newProduit.nom.trim() === '') {
      this.toastService.warning('Veuillez saisir le nom du produit');
      return false;
    }
    return true;
  }

  nouveauStock() {
    this.newStock = {
      produitId: undefined,
      typeProduit: 'ESSENCE',
      quantite: 0,
      depotId: undefined,
      seuilMinimum: 0,
      prixUnitaire: 0,
      unite: 'L'
    };
    this.showAddStockModal = true;
  }

  closeAddStockModal() {
    this.showAddStockModal = false;
    this.newStock = {
      produitId: undefined,
      typeProduit: 'ESSENCE',
      quantite: 0,
      depotId: undefined,
      seuilMinimum: 0,
      prixUnitaire: 0,
      unite: 'L'
    };
  }

  onProduitChange() {
    const produit = this.produitsAPI.find(p => p.id === this.newStock.produitId);
    if (produit) {
      this.newStock.typeProduit = produit.typeProduit;
    }
  }

  addStock() {
    if (!this.validateStock()) {
      return;
    }

    this.isLoading = true;
    const stockToCreate: Stock = {
      produitId: this.newStock.produitId!,
      quantite: this.newStock.quantite,
      depotId: this.newStock.depotId!,
      seuilMinimum: this.newStock.seuilMinimum || undefined,
      prixUnitaire: this.newStock.prixUnitaire || undefined,
      unite: this.newStock.unite
    };

    this.stocksService.createStock(stockToCreate).subscribe({
      next: (newStock) => {
        this.isLoading = false;
        this.toastService.success('Stock ajouté avec succès!');
        this.closeAddStockModal();
        this.loadProduitsAvecStocks(); // Recharger la liste des produits avec stocks
      },
      error: (error) => {
        console.error('Erreur lors de l\'ajout du stock:', error);
        this.toastService.error('Erreur lors de l\'ajout du stock. Veuillez réessayer.');
        this.isLoading = false;
      }
    });
  }

  validateStock(): boolean {
    if (!this.newStock.produitId) {
      this.toastService.warning('Veuillez sélectionner un produit');
      return false;
    }
    if (!this.newStock.depotId) {
      this.toastService.warning('Veuillez sélectionner un dépôt');
      return false;
    }
    if (!this.newStock.quantite || this.newStock.quantite <= 0) {
      this.toastService.warning('Veuillez saisir une quantité valide (supérieure à 0)');
      return false;
    }
    return true;
  }

  inventaire() {
  }

  voirMouvements(produit: ProduitAvecStocks) {
  }

  setTab(tab: 'stocks' | 'mouvements' | 'manquants') {
    this.activeTab = tab;
    if (tab === 'manquants') {
      this.loadManquants();
    }
  }

  loadManquants() {
    this.isLoadingManquants = true;
    let request;

    if (this.filterTypeManquants === 'date' && this.filterDateManquants) {
      request = this.manquantsService.getManquantsByDate(
        this.filterDateManquants,
        this.currentPageManquants,
        this.pageSize
      );
    } else if (this.filterTypeManquants === 'range' && this.filterStartDateManquants && this.filterEndDateManquants) {
      request = this.manquantsService.getManquantsByDateRange(
        this.filterStartDateManquants,
        this.filterEndDateManquants,
        this.currentPageManquants,
        this.pageSize
      );
    } else {
      request = this.manquantsService.getManquants(
        this.currentPageManquants,
        this.pageSize
      );
    }

    request.subscribe({
      next: (data) => {
        this.manquantsPage = data;
        this.manquants = data.manquants;
        this.isLoadingManquants = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des manquants:', error);
        this.toastService.error('Erreur lors du chargement des manquants');
        this.isLoadingManquants = false;
      }
    });
  }

  applyFilterManquants() {
    this.currentPageManquants = 0;
    this.loadManquants();
  }

  changePageManquants(page: number) {
    this.currentPageManquants = page;
    this.loadManquants();
  }

  formatMontant(montant: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'XOF',
      minimumFractionDigits: 0
    }).format(montant || 0);
  }

  formatDateManquant(dateString: string): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getPagesArray(totalPages: number): number[] {
    const pages: number[] = [];
    const maxVisiblePages = 5;
    let start = Math.max(0, this.currentPageManquants - Math.floor(maxVisiblePages / 2));
    let end = Math.min(totalPages, start + maxVisiblePages);
    
    if (end - start < maxVisiblePages) {
      start = Math.max(0, end - maxVisiblePages);
    }
    
    for (let i = start; i < end; i++) {
      pages.push(i);
    }
    return pages;
  }
}
