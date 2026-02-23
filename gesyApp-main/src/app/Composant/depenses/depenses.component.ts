import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DepensesService, Depense, CategorieDepense, DepensePage } from '../../services/depenses.service';
import { ComptesBancairesService, CompteBancaire } from '../../services/comptes-bancaires.service';
import { CaissesService, Caisse } from '../../services/caisses.service';
import { ToastService } from '../../nativeComp/toast/toast.service';
import { AlertService } from '../../nativeComp/alert/alert.service';

@Component({
  selector: 'app-depenses',
  templateUrl: './depenses.component.html',
  styleUrls: ['./depenses.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class DepensesComponent implements OnInit {
  // Data
  depenses: Depense[] = [];
  categories: CategorieDepense[] = [];
  comptes: CompteBancaire[] = [];
  caisses: Caisse[] = [];
  
  // Pagination
  currentPage: number = 0;
  pageSize: number = 10;
  totalElements: number = 0;
  totalPages: number = 0;
  
  // Filters
  searchTerm: string = '';
  selectedCategorieId: number | null = null;
  filterType: 'none' | 'date' | 'range' = 'none';
  filterDate: string = '';
  filterStartDate: string = '';
  filterEndDate: string = '';
  
  // UI State
  isLoading: boolean = false;
  activeTab: 'depenses' | 'categories' = 'depenses';
  
  // Modals
  showDepenseModal: boolean = false;
  showCategorieModal: boolean = false;
  showDeleteConfirm: boolean = false;
  
  // Form data
  isEditingDepense: boolean = false;
  isEditingCategorie: boolean = false;
  
  /** 'compte' | 'caisse' pour afficher le bon select (création obligatoire). */
  sourceType: 'compte' | 'caisse' | null = null;

  depenseForm: Partial<Depense> = {
    libelle: '',
    montant: 0,
    dateDepense: new Date().toISOString().split('T')[0],
    categorieId: undefined,
    description: '',
    reference: '',
    compteId: undefined,
    caisseId: undefined
  };
  
  categorieForm: Partial<CategorieDepense> = {
    nom: '',
    description: '',
    statut: 'ACTIF'
  };
  
  itemToDelete: { type: 'depense' | 'categorie', id: number } | null = null;
  
  // Stats
  totalDepenses: number = 0;

  constructor(
    private depensesService: DepensesService,
    private comptesBancairesService: ComptesBancairesService,
    private caissesService: CaissesService,
    private toastService: ToastService,
    private alertService: AlertService
  ) {}

  ngOnInit() {
    this.loadCategories();
    this.loadComptes();
    this.loadCaisses();
    this.loadDepenses();
  }

  loadComptes() {
    this.comptesBancairesService.getAllComptes().subscribe({
      next: (data) => this.comptes = data,
      error: () => this.toastService.error('Erreur chargement des comptes bancaires')
    });
  }

  loadCaisses() {
    this.caissesService.getAllCaisses().subscribe({
      next: (data) => this.caisses = data,
      error: () => this.toastService.error('Erreur chargement des caisses')
    });
  }

  // === DATA LOADING ===
  loadCategories() {
    this.depensesService.getCategories().subscribe({
      next: (data) => {
        this.categories = data;
      },
      error: (err) => {
        console.error('Erreur chargement catégories:', err);
        this.toastService.error('Erreur lors du chargement des catégories');
      }
    });
  }

  loadDepenses() {
    this.isLoading = true;
    
    let request;
    
    if (this.filterType === 'date' && this.filterDate) {
      if (this.selectedCategorieId) {
        request = this.depensesService.getDepensesByCategorieAndDate(
          this.selectedCategorieId, this.filterDate, this.currentPage, this.pageSize
        );
      } else {
        request = this.depensesService.getDepensesByDate(this.filterDate, this.currentPage, this.pageSize);
      }
    } else if (this.filterType === 'range' && this.filterStartDate && this.filterEndDate) {
      if (this.selectedCategorieId) {
        request = this.depensesService.getDepensesByCategorieAndDateRange(
          this.selectedCategorieId, this.filterStartDate, this.filterEndDate, this.currentPage, this.pageSize
        );
      } else {
        request = this.depensesService.getDepensesByDateRange(
          this.filterStartDate, this.filterEndDate, this.currentPage, this.pageSize
        );
      }
    } else if (this.selectedCategorieId) {
      request = this.depensesService.getDepensesByCategorie(this.selectedCategorieId, this.currentPage, this.pageSize);
    } else {
      request = this.depensesService.getDepenses(this.currentPage, this.pageSize);
    }
    
    request.subscribe({
      next: (data: DepensePage) => {
        this.depenses = data.depenses;
        this.totalElements = data.totalElements;
        this.totalPages = data.totalPages;
        this.calculateTotalDepenses();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Erreur chargement dépenses:', err);
        this.toastService.error('Erreur lors du chargement des dépenses');
        this.isLoading = false;
      }
    });
  }

  calculateTotalDepenses() {
    this.totalDepenses = this.depenses.reduce((sum, d) => sum + (d.montant || 0), 0);
  }

  // === FILTERS ===
  onFilterChange() {
    this.currentPage = 0;
    this.loadDepenses();
  }

  clearFilters() {
    this.selectedCategorieId = null;
    this.filterType = 'none';
    this.filterDate = '';
    this.filterStartDate = '';
    this.filterEndDate = '';
    this.searchTerm = '';
    this.currentPage = 0;
    this.loadDepenses();
  }

  get filteredDepenses(): Depense[] {
    if (!this.searchTerm.trim()) return this.depenses;
    
    const term = this.searchTerm.toLowerCase();
    return this.depenses.filter(d => 
      d.libelle?.toLowerCase().includes(term) ||
      d.categorieNom?.toLowerCase().includes(term) ||
      d.reference?.toLowerCase().includes(term) ||
      d.description?.toLowerCase().includes(term)
    );
  }

  // === PAGINATION ===
  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadDepenses();
    }
  }

  get pages(): number[] {
    const pages: number[] = [];
    const maxVisiblePages = 5;
    let start = Math.max(0, this.currentPage - Math.floor(maxVisiblePages / 2));
    let end = Math.min(this.totalPages, start + maxVisiblePages);
    
    if (end - start < maxVisiblePages) {
      start = Math.max(0, end - maxVisiblePages);
    }
    
    for (let i = start; i < end; i++) {
      pages.push(i);
    }
    return pages;
  }

  // === DEPENSE CRUD ===
  openAddDepenseModal() {
    this.isEditingDepense = false;
    this.sourceType = null;
    this.depenseForm = {
      libelle: '',
      montant: 0,
      dateDepense: new Date().toISOString().split('T')[0],
      categorieId: undefined,
      description: '',
      reference: '',
      compteId: undefined,
      caisseId: undefined
    };
    this.showDepenseModal = true;
  }

  openEditDepenseModal(depense: Depense) {
    this.isEditingDepense = true;
    this.depenseForm = {
      ...depense,
      dateDepense: depense.dateDepense ? depense.dateDepense.split('T')[0] : '',
      compteId: depense.compteId,
      caisseId: depense.caisseId
    };
    this.sourceType = depense.compteId != null ? 'compte' : depense.caisseId != null ? 'caisse' : null;
    this.showDepenseModal = true;
  }

  closeDepenseModal() {
    this.showDepenseModal = false;
    this.depenseForm = {};
  }

  saveDepense() {
    if (!this.depenseForm.libelle || !this.depenseForm.montant || !this.depenseForm.categorieId) {
      this.toastService.error('Veuillez remplir tous les champs obligatoires');
      return;
    }
    if (!this.isEditingDepense) {
      const hasCompte = this.depenseForm.compteId != null;
      const hasCaisse = this.depenseForm.caisseId != null;
      if (hasCompte === hasCaisse) {
        this.toastService.error('Veuillez sélectionner soit un compte bancaire, soit une caisse pour déduire le montant');
        return;
      }
    }

    const depenseData: Depense = {
      ...this.depenseForm as Depense,
      dateDepense: this.depenseForm.dateDepense + 'T00:00:00',
      compteId: this.depenseForm.compteId ?? undefined,
      caisseId: this.depenseForm.caisseId ?? undefined
    };

    if (this.isEditingDepense && this.depenseForm.id) {
      this.depensesService.updateDepense(this.depenseForm.id, depenseData).subscribe({
        next: () => {
          this.toastService.success('Dépense modifiée avec succès');
          this.closeDepenseModal();
          this.loadDepenses();
        },
        error: (err) => {
          console.error('Erreur modification:', err);
          this.toastService.error('Erreur lors de la modification');
        }
      });
    } else {
      this.depensesService.createDepense(depenseData).subscribe({
        next: () => {
          this.toastService.success('Dépense ajoutée avec succès');
          this.closeDepenseModal();
          this.loadDepenses();
        },
        error: (err) => {
          console.error('Erreur création:', err);
          this.toastService.error('Erreur lors de la création');
        }
      });
    }
  }

  // === CATEGORIE CRUD ===
  openAddCategorieModal() {
    this.isEditingCategorie = false;
    this.categorieForm = {
      nom: '',
      description: '',
      statut: 'ACTIF'
    };
    this.showCategorieModal = true;
  }

  openEditCategorieModal(categorie: CategorieDepense) {
    this.isEditingCategorie = true;
    this.categorieForm = { ...categorie };
    this.showCategorieModal = true;
  }

  closeCategorieModal() {
    this.showCategorieModal = false;
    this.categorieForm = {};
  }

  saveCategorie() {
    if (!this.categorieForm.nom) {
      this.toastService.error('Le nom de la catégorie est obligatoire');
      return;
    }

    if (this.isEditingCategorie && this.categorieForm.id) {
      this.depensesService.updateCategorie(this.categorieForm.id, this.categorieForm as CategorieDepense).subscribe({
        next: () => {
          this.toastService.success('Catégorie modifiée avec succès');
          this.closeCategorieModal();
          this.loadCategories();
        },
        error: (err) => {
          console.error('Erreur modification:', err);
          this.toastService.error('Erreur lors de la modification');
        }
      });
    } else {
      this.depensesService.createCategorie(this.categorieForm as CategorieDepense).subscribe({
        next: () => {
          this.toastService.success('Catégorie ajoutée avec succès');
          this.closeCategorieModal();
          this.loadCategories();
        },
        error: (err) => {
          console.error('Erreur création:', err);
          this.toastService.error('Erreur lors de la création');
        }
      });
    }
  }

  // === DELETE ===
  confirmDelete(type: 'depense' | 'categorie', id: number) {
    this.itemToDelete = { type, id };
    this.showDeleteConfirm = true;
  }

  cancelDelete() {
    this.itemToDelete = null;
    this.showDeleteConfirm = false;
  }

  executeDelete() {
    if (!this.itemToDelete) return;

    if (this.itemToDelete.type === 'depense') {
      this.depensesService.deleteDepense(this.itemToDelete.id).subscribe({
        next: () => {
          this.toastService.success('Dépense supprimée avec succès');
          this.loadDepenses();
        },
        error: (err) => {
          console.error('Erreur suppression:', err);
          this.toastService.error('Erreur lors de la suppression');
        }
      });
    } else {
      this.depensesService.deleteCategorie(this.itemToDelete.id).subscribe({
        next: () => {
          this.toastService.success('Catégorie supprimée avec succès');
          this.loadCategories();
        },
        error: (err) => {
          console.error('Erreur suppression:', err);
          this.toastService.error('Erreur lors de la suppression');
        }
      });
    }

    this.cancelDelete();
  }

  // === HELPERS ===
  formatDate(dateString: string): string {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  formatMontant(montant: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'XOF',
      minimumFractionDigits: 0
    }).format(montant || 0);
  }

  getCategorieNom(categorieId: number): string {
    const cat = this.categories.find(c => c.id === categorieId);
    return cat ? cat.nom : '-';
  }

  getCategorieColor(categorieNom: string): string {
    const colors: { [key: string]: string } = {
      'Carburant': '#3b82f6',
      'Maintenance': '#f59e0b',
      'Salaires': '#10b981',
      'Loyer': '#8b5cf6',
      'Fournitures': '#ec4899',
      'Transport': '#06b6d4',
      'Autres': '#6b7280'
    };
    return colors[categorieNom] || '#6b7280';
  }

  getSourceLabel(depense: Depense): string {
    if (depense.compteId != null) {
      const c = this.comptes.find(x => x.id === depense.compteId);
      return c ? `${c.banque} - ${c.numero}` : `Compte #${depense.compteId}`;
    }
    if (depense.caisseId != null) {
      const c = this.caisses.find(x => x.id === depense.caisseId);
      return c ? c.nom : `Caisse #${depense.caisseId}`;
    }
    return '-';
  }

  onSourceTypeChange() {
    this.depenseForm.compteId = undefined;
    this.depenseForm.caisseId = undefined;
  }
}

