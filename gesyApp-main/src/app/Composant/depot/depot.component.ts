import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DepotsService, Depot } from '../../services/depots.service';
import { ProduitsService, Produit } from '../../services/produits.service';
import { StocksService, Stock } from '../../services/stocks.service';
import { AlertService } from '../../nativeComp/alert/alert.service';
import { ToastService } from '../../nativeComp/toast/toast.service';
import { AuthService } from 'src/app/services/auth.service';

interface ProduitStock {
  nom: string;
  quantite: number;
  /** Quantité en cession (incluse dans quantite) */
  quantityCession?: number;
  unite: string;
  niveau: 'optimal' | 'faible' | 'critique';
}

interface DepotDisplay extends Depot {
  stockActuel?: number;
  produits?: ProduitStock[];
  dateCreation?: string;
  couleur: string;
}

@Component({
  selector: 'app-depot',
  templateUrl: './depot.component.html',
  styleUrls: ['./depot.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class DepotComponent implements OnInit {
  searchTerm: string = '';
  activeFilter: string = 'tous';
  showAddModal: boolean = false;
  showEditModal: boolean = false;
  showApprovisionnementModal: boolean = false;
  showSortieModal: boolean = false;
  depotPourApprovisionnement: DepotDisplay | null = null;
  depotPourSortie: DepotDisplay | null = null;
  selectedDepotForEdit: Depot | null = null;
  editDepotData: Partial<Depot> = {};
  isLoading: boolean = false;
  depots: DepotDisplay[] = [];
  couleurs = ['blue', 'green', 'purple', 'orange', 'red', 'teal', 'pink', 'indigo'];
  couleurIndex = 0;
  stocksDepot: Stock[] = [];
  isLogistique: boolean = false;
  isAdmin: boolean = false;

  // Approvisionnement
  produitsDisponibles: Produit[] = [];
  produitApprovisionnement: {
    produitId: number | undefined;
    quantite: number;
    prixUnitaire?: number;
    seuilMinimum?: number;
  } = {
    produitId: undefined,
    quantite: 0,
    prixUnitaire: 0,
    seuilMinimum: 0
  };

  // Sortie de stock
  produitSortie: {
    stockId: number | undefined;
    quantite: number;
  } = {
    stockId: undefined,
    quantite: 0
  };

  newDepot: Partial<Depot> = {
    nom: '',
    ville: '',
    adresse: '',
    telephone: '',
    responsable: '',
    capacite: 0,
    statut: 'ACTIF'
  };

  stats = {
    total: 0,
    actifs: 0,
    inactifs: 0,
    capaciteTotale: 0,
    stockTotal: 0
  };

  constructor(
    private depotsService: DepotsService,
    private produitsService: ProduitsService,
    private stocksService: StocksService,
    private alertService: AlertService,
    private toastService: ToastService,
    private authService: AuthService
  ) { }

  ngOnInit() {
    this.loadDepots();
    this.loadProduits();
    this.isLogistique = this.hasDepotAccess();
    this.isAdmin = this.authService.hasRole('ROLE_ADMIN');
  }

  hasDepotAccess(): boolean {
    return this.authService.hasRole('ROLE_COMPTABLE') || this.authService.hasRole('ROLE_ADMIN');
  }

  loadProduits() {
    this.produitsService.getAllProduits().subscribe({
      next: (data) => {
        this.produitsDisponibles = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des produits:', error);
      }
    });
  }

  loadDepots() {
    this.isLoading = true;
    this.couleurIndex = 0;
    this.depotsService.getAllDepots().subscribe({
      next: (data) => {
        this.depots = data.map(d => ({
          ...d,
          stockActuel: d.capaciteUtilisee || 0,
          produits: [], // Les produits seront chargés après
          dateCreation: new Date().toLocaleDateString('fr-FR', {
            day: 'numeric',
            month: 'long',
            year: 'numeric'
          }),
          couleur: this.getNextCouleur(),
          statut: (d.statut || 'ACTIF') as 'ACTIF' | 'INACTIF' | 'EN_MAINTENANCE' | 'PLEIN'
        }));

        // Charger les stocks pour chaque dépôt
        this.loadStocksForDepots();
        this.updateStats();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des dépôts:', error);
        this.isLoading = false;
      }
    });
  }

  loadStocksForDepots() {
    this.depots.forEach(depot => {
      if (depot.id) {
        this.stocksService.getStocksByDepot(depot.id).subscribe({
          next: (stocks) => {
            // Grouper les stocks par type de produit
            const stocksByType = this.groupStocksByTypeProduit(stocks);
            const produits: ProduitStock[] = stocksByType.map(group => ({
              nom: group.typeProduit || 'Autre',
              quantite: group.totalQuantite,
              quantityCession: group.totalCession > 0 ? group.totalCession : undefined,
              unite: group.unite || 'L',
              niveau: this.getNiveauStockFromQuantite(group.totalQuantite, group.seuilMinimum)
            }));

            // Mettre à jour le dépôt avec les produits
            const depotIndex = this.depots.findIndex(d => d.id === depot.id);
            if (depotIndex !== -1) {
              this.depots[depotIndex].produits = produits;
            }
          },
          error: (error) => {
            console.error(`Erreur lors du chargement des stocks pour le dépôt ${depot.id}:`, error);
          }
        });
      }
    });
  }

  groupStocksByTypeProduit(stocks: Stock[]): Array<{
    typeProduit: string;
    totalQuantite: number;
    totalCession: number;
    unite: string;
    seuilMinimum?: number;
  }> {
    const grouped = new Map<string, {
      typeProduit: string;
      totalQuantite: number;
      totalCession: number;
      unite: string;
      seuilMinimum?: number;
    }>();

    stocks.forEach(stock => {
      const typeProduit = stock.typeProduit || 'AUTRE';
      const qte = stock.quantite || 0;
      const cession = stock.quantityCession || 0;
      const total = qte + cession;
      const existing = grouped.get(typeProduit);

      if (existing) {
        existing.totalQuantite += total;
        existing.totalCession += cession;
        if (stock.seuilMinimum && (!existing.seuilMinimum || stock.seuilMinimum < existing.seuilMinimum)) {
          existing.seuilMinimum = stock.seuilMinimum;
        }
      } else {
        grouped.set(typeProduit, {
          typeProduit: typeProduit,
          totalQuantite: total,
          totalCession: cession,
          unite: stock.unite || 'L',
          seuilMinimum: stock.seuilMinimum
        });
      }
    });

    return Array.from(grouped.values());
  }

  getNiveauStockFromQuantite(quantite: number, seuilMinimum?: number): 'optimal' | 'faible' | 'critique' {
    if (!seuilMinimum) return 'optimal';
    if (quantite < seuilMinimum * 0.5) return 'critique';
    if (quantite < seuilMinimum * 1.2) return 'faible';
    return 'optimal';
  }

  /** Quantité restante (capacité non utilisée) du dépôt. Sans argument, utilise depotPourApprovisionnement. */
  getCapaciteRestante(depot?: DepotDisplay | null): number {
    const d = depot ?? this.depotPourApprovisionnement ?? null;
    if (!d) return 0;
    const cap = d.capacite || 0;
    const utilisee = d.stockActuel ?? d.capaciteUtilisee ?? 0;
    return Math.max(0, cap - utilisee);
  }

  getNextCouleur(): string {
    const couleur = this.couleurs[this.couleurIndex % this.couleurs.length];
    this.couleurIndex++;
    return couleur;
  }

  updateStats() {
    this.stats.total = this.depots.length;
    this.stats.actifs = this.depots.filter(d => d.statut === 'ACTIF').length;
    this.stats.inactifs = this.depots.filter(d => d.statut === 'INACTIF').length;
    this.stats.capaciteTotale = this.depots.reduce((sum, d) => sum + (d.capacite || 0), 0);
    this.stats.stockTotal = this.depots.reduce((sum, d) => sum + (d.stockActuel || 0), 0);
  }

  setFilter(filter: string) {
    this.activeFilter = filter;
  }

  get filteredDepots(): DepotDisplay[] {
    let filtered = this.depots;

    if (this.activeFilter === 'actifs') {
      filtered = filtered.filter(d => d.statut === 'ACTIF');
    } else if (this.activeFilter === 'inactifs') {
      filtered = filtered.filter(d => d.statut === 'INACTIF');
    } else if (this.activeFilter === 'maintenance') {
      filtered = filtered.filter(d => d.statut === 'EN_MAINTENANCE');
    }

    if (this.searchTerm) {
      filtered = filtered.filter(d =>
        d.nom.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        (d.ville && d.ville.toLowerCase().includes(this.searchTerm.toLowerCase())) ||
        (d.responsable && d.responsable.toLowerCase().includes(this.searchTerm.toLowerCase()))
      );
    }

    return filtered;
  }

  getPourcentageStock(depot: DepotDisplay): number {
    if (!depot.capacite || depot.capacite === 0) return 0;
    const utilisee = depot.stockActuel ?? depot.capaciteUtilisee ?? 0;
    return Math.min(100, Math.round((utilisee / depot.capacite) * 100));
  }



  getNiveauStock(stock: Stock): 'optimal' | 'faible' | 'critique' {
    if (!stock.seuilMinimum) return 'optimal';
    if (stock.quantite < stock.seuilMinimum * 0.5) return 'critique';
    if (stock.quantite < stock.seuilMinimum * 1.2) return 'faible';
    return 'optimal';
  }

  nouveauDepot() {
    this.newDepot = {
      nom: '',
      ville: '',
      adresse: '',
      telephone: '',
      responsable: '',
      capacite: 0,
      statut: 'ACTIF'
    };
    this.showAddModal = true;
  }

  closeAddModal() {
    this.showAddModal = false;
    this.newDepot = {
      nom: '',
      ville: '',
      adresse: '',
      telephone: '',
      responsable: '',
      capacite: 0,
      statut: 'ACTIF'
    };
  }

  addDepot() {
    if (!this.validateDepot()) {
      return;
    }

    this.isLoading = true;
    const depot: Depot = {
      nom: this.newDepot.nom!,
      adresse: this.newDepot.adresse!,
      ville: this.newDepot.ville,
      telephone: this.newDepot.telephone,
      responsable: this.newDepot.responsable,
      capacite: this.newDepot.capacite!,
      statut: this.newDepot.statut || 'ACTIF',
      capaciteUtilisee: 0
    };

    this.depotsService.createDepot(depot).subscribe({
      next: (newDepot) => {
        this.isLoading = false;
        this.toastService.success('Dépôt ajouté avec succès!');
        this.closeAddModal();
        this.loadDepots();
      },
      error: (error) => {
        console.error('Erreur lors de l\'ajout du dépôt:', error);
        this.toastService.error('Erreur lors de l\'ajout du dépôt. Veuillez réessayer.');
        this.isLoading = false;
      }
    });
  }

  validateDepot(): boolean {
    if (!this.newDepot.nom || this.newDepot.nom.trim() === '') {
      this.toastService.warning('Veuillez saisir le nom du dépôt');
      return false;
    }
    if (!this.newDepot.ville || this.newDepot.ville.trim() === '') {
      this.toastService.warning('Veuillez saisir la ville du dépôt');
      return false;
    }
    if (!this.newDepot.adresse || this.newDepot.adresse.trim() === '') {
      this.toastService.warning('Veuillez saisir l\'adresse du dépôt');
      return false;
    }
    if (!this.newDepot.telephone || this.newDepot.telephone.trim() === '') {
      this.toastService.warning('Veuillez saisir le téléphone du dépôt');
      return false;
    }
    if (!this.newDepot.responsable || this.newDepot.responsable.trim() === '') {
      this.toastService.warning('Veuillez saisir le responsable du dépôt');
      return false;
    }
    if (!this.newDepot.capacite || this.newDepot.capacite <= 0) {
      this.toastService.warning('Veuillez saisir une capacité valide (supérieure à 0)');
      return false;
    }
    return true;
  }

  editDepot(depot: DepotDisplay) {
    // Récupérer les données complètes depuis l'API pour l'édition
    if (depot.id) {
      this.depotsService.getDepotById(depot.id).subscribe({
        next: (depotAPI: Depot) => {
          this.selectedDepotForEdit = depotAPI;
          this.editDepotData = {
            nom: depotAPI.nom,
            ville: depotAPI.ville,
            adresse: depotAPI.adresse,
            telephone: depotAPI.telephone,
            responsable: depotAPI.responsable,
            capacite: depotAPI.capacite,
            statut: depotAPI.statut || 'ACTIF'
          };
          this.showEditModal = true;
        },
        error: (error) => {
          console.error('Erreur lors du chargement du dépôt:', error);
          this.toastService.error('Erreur lors du chargement des données du dépôt');
        }
      });
    }
  }

  closeEditModal() {
    this.showEditModal = false;
    this.selectedDepotForEdit = null;
    this.editDepotData = {};
  }

  updateDepot() {
    if (!this.validateEditDepot()) {
      return;
    }

    if (!this.selectedDepotForEdit || !this.selectedDepotForEdit.id) {
      this.toastService.error('Erreur: ID du dépôt manquant');
      return;
    }

    this.isLoading = true;

    const depotToUpdate: Depot = {
      id: this.selectedDepotForEdit.id,
      nom: this.editDepotData.nom!.trim(),
      ville: this.editDepotData.ville?.trim() || undefined,
      adresse: this.editDepotData.adresse!.trim(),
      telephone: this.editDepotData.telephone?.trim() || undefined,
      responsable: this.editDepotData.responsable?.trim() || undefined,
      capacite: this.editDepotData.capacite!,
      statut: this.editDepotData.statut || 'ACTIF',
      capaciteUtilisee: this.selectedDepotForEdit.capaciteUtilisee || 0
    };

    this.depotsService.updateDepot(this.selectedDepotForEdit.id, depotToUpdate).subscribe({
      next: () => {
        this.isLoading = false;
        this.toastService.success('Dépôt mis à jour avec succès!');
        this.closeEditModal();
        this.loadDepots();
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour du dépôt:', error);
        this.isLoading = false;
        const errorMessage = error.error?.message || error.message || 'Erreur lors de la mise à jour du dépôt';
        this.toastService.error(errorMessage);
      }
    });
  }

  validateEditDepot(): boolean {
    if (!this.editDepotData.nom || this.editDepotData.nom.trim() === '') {
      this.toastService.warning('Veuillez saisir le nom du dépôt');
      return false;
    }
    if (!this.editDepotData.ville || this.editDepotData.ville.trim() === '') {
      this.toastService.warning('Veuillez saisir la ville du dépôt');
      return false;
    }
    if (!this.editDepotData.adresse || this.editDepotData.adresse.trim() === '') {
      this.toastService.warning('Veuillez saisir l\'adresse du dépôt');
      return false;
    }
    if (!this.editDepotData.telephone || this.editDepotData.telephone.trim() === '') {
      this.toastService.warning('Veuillez saisir le téléphone du dépôt');
      return false;
    }
    if (!this.editDepotData.responsable || this.editDepotData.responsable.trim() === '') {
      this.toastService.warning('Veuillez saisir le responsable du dépôt');
      return false;
    }
    if (!this.editDepotData.capacite || this.editDepotData.capacite <= 0) {
      this.toastService.warning('Veuillez saisir une capacité valide (supérieure à 0)');
      return false;
    }
    // Vérifier que la nouvelle capacité n'est pas inférieure à la capacité utilisée
    if (this.selectedDepotForEdit && this.editDepotData.capacite < (this.selectedDepotForEdit.capaciteUtilisee || 0)) {
      this.toastService.warning(`La capacité ne peut pas être inférieure à la capacité utilisée (${this.selectedDepotForEdit.capaciteUtilisee || 0} L)`);
      return false;
    }
    return true;
  }

  deleteDepot(depot: DepotDisplay) {
    this.alertService.confirm(
      `Êtes-vous sûr de vouloir supprimer le dépôt ${depot.nom} ?`,
      'Confirmation de suppression'
    ).subscribe(confirmed => {
      if (!confirmed) return;

      if (!depot.id) {
        this.toastService.error('Erreur: ID du dépôt manquant');
        return;
      }

      this.depotsService.deleteDepot(depot.id).subscribe({
        next: () => {
          this.toastService.success('Dépôt supprimé avec succès');
          this.loadDepots();
        },
        error: (error) => {
          console.error('Erreur lors de la suppression:', error);
          this.toastService.error('Erreur lors de la suppression du dépôt');
        }
      });
    });
  }

  approvisionnerDepot(depot: DepotDisplay) {
    this.depotPourApprovisionnement = depot;
    this.produitApprovisionnement = {
      produitId: undefined,
      quantite: 0,
      prixUnitaire: 0,
      seuilMinimum: 0
    };
    this.showApprovisionnementModal = true;
  }

  closeApprovisionnementModal() {
    this.showApprovisionnementModal = false;
    this.depotPourApprovisionnement = null;
    this.produitApprovisionnement = {
      produitId: undefined,
      quantite: 0,
      prixUnitaire: 0,
      seuilMinimum: 0
    };
  }

  canAddProduit(): boolean {
    if (!this.produitApprovisionnement.produitId || !this.produitApprovisionnement.quantite) {
      return false;
    }
    const capaciteRestante = this.getCapaciteRestante();
    return this.produitApprovisionnement.quantite <= capaciteRestante;
  }

  getProduitUnite(produitId: number | undefined): string {
    if (!produitId) return 'L';
    const produit = this.produitsDisponibles.find(p => p.id === produitId);
    if (!produit) return 'L';

    switch (produit.typeProduit) {
      case 'ESSENCE':
      case 'GAZOLE':
      case 'KEROSENE':
      case 'GPL':
        return 'L';
      default:
        return 'U';
    }
  }

  saveApprovisionnement() {
    if (!this.depotPourApprovisionnement) return;

    if (!this.validateApprovisionnement()) {
      return;
    }

    if (!this.canAddProduit()) {
      this.toastService.warning(`La quantité (${this.produitApprovisionnement.quantite} ${this.getProduitUnite(this.produitApprovisionnement.produitId)}) dépasse la capacité restante (${this.getCapaciteRestante()} L)`);
      return;
    }

    if (!this.produitApprovisionnement.produitId) {
      this.toastService.warning('Veuillez sélectionner un produit');
      return;
    }

    this.isLoading = true;

    const stockToCreate: Stock = {
      produitId: this.produitApprovisionnement.produitId,
      quantite: this.produitApprovisionnement.quantite,
      depotId: this.depotPourApprovisionnement.id!,
      seuilMinimum: this.produitApprovisionnement.seuilMinimum || undefined,
      prixUnitaire: this.produitApprovisionnement.prixUnitaire || undefined,
      unite: this.getProduitUnite(this.produitApprovisionnement.produitId)
    };

    this.stocksService.createStock(stockToCreate).subscribe({
      next: () => {
        this.isLoading = false;
        this.toastService.success('Produit ajouté au dépôt avec succès!');
        this.closeApprovisionnementModal();
        this.loadDepots();
      },
      error: (error) => {
        console.error('Erreur lors de l\'ajout du stock:', error);
        this.isLoading = false;
        this.toastService.error('Erreur lors de l\'ajout du produit au dépôt. Veuillez réessayer.');
      }
    });
  }

  validateApprovisionnement(): boolean {
    if (!this.produitApprovisionnement.produitId) {
      this.toastService.warning('Veuillez sélectionner un produit');
      return false;
    }
    if (!this.produitApprovisionnement.quantite || this.produitApprovisionnement.quantite <= 0) {
      this.toastService.warning('Veuillez saisir une quantité valide (supérieure à 0)');
      return false;
    }
    return true;
  }

  // Sortie de stock
  sortirStock(depot: DepotDisplay) {
    this.depotPourSortie = depot;
    this.produitSortie = {
      stockId: undefined,
      quantite: 0
    };
    // Charger les stocks du dépôt pour le modal
    this.stocksService.getStocksByDepot(depot.id!).subscribe({
      next: (stocks) => {
        this.stocksDepot = stocks;
        this.showSortieModal = true;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des stocks:', error);
        this.toastService.error('Erreur lors du chargement des stocks du dépôt');
      }
    });
  }

  closeSortieModal() {
    this.showSortieModal = false;
    this.depotPourSortie = null;
    this.produitSortie = {
      stockId: undefined,
      quantite: 0
    };
  }

  getStockSelected(): Stock | undefined {
    if (!this.produitSortie.stockId) return undefined;
    // Convertir en number pour la comparaison (le select peut retourner une string)
    const stockId = typeof this.produitSortie.stockId === 'string'
      ? Number(this.produitSortie.stockId)
      : this.produitSortie.stockId;
    return this.stocksDepot.find(s => s.id === stockId);
  }

  getQuantiteDisponible(): number {
    const stock = this.getStockSelected();
    return stock ? stock.quantite : 0;
  }

  canSortirStock(): boolean {
    if (!this.produitSortie.stockId || !this.produitSortie.quantite) {
      return false;
    }
    const stock = this.getStockSelected();
    if (!stock) return false;
    return this.produitSortie.quantite > 0 && this.produitSortie.quantite <= stock.quantite;
  }

  saveSortie() {
    if (!this.depotPourSortie) return;

    if (!this.validateSortie()) {
      return;
    }

    const stock = this.getStockSelected();
    if (!stock || !stock.id) {
      this.toastService.error('Stock non trouvé');
      return;
    }

    if (this.produitSortie.quantite > stock.quantite) {
      this.toastService.warning(`La quantité à sortir (${this.produitSortie.quantite}) dépasse la quantité disponible (${stock.quantite})`);
      return;
    }

    this.isLoading = true;

    // Calculer la nouvelle quantité
    const nouvelleQuantite = stock.quantite - this.produitSortie.quantite;

    const stockToUpdate: Stock = {
      id: stock.id,
      produitId: stock.produitId,
      quantite: nouvelleQuantite,
      depotId: stock.depotId,
      seuilMinimum: stock.seuilMinimum,
      prixUnitaire: stock.prixUnitaire,
      unite: stock.unite
    };

    this.stocksService.updateStock(stock.id, stockToUpdate).subscribe({
      next: () => {
        this.isLoading = false;
        this.toastService.success(`Sortie de stock enregistrée avec succès! ${this.produitSortie.quantite} ${stock.unite || 'L'} sorti(s).`);
        this.closeSortieModal();
        this.loadDepots();
      },
      error: (error) => {
        console.error('Erreur lors de la sortie de stock:', error);
        this.isLoading = false;
        this.toastService.error('Erreur lors de la sortie de stock. Veuillez réessayer.');
      }
    });
  }

  validateSortie(): boolean {
    if (!this.produitSortie.stockId) {
      this.toastService.warning('Veuillez sélectionner un produit');
      return false;
    }
    if (!this.produitSortie.quantite || this.produitSortie.quantite <= 0) {
      this.toastService.warning('Veuillez saisir une quantité valide (supérieure à 0)');
      return false;
    }
    const stock = this.getStockSelected();
    if (!stock) {
      this.toastService.error('Stock non trouvé');
      return false;
    }
    if (this.produitSortie.quantite > stock.quantite) {
      this.toastService.warning(`La quantité à sortir ne peut pas dépasser la quantité disponible (${stock.quantite} ${stock.unite || 'L'})`);
      return false;
    }
    return true;
  }
}
