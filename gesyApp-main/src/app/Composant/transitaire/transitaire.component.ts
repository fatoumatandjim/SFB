import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TransitairesService, Transitaire, TransitairePage } from '../../services/transitaires.service';
import { AlertService } from '../../nativeComp/alert/alert.service';
import { ToastService } from '../../nativeComp/toast/toast.service';

interface Voyage {
  id: string;
  numero: string;
  origine: string;
  destination: string;
  date: string;
  statut: 'en-cours' | 'termine' | 'annule';
  camion: string;
}

interface TransitaireDisplay extends Transitaire {
  voyagesAttribues?: number;
  voyages?: Voyage[];
  dateCreation?: string;
  couleur: string;
}

@Component({
  selector: 'app-transitaire',
  templateUrl: './transitaire.component.html',
  styleUrls: ['./transitaire.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class TransitaireComponent implements OnInit {
  searchTerm: string = '';
  activeFilter: string = 'tous';
  selectedTransitaire: TransitaireDisplay | null = null;
  showPassword: { [key: string]: boolean } = {};
  showAddModal: boolean = false;
  showDetailModal: boolean = false;
  showEditModal: boolean = false;
  selectedTransitaireForEdit: Transitaire | null = null;
  editTransitaireData: Partial<Transitaire> = {};
  isLoading: boolean = false;
  transitaires: TransitaireDisplay[] = [];
  couleurs = ['blue', 'green', 'purple', 'orange', 'red', 'teal', 'pink', 'indigo'];
  couleurIndex = 0;

  // Pagination
  transitairesPage: TransitairePage | null = null;
  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;

  newTransitaire: Partial<Transitaire> = {
    nom: '',
    email: '',
    telephone: '',
    statut: 'ACTIF'
  };

  stats = {
    total: 0,
    actifs: 0,
    inactifs: 0,
    voyagesTotal: 0
  };

  constructor(
    private transitairesService: TransitairesService,
    private alertService: AlertService,
    private toastService: ToastService,
    private router: Router
  ) { }

  ngOnInit() {
    this.loadTransitaires();
  }

  loadTransitaires() {
    this.isLoading = true;
    this.couleurIndex = 0; // Reset color index
    this.transitairesService.getAllTransitairesPaginated(this.currentPage, this.pageSize).subscribe({
      next: (data: TransitairePage) => {
        this.transitairesPage = data;
        this.totalPages = data.totalPages;
        this.totalElements = data.totalElements;
        this.transitaires = data.transitaires.map(t => ({
          ...t,
          voyagesAttribues: t.nombreVoyages || 0,
          voyages: [], // Les voyages seront chargés séparément si nécessaire
          dateCreation: new Date().toLocaleDateString('fr-FR', {
            day: 'numeric',
            month: 'long',
            year: 'numeric'
          }),
          couleur: this.getNextCouleur(),
          statut: (t.statut || 'ACTIF') as 'ACTIF' | 'INACTIF' | 'SUSPENDU'
        }));
        this.updateStats();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des transitaires:', error);
        this.isLoading = false;
      }
    });
  }

  changePage(page: number) {
    this.currentPage = page;
    this.loadTransitaires();
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxPages = Math.min(this.totalPages, 5);
    const startPage = Math.max(0, Math.min(this.currentPage - 2, this.totalPages - maxPages));
    for (let i = 0; i < maxPages; i++) {
      pages.push(startPage + i);
    }
    return pages;
  }

  getNextCouleur(): string {
    const couleur = this.couleurs[this.couleurIndex % this.couleurs.length];
    this.couleurIndex++;
    return couleur;
  }

  updateStats() {
    this.stats.total = this.transitaires.length;
    this.stats.actifs = this.transitaires.filter(t => t.statut === 'ACTIF').length;
    this.stats.inactifs = this.transitaires.filter(t => t.statut === 'INACTIF').length;
    this.stats.voyagesTotal = this.transitaires.reduce((sum, t) => sum + (t.voyagesAttribues || 0), 0);
  }

  setFilter(filter: string) {
    this.activeFilter = filter;
  }

  get filteredTransitaires(): TransitaireDisplay[] {
    let filtered = this.transitaires;

    if (this.activeFilter === 'actifs') {
      filtered = filtered.filter(t => t.statut === 'ACTIF');
    } else if (this.activeFilter === 'inactifs') {
      filtered = filtered.filter(t => t.statut === 'INACTIF');
    }

    if (this.searchTerm) {
      filtered = filtered.filter(t =>
        t.nom.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        t.email.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        (t.identifiant && t.identifiant.toLowerCase().includes(this.searchTerm.toLowerCase())) ||
        t.telephone.includes(this.searchTerm)
      );
    }

    return filtered;
  }

  togglePassword(transitaireId: string) {
    this.showPassword[transitaireId] = !this.showPassword[transitaireId];
  }

  getMaskedPassword(password: string | undefined): string {
    if (!password) return '';
    return '•'.repeat(password.length);
  }

  viewVoyages(transitaire: TransitaireDisplay) {
    this.selectedTransitaire = transitaire;
    // TODO: Charger les voyages réels depuis l'API si nécessaire
  }

  closeVoyagesModal() {
    this.selectedTransitaire = null;
  }

  nouveauTransitaire() {
    this.newTransitaire = {
      nom: '',
      email: '',
      telephone: '',
      statut: 'ACTIF'
    };
    this.showAddModal = true;
  }

  closeAddModal() {
    this.showAddModal = false;
    this.newTransitaire = {
      nom: '',
      email: '',
      telephone: '',
      statut: 'ACTIF'
    };
  }

  addTransitaire() {
    if (!this.validateTransitaire()) {
      return;
    }

    this.isLoading = true;
    const transitaire: Transitaire = {
      nom: this.newTransitaire.nom!,
      email: this.newTransitaire.email!,
      telephone: this.newTransitaire.telephone!,
      statut: this.newTransitaire.statut || 'ACTIF'
    };

    this.transitairesService.createTransitaire(transitaire).subscribe({
      next: (newTransitaire) => {
        this.isLoading = false;
        const message = `Transitaire ajouté avec succès!\nIdentifiant: ${newTransitaire.identifiant}\nMot de passe: ${newTransitaire.defaultPass || newTransitaire.motDePasse}`;
        this.alertService.success(message, 'Transitaire créé').subscribe();
        this.closeAddModal();
        this.loadTransitaires();
      },
      error: (error) => {
        console.error('Erreur lors de l\'ajout du transitaire:', error);
        this.toastService.error('Erreur lors de l\'ajout du transitaire. Veuillez réessayer.');
        this.isLoading = false;
      }
    });
  }

  validateTransitaire(): boolean {
    if (!this.newTransitaire.nom || this.newTransitaire.nom.trim() === '') {
      this.toastService.warning('Veuillez saisir le nom du transitaire');
      return false;
    }
    if (!this.newTransitaire.email || this.newTransitaire.email.trim() === '') {
      this.toastService.warning('Veuillez saisir l\'email du transitaire');
      return false;
    }
    if (!this.newTransitaire.telephone || this.newTransitaire.telephone.trim() === '') {
      this.toastService.warning('Veuillez saisir le téléphone du transitaire');
      return false;
    }
    return true;
  }

  viewTransitaire(transitaire: TransitaireDisplay) {
    if (transitaire.id != null) {
      this.router.navigate(['/transitaire-detail', transitaire.id]);
    } else {
      this.selectedTransitaire = transitaire;
      this.showDetailModal = true;
    }
  }

  closeDetailModal() {
    this.showDetailModal = false;
    this.selectedTransitaire = null;
    this.selectedTransitaireForEdit = null;
  }

  editTransitaire(transitaire: TransitaireDisplay) {
    // Récupérer les données complètes depuis l'API pour l'édition
    if (transitaire.id) {
      this.transitairesService.getTransitaireById(transitaire.id).subscribe({
        next: (transitaireAPI: Transitaire) => {
          this.selectedTransitaireForEdit = transitaireAPI;
          this.editTransitaireData = {
            nom: transitaireAPI.nom,
            email: transitaireAPI.email,
            telephone: transitaireAPI.telephone,
            statut: transitaireAPI.statut || 'ACTIF'
          };
          this.showEditModal = true;
        },
        error: (error) => {
          console.error('Erreur lors du chargement du transitaire:', error);
          this.toastService.error('Erreur lors du chargement des données du transitaire');
        }
      });
    }
  }

  closeEditModal() {
    this.showEditModal = false;
    this.selectedTransitaireForEdit = null;
    this.editTransitaireData = {};
  }

  updateTransitaire() {
    if (!this.validateEditTransitaire()) {
      return;
    }

    if (!this.selectedTransitaireForEdit || !this.selectedTransitaireForEdit.id) {
      this.toastService.error('Erreur: ID du transitaire manquant');
      return;
    }

    this.isLoading = true;

    const transitaireToUpdate: Transitaire = {
      id: this.selectedTransitaireForEdit.id,
      nom: this.editTransitaireData.nom!.trim(),
      email: this.editTransitaireData.email!.trim(),
      telephone: this.editTransitaireData.telephone!.trim(),
      statut: this.editTransitaireData.statut || 'ACTIF',
      identifiant: this.selectedTransitaireForEdit.identifiant
    };

    this.transitairesService.updateTransitaire(this.selectedTransitaireForEdit.id, transitaireToUpdate).subscribe({
      next: () => {
        this.isLoading = false;
        this.toastService.success('Transitaire mis à jour avec succès!');
        this.closeEditModal();
        this.loadTransitaires();
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour du transitaire:', error);
        this.isLoading = false;
        const errorMessage = error.error?.message || error.message || 'Erreur lors de la mise à jour du transitaire';
        this.toastService.error(errorMessage);
      }
    });
  }

  validateEditTransitaire(): boolean {
    if (!this.editTransitaireData.nom || this.editTransitaireData.nom.trim() === '') {
      this.toastService.warning('Veuillez saisir le nom du transitaire');
      return false;
    }
    if (!this.editTransitaireData.email || this.editTransitaireData.email.trim() === '') {
      this.toastService.warning('Veuillez saisir l\'email du transitaire');
      return false;
    }
    if (!this.editTransitaireData.telephone || this.editTransitaireData.telephone.trim() === '') {
      this.toastService.warning('Veuillez saisir le téléphone du transitaire');
      return false;
    }
    return true;
  }

  deleteTransitaire(transitaire: TransitaireDisplay) {
    this.alertService.confirm(
      `Êtes-vous sûr de vouloir supprimer le transitaire ${transitaire.nom} ?`,
      'Confirmation de suppression'
    ).subscribe(confirmed => {
      if (!confirmed) return;

      if (!transitaire.id) {
        this.toastService.error('Erreur: ID du transitaire manquant');
        return;
      }

      this.transitairesService.deleteTransitaire(transitaire.id).subscribe({
        next: () => {
          this.toastService.success('Transitaire supprimé avec succès');
          this.loadTransitaires();
        },
        error: (error) => {
          console.error('Erreur lors de la suppression:', error);
          this.toastService.error('Erreur lors de la suppression du transitaire');
        }
      });
    });
  }

  resetPassword(transitaire: TransitaireDisplay) {
    this.alertService.confirm(
      `Réinitialiser le mot de passe pour ${transitaire.nom} ?`,
      'Confirmation'
    ).subscribe(confirmed => {
      if (!confirmed) return;
      // TODO: Implémenter la réinitialisation du mot de passe via API
    });
  }
}
