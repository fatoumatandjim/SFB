import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { VoyagesService, Voyage, VoyagePage, TransitaireStats } from '../services/voyages.service';
import { TransactionsService, Transaction } from '../services/transactions.service';
import { TransitairesService, Transitaire } from '../services/transitaires.service';
import { DouaneService, Douane, HistoriqueDouane, FraisDouaneAxe } from '../services/douane.service';
import { AxesService, Axe } from '../services/axes.service';
import { CamionsService, Camion } from '../services/camions.service';
import { addIcons } from 'ionicons';
import { arrowBackOutline } from 'ionicons/icons';
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
  passager?: string;
  transactions?: Transaction[];
}

interface TransitaireInfo {
  id: number;
  nom: string;
  identifiant: string;
}

@Component({
  selector: 'app-transitaire-detail',
  templateUrl: './transitaire-detail.page.html',
  styleUrls: ['./transitaire-detail.page.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, IonIcon]
})
export class TransitaireDetailPage implements OnInit {
  activeTab: 'en-cours' | 'voyages-en-cours' | 'archives' = 'en-cours';
  searchTerm: string = '';
  isLoading: boolean = false;
  voyages: VoyageDisplay[] = [];
  filteredVoyages: VoyageDisplay[] = [];

  // Onglet "Voyages en cours" (non déclarés ou passer_non_declarer, paginés)
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

  selectedVoyages: Set<number> = new Set();
  isDeclaringMultiple: boolean = false;
  isLiberingMultiple: boolean = false;

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

  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;

  filterDate: string = '';
  filterStartDate: string = '';
  filterEndDate: string = '';
  useDateRange: boolean = false;

  douane: Douane | null = null;
  isLoadingDouane: boolean = false;
  isEditingDouane: boolean = false;
  douaneForm: Douane = { fraisParLitre: 0, fraisParLitreGasoil: 0, fraisT1: 0 };
  historiqueDouane: HistoriqueDouane[] = [];
  showHistorique: boolean = false;

  fraisDouaneAxes: FraisDouaneAxe[] = [];
  axes: Axe[] = [];
  isLoadingFraisAxe: boolean = false;
  showFraisAxeModal: boolean = false;
  editingFraisAxe: FraisDouaneAxe | null = null;
  formFraisAxe: { axeId?: number; fraisParLitre: number; fraisParLitreGasoil: number; fraisT1: number } = { fraisParLitre: 0, fraisParLitreGasoil: 0, fraisT1: 0 };
  createNewAxe: boolean = false;
  newAxeNom: string = '';
  isSavingFraisAxe: boolean = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private voyagesService: VoyagesService,
    private transitairesService: TransitairesService,
    private douaneService: DouaneService,
    private axesService: AxesService,
    private camionsService: CamionsService,
    private alertService: AlertService,
    private toastService: ToastService
  ) {
    addIcons({ arrowBackOutline });
  }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/home']);
      return;
    }
    const idNum = Number(id);
    if (isNaN(idNum)) {
      this.router.navigate(['/home']);
      return;
    }
    this.transitairesService.getTransitaireById(idNum).subscribe({
      next: (t: Transitaire) => {
        this.transitaireInfo = {
          id: t.id ?? idNum,
          nom: t.nom ?? '',
          identifiant: t.identifiant ?? ''
        };
        this.loadVoyages();
        this.loadStats();
        this.loadDouane();
        this.loadFraisDouaneAxes();
        this.loadAxes();
      },
      error: () => {
        this.toastService.error('Transitaire introuvable');
        this.router.navigate(['/home']);
      }
    });
  }

  back() {
    this.router.navigate(['/home'], { queryParams: { section: 'transitaire' } });
  }

  loadVoyages() {
    if (!this.transitaireInfo?.identifiant) return;
    this.isLoading = true;
    this.voyagesService.getVoyagesNonDeclaresByTransitaireIdentifiant(this.transitaireInfo.identifiant).subscribe({
      next: (data) => {
        this.voyages = data.map(v => ({
          ...v,
          camionImmatriculation: (v as any).camionImmatriculation,
          clientNom: (v as any).clientNom,
          clientEmail: (v as any).clientEmail,
          typeProduit: (v as any).typeProduit || 'Essence',
          transactions: (v as any).transactions || [],
          liberer: (v as any).liberer ?? false
        }));
        this.updateFilteredVoyages();
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  setTab(tab: 'en-cours' | 'voyages-en-cours' | 'archives') {
    this.activeTab = tab;
    if (tab === 'archives') {
      this.currentPage = 0;
      this.loadArchivedVoyages();
    } else if (tab === 'voyages-en-cours') {
      this.currentPageEnCours = 0;
      this.loadVoyagesEnCours();
    } else {
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
          this.voyagesEnCours = (page.voyages || []).map(v => ({
            ...v,
            camionImmatriculation: (v as any).camionImmatriculation,
            clientNom: (v as any).clientNom,
            clientEmail: (v as any).clientEmail,
            typeProduit: (v as any).typeProduit || 'Essence',
            transactions: (v as any).transactions || [],
            liberer: (v as any).liberer ?? false
          }));
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

  updateFilteredVoyages() {
    let filtered = this.voyages;
    if (this.activeTab !== 'en-cours') return;
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
    if (this.activeTab === 'en-cours') this.updateFilteredVoyages();
    else {
      this.currentPage = 0;
      this.loadArchivedVoyages();
    }
  }

  loadArchivedVoyages() {
    if (!this.transitaireInfo) return;
    this.isLoading = true;
    let request: Observable<VoyagePage>;
    if (this.useDateRange && this.filterStartDate && this.filterEndDate) {
      request = this.voyagesService.getArchivedVoyagesByTransitaireIdentifiantAndDateRange(
        this.transitaireInfo.identifiant, this.filterStartDate, this.filterEndDate, this.currentPage, this.pageSize);
    } else if (this.filterDate) {
      request = this.voyagesService.getArchivedVoyagesByTransitaireIdentifiantAndDate(
        this.transitaireInfo.identifiant, this.filterDate, this.currentPage, this.pageSize);
    } else {
      request = this.voyagesService.getArchivedVoyagesByTransitaireIdentifiant(
        this.transitaireInfo.identifiant, this.currentPage, this.pageSize);
    }
    request.subscribe({
      next: (page: VoyagePage) => {
        this.filteredVoyages = page.voyages.map(v => ({
          ...v,
          camionImmatriculation: (v as any).camionImmatriculation,
          clientNom: (v as any).clientNom,
          clientEmail: (v as any).clientEmail,
          typeProduit: (v as any).typeProduit || 'Essence',
          transactions: (v as any).transactions || []
        }));
        if (this.searchTerm.trim()) {
          const term = this.searchTerm.toLowerCase();
          this.filteredVoyages = this.filteredVoyages.filter(v =>
            v.numeroVoyage?.toLowerCase().includes(term) ||
            v.clientNom?.toLowerCase().includes(term) ||
            v.destination?.toLowerCase().includes(term) ||
            v.camionImmatriculation?.toLowerCase().includes(term));
        }
        this.currentPage = page.currentPage;
        this.totalPages = page.totalPages;
        this.totalElements = page.totalElements;
        this.isLoading = false;
      },
      error: () => {
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

  getStatusLabel(statut: string | undefined): string {
    const labels: { [key: string]: string } = {
      'CHARGEMENT': 'Chargement', 'CHARGE': 'Chargé', 'DEPART': 'Départ', 'ARRIVER': 'Arrivé',
      'DOUANE': 'Douane', 'RECEPTIONNER': 'Sortie de douane', 'LIVRE': 'Livré'
    };
    return labels[statut || ''] || statut || 'N/A';
  }

  getStatusClass(statut: string): string {
    const classes: { [key: string]: string } = {
      'CHARGEMENT': 'badge-blue', 'CHARGE': 'badge-orange', 'DEPART': 'badge-purple', 'ARRIVER': 'badge-green',
      'DOUANE': 'badge-yellow', 'RECEPTIONNER': 'badge-teal', 'LIVRE': 'badge-teal'
    };
    return classes[statut] || 'badge-gray';
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) return dateString;
      return date.toLocaleDateString('fr-FR', {
        day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
      });
    } catch {
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

  toggleVoyageSelection(voyageId: number | undefined) {
    if (!voyageId) return;
    if (this.selectedVoyages.has(voyageId)) this.selectedVoyages.delete(voyageId);
    else this.selectedVoyages.add(voyageId);
  }

  isVoyageSelected(voyageId: number | undefined): boolean {
    return !!(voyageId && this.selectedVoyages.has(voyageId));
  }

  toggleSelectAll() {
    const voyages = this.filteredVoyages.filter(v => v.statut === 'DOUANE' && v.id);
    if (voyages.length === 0) return;
    const allSelected = voyages.every(v => v.id && this.selectedVoyages.has(v.id));
    if (allSelected) voyages.forEach(v => { if (v.id) this.selectedVoyages.delete(v.id); });
    else voyages.forEach(v => { if (v.id) this.selectedVoyages.add(v.id); });
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

  getSelectedLibererCount(): number {
    return this.filteredVoyages.filter(v => v.id && this.selectedVoyages.has(v.id) && !v.liberer).length;
  }

  declarerVoyage(voyage: VoyageDisplay) {
    if (!voyage.id || !voyage.camionId) {
      this.toastService.error('Informations du voyage incomplètes');
      return;
    }
    this.douaneService.getDouane().subscribe({
      next: (douane: Douane) => {
        this.camionsService.getCamionById(voyage.camionId).subscribe({
          next: (camion: Camion) => {
            const fraisDouane = douane.fraisParLitre * camion.capacite;
            const fraisT1 = douane.fraisT1;
            const message = `Déclarer le citerne ${camion.immatriculation}\n\nFrais douane: ${fraisDouane.toLocaleString('fr-FR')} FCFA\nFrais T1: ${fraisT1.toLocaleString('fr-FR')} FCFA\nTotal: ${(fraisDouane + fraisT1).toLocaleString('fr-FR')} FCFA`;
            this.alertService.confirm(message, 'Confirmation').subscribe(confirmed => {
              if (!confirmed) return;
              this.voyagesService.declarerVoyage(voyage.id!, undefined, undefined).subscribe({
                next: () => {
                  this.loadVoyages();
                  this.loadStats();
                  this.toastService.success('Voyage déclaré avec succès');
                },
                error: (err) => this.toastService.error(err?.error?.message || 'Erreur lors de la déclaration')
              });
            });
          },
          error: () => this.toastService.error('Erreur lors de la récupération du camion')
        });
      },
      error: () => this.toastService.error('Erreur lors de la récupération des frais douane')
    });
  }

  passerNonDeclarer(voyage: VoyageDisplay) {
    if (!voyage.id) return;
    this.alertService.confirm(`Marquer le voyage ${voyage.numeroVoyage} comme "Passé non déclaré" ?`, 'Confirmation').subscribe(confirmed => {
      if (!confirmed) return;
      this.voyagesService.passerNonDeclarer(voyage.id!).subscribe({
        next: () => {
          this.loadVoyages();
          this.toastService.success('Voyage marqué comme passé non déclaré');
        },
        error: (err) => this.toastService.error(err?.error?.message || 'Erreur')
      });
    });
  }

  libererVoyage(voyage: VoyageDisplay) {
    if (!voyage.id) return;
    this.voyagesService.libererVoyage(voyage.id).subscribe({
      next: () => {
        this.loadVoyages();
        this.selectedVoyages.delete(voyage.id!);
        this.toastService.success('Voyage libéré avec succès');
      },
      error: (err) => this.toastService.error(err?.error?.message || 'Erreur')
    });
  }

  libererTous() {
    const voyageIds = this.filteredVoyages
      .filter(v => v.id && this.selectedVoyages.has(v.id) && !v.liberer)
      .map(v => v.id!);
    if (voyageIds.length === 0) {
      this.toastService.warning('Aucun voyage à libérer');
      return;
    }
    this.isLiberingMultiple = true;
    this.voyagesService.libererVoyages(voyageIds).subscribe({
      next: (updatedVoyages) => {
        this.loadVoyages();
        this.selectedVoyages.clear();
        this.isLiberingMultiple = false;
        this.toastService.success(`${updatedVoyages.length} voyage(s) libéré(s)`);
      },
      error: (err) => {
        this.isLiberingMultiple = false;
        this.toastService.error(err?.error?.message || 'Erreur');
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
    this.douaneService.getDouane().subscribe({
      next: (douane: Douane) => {
        let totalFrais = 0;
        let totalCamions = 0;
        const camionPromises = voyagesSelectionnes.map(v => v.camionId ? this.camionsService.getCamionById(v.camionId).toPromise() : Promise.resolve(null));
        Promise.all(camionPromises).then(camions => {
          camions.forEach((camion, index) => {
            if (camion) {
              const voyage = voyagesSelectionnes[index];
              const fraisParLitre = voyage.typeProduit === 'GAZOLE' ? douane.fraisParLitreGasoil : douane.fraisParLitre;
              totalFrais += fraisParLitre * camion.capacite + douane.fraisT1;
              totalCamions++;
            }
          });
          this.alertService.confirm(`Déclarer ${totalCamions} camion(s)\n\nTotal: ${totalFrais.toLocaleString('fr-FR')} FCFA`, 'Confirmation').subscribe(confirmed => {
            if (!confirmed) return;
            this.isDeclaringMultiple = true;
            this.voyagesService.declarerVoyagesMultiple(voyageIds, undefined, undefined).subscribe({
              next: (updatedVoyages) => {
                this.loadVoyages();
                this.loadStats();
                this.selectedVoyages.clear();
                this.isDeclaringMultiple = false;
                this.toastService.success(`${updatedVoyages.length} voyage(s) déclaré(s)`);
              },
              error: (err) => {
                this.isDeclaringMultiple = false;
                this.toastService.error(err?.error?.message || 'Erreur');
              }
            });
          });
        });
      },
      error: () => this.toastService.error('Erreur frais douane')
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
      error: () => { this.isLoadingStats = false; }
    });
  }

  loadDouane() {
    this.isLoadingDouane = true;
    this.douaneService.getDouane().subscribe({
      next: (douane) => {
        this.douane = douane;
        this.isLoadingDouane = false;
      },
      error: () => { this.isLoadingDouane = false; }
    });
  }

  editDouane() {
    if (this.douane) this.douaneForm = { ...this.douane };
    this.isEditingDouane = true;
  }

  cancelEditDouane() {
    this.isEditingDouane = false;
  }

  saveDouane() {
    if (!this.douaneForm.fraisParLitre || !this.douaneForm.fraisParLitreGasoil || !this.douaneForm.fraisT1) {
      this.toastService.error('Veuillez remplir tous les champs');
      return;
    }
    this.douaneService.updateDouane(this.douaneForm).subscribe({
      next: (updated) => {
        this.douane = updated;
        this.isEditingDouane = false;
        this.toastService.success('Frais de douane mis à jour');
        this.loadHistoriqueDouane();
      },
      error: () => { this.toastService.error('Erreur lors de la mise à jour'); }
    });
  }

  loadHistoriqueDouane() {
    this.douaneService.getHistorique().subscribe({
      next: (historique) => { this.historiqueDouane = historique; },
      error: () => {}
    });
  }

  toggleHistorique() {
    this.showHistorique = !this.showHistorique;
    if (this.showHistorique && this.historiqueDouane.length === 0) this.loadHistoriqueDouane();
  }

  loadAxes() {
    this.axesService.getAllAxes().subscribe({
      next: (data) => { this.axes = data; },
      error: () => {}
    });
  }

  loadFraisDouaneAxes() {
    this.isLoadingFraisAxe = true;
    this.douaneService.getFraisDouaneAxeAll().subscribe({
      next: (data) => { this.fraisDouaneAxes = data; this.isLoadingFraisAxe = false; },
      error: () => { this.isLoadingFraisAxe = false; }
    });
  }

  /**
   * Regroupe l'affichage des frais par axe sous un "bureau" (Dakar / Abidjan).
   * Le bureau est déduit du NOM de l'axe (liste fixe ci‑dessous), pas stocké en base.
   * Pour que ce soit configurable : ajouter un champ "bureau" sur l'entité Axe côté backend.
   */
  getBureauForAxe(axeNom: string | undefined): string {
    if (!axeNom) return 'Autre';
    const n = axeNom.toLowerCase().trim();
    if (n === 'diboli' || n === 'moussala') return 'Dakar';
    if (n === 'kadiana' || n === 'zegoua') return 'Abidjan';
    return 'Autre';
  }

  getFraxisByBureau(): { bureau: string; items: FraisDouaneAxe[] }[] {
    const groups = new Map<string, FraisDouaneAxe[]>();
    for (const f of this.fraisDouaneAxes) {
      const bureau = this.getBureauForAxe(f.axeNom);
      if (!groups.has(bureau)) groups.set(bureau, []);
      groups.get(bureau)!.push(f);
    }
    const order = ['Dakar', 'Abidjan', 'Autre'];
    return order.filter(b => groups.has(b)).map(bureau => ({ bureau, items: groups.get(bureau)! }));
  }

  axesSansFrais(): Axe[] {
    const axeIdsAvecFrais = new Set(this.fraisDouaneAxes.map(f => f.axeId));
    return this.axes.filter(a => !axeIdsAvecFrais.has(a.id));
  }

  openAddFraisAxeModal() {
    this.editingFraisAxe = null;
    this.formFraisAxe = { fraisParLitre: 0, fraisParLitreGasoil: 0, fraisT1: 0 };
    this.createNewAxe = false;
    this.newAxeNom = '';
    this.showFraisAxeModal = true;
  }

  openEditFraisAxeModal(frais: FraisDouaneAxe) {
    this.editingFraisAxe = frais;
    this.formFraisAxe = {
      axeId: frais.axeId,
      fraisParLitre: frais.fraisParLitre ?? 0,
      fraisParLitreGasoil: frais.fraisParLitreGasoil ?? 0,
      fraisT1: frais.fraisT1 ?? 0
    };
    this.showFraisAxeModal = true;
  }

  closeFraisAxeModal() {
    this.showFraisAxeModal = false;
    this.editingFraisAxe = null;
    this.createNewAxe = false;
    this.newAxeNom = '';
  }

  saveFraisAxe() {
    if (this.editingFraisAxe?.id) {
      this.isSavingFraisAxe = true;
      this.douaneService.updateFraisDouaneAxe(this.editingFraisAxe.id, this.formFraisAxe).subscribe({
        next: () => {
          this.loadFraisDouaneAxes();
          this.closeFraisAxeModal();
          this.isSavingFraisAxe = false;
          this.toastService.success('Frais de douane (axe) mis à jour');
        },
        error: () => { this.isSavingFraisAxe = false; this.toastService.error('Erreur lors de la mise à jour'); }
      });
    } else {
      if (this.createNewAxe) {
        const nom = this.newAxeNom?.trim();
        if (!nom) {
          this.toastService.error('Veuillez saisir le nom du nouvel axe');
          return;
        }
        this.isSavingFraisAxe = true;
        this.douaneService.createFraisDouaneAxeWithNewAxe({
          nomAxe: nom,
          fraisParLitre: this.formFraisAxe.fraisParLitre,
          fraisParLitreGasoil: this.formFraisAxe.fraisParLitreGasoil,
          fraisT1: this.formFraisAxe.fraisT1
        }).subscribe({
          next: () => {
            this.loadAxes();
            this.loadFraisDouaneAxes();
            this.closeFraisAxeModal();
            this.isSavingFraisAxe = false;
            this.toastService.success('Axe créé et frais de douane ajoutés');
          },
          error: (err) => {
            this.isSavingFraisAxe = false;
            const msg = err?.error?.message || err?.message || 'Erreur lors de la création';
            this.toastService.error(msg.includes('existe déjà')
              ? msg + ' Décochez « Créer un nouvel axe » et sélectionnez-le dans la liste.'
              : msg);
            this.loadAxes();
          }
        });
      } else {
        if (this.formFraisAxe.axeId == null) {
          this.toastService.error('Veuillez sélectionner un axe ou créer un nouvel axe');
          return;
        }
        this.isSavingFraisAxe = true;
        this.douaneService.createFraisDouaneAxe({
          axeId: this.formFraisAxe.axeId,
          fraisParLitre: this.formFraisAxe.fraisParLitre,
          fraisParLitreGasoil: this.formFraisAxe.fraisParLitreGasoil,
          fraisT1: this.formFraisAxe.fraisT1
        }).subscribe({
          next: () => {
            this.loadFraisDouaneAxes();
            this.closeFraisAxeModal();
            this.isSavingFraisAxe = false;
            this.toastService.success('Frais de douane (axe) ajoutés');
          },
          error: () => { this.isSavingFraisAxe = false; this.toastService.error('Erreur lors de l\'ajout'); }
        });
      }
    }
  }

  formatDateTime(dateString: string): string {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }
}
