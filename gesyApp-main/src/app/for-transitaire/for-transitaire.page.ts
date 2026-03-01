import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { VoyagesService, Voyage, VoyagePage, TransitaireStats } from '../services/voyages.service';
import { TransactionsService, Transaction } from '../services/transactions.service';
import { AuthService } from '../services/auth.service';
import { TransitairesService, Transitaire } from '../services/transitaires.service';
import { CamionsService, Camion } from '../services/camions.service';
import { PaysService, Pays } from '../services/pays.service';
import { AxesService, Axe } from '../services/axes.service';
import { AxesComponent } from '../Composant/axes/axes.component';
import { addIcons } from 'ionicons';
import { logOutOutline } from 'ionicons/icons';
import { IonIcon } from '@ionic/angular/standalone';
import { AlertService } from '../nativeComp/alert/alert.service';
import { ToastService } from '../nativeComp/toast/toast.service';

interface VoyageDisplay extends Voyage {
  camionImmatriculation?: string;
  clientNom?: string;
  clientEmail?: string;
  typeProduit?: string;
  declarer?: boolean;
  liberer?: boolean;
  passager?: string; // passer_declarer, passer_non_declarer ou null
  transactions?: Transaction[];
}

interface TransitaireInfo {
  id: number;
  nom: string;
  identifiant: string;
}

@Component({
  selector: 'app-for-transitaire',
  templateUrl: './for-transitaire.page.html',
  styleUrls: ['./for-transitaire.page.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, IonIcon, AxesComponent]
})
export class ForTransitairePage implements OnInit {
  activeTab: 'en-cours' | 'voyages-en-cours' | 'archives' | 'axes' = 'en-cours';
  searchTerm: string = '';
  isLoading: boolean = false;
  voyages: VoyageDisplay[] = [];
  filteredVoyages: VoyageDisplay[] = [];

  // Onglet "Voyages en cours" (tous les voyages non déchargés, paginés)
  voyagesEnCours: VoyageDisplay[] = [];
  currentPageEnCours: number = 0;
  pageSizeEnCours: number = 10;
  totalPagesEnCours: number = 0;
  totalElementsEnCours: number = 0;
  isLoadingEnCours: boolean = false;
  showDetailModal: boolean = false;
  selectedVoyage: VoyageDisplay | null = null;
  activeDetailTab: 'details' | 'frais' = 'details';
  transitaireInfo: TransitaireInfo | null = null;

  // Sélection multiple
  selectedVoyages: Set<number> = new Set();
  isDeclaringMultiple: boolean = false;
  isLiberingMultiple: boolean = false;

  // Statistiques
  stats: {
    nombreCamionsDeclares: number;
    totalFraisDouane: number;
    totalMontantT1: number;
  } = {
    nombreCamionsDeclares: 0,
    totalFraisDouane: 0,
    totalMontantT1: 0
  };
  isLoadingStats: boolean = false;

  // Pagination pour les archives
  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;

  // Filtres de date pour les archives
  filterDate: string = '';
  filterStartDate: string = '';
  filterEndDate: string = '';
  useDateRange: boolean = false;

  paysList: Pays[] = [];
  axesList: Axe[] = [];

  constructor(
    private voyagesService: VoyagesService,
    private transactionsService: TransactionsService,
    private router: Router,
    private authService: AuthService,
    private transitairesService: TransitairesService,
    private camionsService: CamionsService,
    private paysService: PaysService,
    private axesService: AxesService,
    private alertService: AlertService,
    private toastService: ToastService
  ) {
    addIcons({logOutOutline});
  }

  ngOnInit() {
    // Récupérer l'identifiant directement depuis le localStorage
    const identifiant = this.authService.getIdentifiant();
    if (identifiant) {
      // Utiliser directement l'identifiant sans appel API supplémentaire
      this.transitaireInfo = {
        id: 0, // Non utilisé car on utilise l'identifiant
        nom: identifiant,
        identifiant: identifiant
      };
      this.loadVoyages();
      this.loadStats();
      this.loadPaysAndAxes();
    } else {
      console.warn('Aucune session utilisateur trouvée');
      this.router.navigate(['/login']);
    }
  }

  loadVoyages() {
    if (!this.transitaireInfo?.identifiant) return;

    this.isLoading = true;
    // Charger uniquement les voyages non déclarés pour l'onglet "en-cours" via l'identifiant
    this.voyagesService.getVoyagesNonDeclaresByTransitaireIdentifiant(this.transitaireInfo.identifiant).subscribe({
      next: (data) => {
        this.voyages = this.sortByDateDepartAsc(
          data.map(v => ({
            ...v,
            camionImmatriculation: (v as any).camionImmatriculation,
            clientNom: (v as any).clientNom,
            clientEmail: (v as any).clientEmail,
            typeProduit: (v as any).typeProduit || 'Essence',
            transactions: (v as any).transactions || [],
            liberer: (v as any).liberer ?? false
          }))
        );
        this.updateFilteredVoyages();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des voyages:', error);
        this.isLoading = false;
      }
    });
  }

  setTab(tab: 'en-cours' | 'voyages-en-cours' | 'archives' | 'axes') {
    this.activeTab = tab;
    if (tab === 'archives') {
      this.currentPage = 0;
      this.loadArchivedVoyages();
    } else if (tab === 'voyages-en-cours') {
      this.currentPageEnCours = 0;
      this.loadVoyagesEnCours();
    } else if (tab === 'en-cours') {
      this.loadVoyages();
    }
  }

  loadVoyagesEnCours() {
    if (!this.transitaireInfo?.identifiant) return;

    this.isLoadingEnCours = true;
    this.voyagesService
      .getVoyagesEnCoursByTransitaireIdentifiant(
        this.transitaireInfo.identifiant,
        this.currentPageEnCours,
        this.pageSizeEnCours
      )
      .subscribe({
        next: (page: VoyagePage) => {
          this.voyagesEnCours = this.sortByDateDepartAsc(
            (page.voyages || []).map(v => ({
              ...v,
              camionImmatriculation: (v as any).camionImmatriculation,
              clientNom: (v as any).clientNom,
              clientEmail: (v as any).clientEmail,
              typeProduit: (v as any).typeProduit || 'Essence',
              transactions: (v as any).transactions || [],
              liberer: (v as any).liberer ?? false
            }))
          );
          this.totalPagesEnCours = page.totalPages ?? 0;
          this.totalElementsEnCours = page.totalElements ?? 0;
          this.isLoadingEnCours = false;
        },
        error: () => {
          this.toastService.error('Erreur lors du chargement des voyages en cours');
          this.isLoadingEnCours = false;
        }
      });
  }

  goToPageEnCours(page: number) {
    if (page >= 0 && page < this.totalPagesEnCours) {
      this.currentPageEnCours = page;
      this.loadVoyagesEnCours();
    }
  }

  updateFilteredVoyages() {
    let filtered = this.voyages;

    // Filtrer par statut (en cours vs archives)
    if (this.activeTab === 'en-cours') {
      // Les voyages non déclarés sont déjà filtrés par le backend
      // Pas besoin de re-filtrer ici, la liste contient uniquement les voyages non déclarés
    } else {
      // Pour les archives, on utilise la pagination backend, donc pas de filtrage local
      return;
    }

    // Filtrer par terme de recherche
    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(v =>
        v.numeroVoyage?.toLowerCase().includes(term) ||
        v.clientNom?.toLowerCase().includes(term) ||
        v.destination?.toLowerCase().includes(term) ||
        v.camionImmatriculation?.toLowerCase().includes(term)
      );
    }

    this.filteredVoyages = filtered;
  }

  onSearchChange() {
    if (this.activeTab === 'en-cours') {
      this.updateFilteredVoyages();
    } else {
      // Pour les archives, recharger depuis le backend
      this.currentPage = 0;
      this.loadArchivedVoyages();
    }
  }

  loadArchivedVoyages() {
    if (!this.transitaireInfo) return;

    this.isLoading = true;
    let request: Observable<VoyagePage>;

    if (this.useDateRange && this.filterStartDate && this.filterEndDate) {
      // Filtrer par intervalle de dates
      request = this.voyagesService.getArchivedVoyagesByTransitaireIdentifiantAndDateRange(
        this.transitaireInfo.identifiant,
        this.filterStartDate,
        this.filterEndDate,
        this.currentPage,
        this.pageSize
      );
    } else if (this.filterDate) {
      // Filtrer par date unique
      request = this.voyagesService.getArchivedVoyagesByTransitaireIdentifiantAndDate(
        this.transitaireInfo.identifiant,
        this.filterDate,
        this.currentPage,
        this.pageSize
      );
    } else {
      // Pas de filtre de date
      request = this.voyagesService.getArchivedVoyagesByTransitaireIdentifiant(
        this.transitaireInfo.identifiant,
        this.currentPage,
        this.pageSize
      );
    }

    request.subscribe({
      next: (page: VoyagePage) => {
        this.filteredVoyages = this.sortByDateDepartAsc(
          page.voyages.map(v => ({
            ...v,
            camionImmatriculation: (v as any).camionImmatriculation,
            clientNom: (v as any).clientNom,
            clientEmail: (v as any).clientEmail,
            typeProduit: (v as any).typeProduit || 'Essence',
            transactions: (v as any).transactions || []
          }))
        );

        // Appliquer le filtre de recherche si présent
        if (this.searchTerm.trim()) {
          const term = this.searchTerm.toLowerCase();
          this.filteredVoyages = this.filteredVoyages.filter(v =>
            v.numeroVoyage?.toLowerCase().includes(term) ||
            v.clientNom?.toLowerCase().includes(term) ||
            v.destination?.toLowerCase().includes(term) ||
            v.camionImmatriculation?.toLowerCase().includes(term)
          );
        }

        this.currentPage = page.currentPage;
        this.totalPages = page.totalPages;
        this.totalElements = page.totalElements;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des archives:', error);
        this.isLoading = false;
        this.toastService.error('Erreur lors du chargement des archives');
      }
    });
  }

  onDateFilterChange() {
    this.currentPage = 0;
    this.loadArchivedVoyages();
  }

  clearDateFilters() {
    this.filterDate = '';
    this.filterStartDate = '';
    this.filterEndDate = '';
    this.useDateRange = false;
    this.currentPage = 0;
    this.loadArchivedVoyages();
  }

  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadArchivedVoyages();
    }
  }

  previousPage() {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadArchivedVoyages();
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadArchivedVoyages();
    }
  }

  updateStatut(voyage: VoyageDisplay, event: Event) {
    const select = event.target as HTMLSelectElement;
    const newStatut = select.value;

    if (newStatut === voyage.statut) return;

    this.voyagesService.updateStatut(voyage.id!, newStatut, {}).subscribe({
      next: (updatedVoyage) => {
        // Mettre à jour le voyage dans la liste
        const index = this.voyages.findIndex(v => v.id === voyage.id);
        if (index !== -1) {
          this.voyages[index] = {
            ...this.voyages[index],
            statut: updatedVoyage.statut as any
          };
        }
        if (this.selectedVoyage && this.selectedVoyage.id === voyage.id) {
          this.selectedVoyage.statut = updatedVoyage.statut as any;
        }
        this.updateFilteredVoyages();
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour du statut:', error);
        this.toastService.error('Erreur lors de la mise à jour du statut');
        // Réinitialiser le select
        select.value = voyage.statut || '';
      }
    });
  }

  isStatusLocked(statut: string | undefined): boolean {
    if (!statut) return false;
    // Les statuts finaux ne peuvent pas être modifiés
    return statut === 'LIVRE' || statut === 'RECEPTIONNER';
  }

  /** Retourne true si le voyage est considéré comme déclaré (backend peut renvoyer boolean ou string). */
  isDeclared(voyage: VoyageDisplay): boolean {
    if (!voyage) return false;
    const d = (voyage as { declarer?: boolean | string }).declarer;
    return d === true || d === 'true';
  }

  /** True si le voyage est en état "À déclarer" (douane non déclaré ou passé non déclaré). */
  isStatutADeclarer(voyage: VoyageDisplay): boolean {
    if (!voyage) return false;
    const statut = (voyage.statut || '').toString().toUpperCase();
    const isDouane = statut === 'DOUANE';
    const isPasseNonDeclarer = voyage.passager === 'passer_non_declarer';
    return (isDouane || isPasseNonDeclarer) && !this.isDeclared(voyage);
  }

  /** Libellé du statut. Utilise isStatutADeclarer pour "À déclarer" (une seule source de vérité). */
  getStatusLabel(statut: string | undefined, declarer?: boolean | string, passager?: string): string {
    if (!statut) return 'N/A';
    const v: VoyageDisplay = { statut, declarer, passager } as VoyageDisplay;
    if (this.isStatutADeclarer(v)) return 'À déclarer';
    const key = (statut || '').toString().toUpperCase();
    const labels: { [key: string]: string } = {
      'CHARGEMENT': 'Chargement',
      'CHARGE': 'Chargé',
      'DEPART': 'Départ',
      'ARRIVER': 'Arrivé',
      'DOUANE': 'Douane',
      'RECEPTIONNER': 'Sortie de douane',
      'LIVRE': 'Livré'
    };
    return labels[key] || statut;
  }

  getStatusClass(statut: string): string {
    const key = (statut || '').toString().toUpperCase();
    const classes: { [key: string]: string } = {
      'CHARGEMENT': 'badge-blue',
      'CHARGE': 'badge-orange',
      'DEPART': 'badge-purple',
      'ARRIVER': 'badge-green',
      'DOUANE': 'badge-yellow',
      'RECEPTIONNER': 'badge-teal',
      'LIVRE': 'badge-teal'
    };
    return classes[key] || 'badge-gray';
  }

  /** True si le statut doit être cliquable pour déclencher la déclaration (même logique que l'affichage "À déclarer"). */
  canDeclarer(voyage: VoyageDisplay): boolean {
    return this.isStatutADeclarer(voyage);
  }

  /** Gestion du clic sur la cellule de statut « À déclarer » : met à « Libérer » en premier, puis déclare si déjà prêt. */
  onStatutClick(voyage: VoyageDisplay) {
    if (!this.canDeclarer(voyage)) return;
    // Si déjà en état Libérer (passer_non_declarer), déclarer (Sortie de douane)
    if (voyage.passager === 'passer_non_declarer') {
      this.declarerVoyage(voyage);
    } else {
      // Sinon, mettre à Libérer en premier (comme Passer non déclaré)
      this.passerNonDeclarer(voyage);
    }
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

  viewVoyage(voyage: VoyageDisplay) {
    this.selectedVoyage = voyage;
    this.activeDetailTab = 'details';
    this.showDetailModal = true;
  }

  closeDetailModal() {
    this.showDetailModal = false;
    this.selectedVoyage = null;
    this.activeDetailTab = 'details';
  }

  setDetailTab(tab: 'details' | 'frais') {
    this.activeDetailTab = tab;
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
      'FRAIS_CHAMBRE_COMMERCE': 'Frais chambre de commerce',
      'SALAIRE': 'Salaire'
    };
    return labels[type] || type;
  }

  getTransactionStatusLabel(statut: string): string {
    const labels: { [key: string]: string } = {
      'EN_ATTENTE': 'En attente',
      'VALIDE': 'Validé',
      'REJETE': 'Rejeté',
      'ANNULE': 'Annulé'
    };
    return labels[statut] || statut;
  }

  getTransactionStatusClass(statut: string): string {
    const classes: { [key: string]: string } = {
      'EN_ATTENTE': 'badge-yellow',
      'VALIDE': 'badge-green',
      'REJETE': 'badge-red',
      'ANNULE': 'badge-gray'
    };
    return classes[statut] || 'badge-gray';
  }

  declarerVoyage(voyage: VoyageDisplay) {
    if (!voyage.id || !voyage.camionId) {
      this.toastService.error('Informations du voyage incomplètes');
      return;
    }

    this.camionsService.getCamionById(voyage.camionId).subscribe({
      next: (camion: Camion) => {
        const pays = this.getPaysForVoyage(voyage);
        if (!pays) {
          this.toastService.error('Aucun pays/frais configuré pour l\'axe de ce voyage');
          return;
        }

        const isGasoil = voyage.typeProduit === 'GAZOLE';
        const fraisParLitre = isGasoil ? pays.fraisParLitreGasoil : pays.fraisParLitre;
        const fraisDouane = fraisParLitre * camion.capacite;
        const fraisT1 = pays.fraisT1;

        const message = `Vous allez déclarer le citerne matriculé ${camion.immatriculation}\n\n` +
          `Pays : ${pays.nom}\n` +
          `Frais de douane: ${fraisDouane.toLocaleString('fr-FR')} FCFA\n` +
          `Frais T1: ${fraisT1.toLocaleString('fr-FR')} FCFA\n\n` +
          `Total: ${(fraisDouane + fraisT1).toLocaleString('fr-FR')} FCFA`;

        this.alertService.confirm(message, 'Confirmation de déclaration').subscribe(confirmed => {
          if (!confirmed) return;
          this.voyagesService.declarerVoyage(voyage.id!, undefined, undefined).subscribe({
            next: () => {
              this.loadVoyages();
              this.loadStats();
              this.toastService.success('Voyage déclaré avec succès');
            },
            error: (error) => {
              const errorMessage = error.error?.message || 'Erreur lors de la déclaration du voyage';
              this.toastService.error(errorMessage);
            }
          });
        });
      },
      error: () => {
        this.toastService.error('Erreur lors de la récupération des informations du camion');
      }
    });
  }

  passerNonDeclarer(voyage: VoyageDisplay) {
    if (!voyage.id) {
      this.toastService.error('Informations du voyage incomplètes');
      return;
    }

    this.alertService.confirm(
      `Voulez-vous marquer le voyage ${voyage.numeroVoyage} comme "Passé non déclaré" ?`,
      'Confirmation'
    ).subscribe(confirmed => {
      if (!confirmed) return;

      this.voyagesService.passerNonDeclarer(voyage.id!).subscribe({
        next: () => {
          // Recharger la liste depuis le backend : le voyage reste en « voyages actifs »
          this.loadVoyages();
          this.toastService.success('Voyage marqué comme passé non déclaré');
        },
        error: (error) => {
          console.error('Erreur lors du marquage:', error);
          const errorMessage = error.error?.message || 'Erreur lors du marquage du voyage';
          this.toastService.error(errorMessage);
        }
      });
    });
  }

  loadStats() {
    if (!this.transitaireInfo?.identifiant) return;

    this.isLoadingStats = true;
    this.voyagesService.getTransitaireStatsByIdentifiant(this.transitaireInfo.identifiant).subscribe({
      next: (stats: TransitaireStats) => {
        this.stats = {
          nombreCamionsDeclares: stats.nombreCamionsDeclaresCeMois,
          totalFraisDouane: stats.totalFraisDouaneCeMois ?? 0,
          totalMontantT1: stats.totalMontantT1CeMois ?? 0
        };
        this.isLoadingStats = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des statistiques:', error);
        this.isLoadingStats = false;
      }
    });
  }

  toggleVoyageSelection(voyageId: number | undefined) {
    if (!voyageId) return;
    if (this.selectedVoyages.has(voyageId)) {
      this.selectedVoyages.delete(voyageId);
    } else {
      this.selectedVoyages.add(voyageId);
    }
  }

  isVoyageSelected(voyageId: number | undefined): boolean {
    if (!voyageId) return false;
    return this.selectedVoyages.has(voyageId);
  }

  toggleSelectAll() {
    const voyages = this.voyagesADeclarer;
    if (voyages.length === 0) return;

    const allSelected = this.allVoyagesSelected;
    if (allSelected) {
      voyages.forEach(v => {
        if (v.id) this.selectedVoyages.delete(v.id);
      });
    } else {
      voyages.forEach(v => {
        if (v.id) this.selectedVoyages.add(v.id);
      });
    }
  }

  getSelectedCount(): number {
    return this.selectedVoyages.size;
  }

  get voyagesADeclarer(): VoyageDisplay[] {
    return this.filteredVoyages.filter(v => v.statut === 'DOUANE' && v.id);
  }

  get allVoyagesSelected(): boolean {
    const voyages = this.voyagesADeclarer;
    return voyages.length > 0 && voyages.every(v => v.id && this.selectedVoyages.has(v.id));
  }

  get selectAllTitle(): string {
    return this.allVoyagesSelected ? 'Désélectionner tout' : 'Sélectionner tout';
  }

  /** Nombre de voyages sélectionnés qui ne sont pas encore libérés */
  getSelectedLibererCount(): number {
    return this.filteredVoyages.filter(v => v.id && this.selectedVoyages.has(v.id) && !v.liberer).length;
  }

  libererVoyage(voyage: VoyageDisplay) {
    const voyageId = voyage.id;
    if (!voyageId) return;
    this.voyagesService.libererVoyage(voyageId).subscribe({
      next: () => {
        this.loadVoyages();
        this.selectedVoyages.delete(voyageId);
        this.toastService.success('Voyage libéré avec succès');
      },
      error: (err) => {
        this.toastService.error(err?.error?.message || 'Erreur lors de la libération du voyage');
      }
    });
  }

  libererTous() {
    const voyageIds = this.filteredVoyages
      .filter(v => v.id && this.selectedVoyages.has(v.id) && !v.liberer)
      .map(v => v.id!);
    if (voyageIds.length === 0) {
      this.toastService.warning('Aucun voyage à libérer dans la sélection');
      return;
    }
    this.isLiberingMultiple = true;
    this.voyagesService.libererVoyages(voyageIds).subscribe({
      next: (updatedVoyages) => {
        this.loadVoyages();
        this.selectedVoyages.clear();
        this.isLiberingMultiple = false;
        this.toastService.success(`${updatedVoyages.length} voyage(s) libéré(s) avec succès`);
      },
      error: (err) => {
        this.isLiberingMultiple = false;
        this.toastService.error(err?.error?.message || 'Erreur lors de la libération des voyages');
      }
    });
  }

  declarerTous() {
    const voyageIds = Array.from(this.selectedVoyages);
    if (voyageIds.length === 0) {
      this.toastService.warning('Aucun voyage sélectionné');
      return;
    }

    const voyagesSelectionnes = this.filteredVoyages.filter(v => v.id && this.selectedVoyages.has(v.id));

    const camionPromises = voyagesSelectionnes.map(voyage => {
      if (!voyage.camionId) return Promise.resolve(null);
      return this.camionsService.getCamionById(voyage.camionId).toPromise();
    });

    Promise.all(camionPromises).then(camions => {
      let totalFrais = 0;
      let totalCamions = 0;

      camions.forEach((camion, index) => {
        if (camion) {
          const voyage = voyagesSelectionnes[index];
          const pays = this.getPaysForVoyage(voyage);
          if (pays) {
            const isGasoil = voyage.typeProduit === 'GAZOLE';
            const fraisParLitre = isGasoil ? pays.fraisParLitreGasoil : pays.fraisParLitre;
            totalFrais += fraisParLitre * camion.capacite + pays.fraisT1;
            totalCamions++;
          }
        }
      });

      const message = `Vous allez déclarer ${totalCamions} camion(s)\n\n` +
        `Total des frais: ${totalFrais.toLocaleString('fr-FR')} FCFA`;

      this.alertService.confirm(message, 'Confirmation de déclaration multiple').subscribe(confirmed => {
        if (!confirmed) return;

        this.isDeclaringMultiple = true;
        this.voyagesService.declarerVoyagesMultiple(voyageIds, undefined, undefined).subscribe({
          next: (updatedVoyages) => {
            this.loadVoyages();
            this.loadStats();
            this.selectedVoyages.clear();
            this.toastService.success(`${updatedVoyages.length} voyage(s) déclaré(s) avec succès`);
            this.isDeclaringMultiple = false;
          },
          error: (error) => {
            const errorMessage = error.error?.message || 'Erreur lors de la déclaration des voyages';
            this.toastService.error(errorMessage);
            this.isDeclaringMultiple = false;
          }
        });
      });
    });
  }

  deconnecter() {
    this.alertService.confirm(
      'Êtes-vous sûr de vouloir vous déconnecter ?',
      'Déconnexion'
    ).subscribe(confirmed => {
      if (!confirmed) return;
      this.authService.logout().subscribe({
        next: () => {
          this.router.navigate(['/login']);
        },
        error: (error) => {
          console.error('Erreur lors de la déconnexion:', error);
          // Déconnexion locale même en cas d'erreur
          this.authService.clearAuthData();
          this.router.navigate(['/login']);
        }
      });
    });
  }

  loadPaysAndAxes() {
    this.paysService.getAll().subscribe({
      next: (data) => { this.paysList = data; },
      error: () => {}
    });
    this.axesService.getAllAxes().subscribe({
      next: (data) => { this.axesList = data; },
      error: () => {}
    });
  }

  getPaysForVoyage(voyage: VoyageDisplay): Pays | null {
    if (!voyage.axeId) return null;
    const axe = this.axesList.find(a => a.id === voyage.axeId);
    if (!axe || !axe.paysId) return null;
    return this.paysList.find(p => p.id === axe.paysId) ?? null;
  }

  formatDateTime(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  private sortByDateDepartAsc<T extends { dateDepart?: string }>(voyages: T[]): T[] {
    return voyages.sort((a, b) => {
      const da = a.dateDepart ? new Date(a.dateDepart).getTime() : 0;
      const db = b.dateDepart ? new Date(b.dateDepart).getTime() : 0;
      return da - db;
    });
  }
}
