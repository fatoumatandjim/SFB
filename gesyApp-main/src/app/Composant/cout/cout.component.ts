import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FournisseursService, Fournisseur } from '../../services/fournisseurs.service';
import { VoyagesService, CoutTransport, Voyage } from '../../services/voyages.service';
import { AuthService } from '../../services/auth.service';
import { ToastService } from '../../nativeComp/toast/toast.service';
import { EditPrixTransportModalComponent, VoyagePrixRef } from '../shared/edit-prix-transport-modal/edit-prix-transport-modal.component';


@Component({
  selector: 'app-cout',
  templateUrl: './cout.component.html',
  styleUrls: ['./cout.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, EditPrixTransportModalComponent]
})
export class CoutComponent implements OnInit {
  fournisseurs: Fournisseur[] = [];
  selectedFournisseurId: number | null = null;
  filterOption: 'nonPaye' | 'paye' | 'intervalle' | 'tous' = 'tous';
  couts: CoutTransport[] = [];
  isLoading: boolean = false;

  // Filtres de date pour intervalle
  filterStartDate: string = '';
  filterEndDate: string = '';

  // Statistiques
  totalCout: number = 0;
  totalNonPaye: number = 0;
  totalPaye: number = 0;

  // Erreur API (couts-transport)
  coutsError: string | null = null;

  // Modal modification prix (comptable) — référence stable (pas de getter)
  editingCout: CoutTransport | null = null;
  voyageRefToPass: VoyagePrixRef | null = null;

  constructor(
    private fournisseursService: FournisseursService,
    private voyagesService: VoyagesService,
    private authService: AuthService,
    private toastService: ToastService
  ) {}

  ngOnInit() {
    this.loadFournisseurs();
  }

  loadFournisseurs() {
    this.fournisseursService.getAllFournisseurs().subscribe({
      next: (data: Fournisseur[]) => {
        // Filtrer uniquement les fournisseurs de transport
        this.fournisseurs = (data || []).filter((f: Fournisseur) => f.typeFournisseur === 'TRANSPORT');
      },
      error: (error: unknown) => {
        console.error('Erreur lors du chargement des fournisseurs:', error);
      }
    });
  }

  onFournisseurChange() {
    if (this.selectedFournisseurId) {
      this.loadCouts();
    } else {
      this.couts = [];
      this.resetStats();
      this.coutsError = null;
    }
  }

  onFilterOptionChange() {
    if (this.selectedFournisseurId) {
      this.loadCouts();
    }
  }

  onDateRangeChange() {
    if (this.filterOption === 'intervalle' && this.filterStartDate && this.filterEndDate) {
      this.loadCouts();
    }
  }

  loadCouts() {
    const fournisseurId = this.selectedFournisseurId != null ? Number(this.selectedFournisseurId) : null;
    if (fournisseurId == null) {
      this.couts = [];
      this.resetStats();
      this.coutsError = null;
      return;
    }

    this.isLoading = true;
    this.coutsError = null;

    // Préparer les paramètres
    let filterOption = this.filterOption;
    let startDate: string | undefined;
    let endDate: string | undefined;

    if (this.filterOption === 'intervalle' && this.filterStartDate && this.filterEndDate) {
      startDate = this.filterStartDate;
      endDate = this.filterEndDate;
    }

    // Appeler le backend
    this.voyagesService.getCoutsTransport(fournisseurId, filterOption, startDate, endDate).subscribe({
      next: (response: any) => {
        this.couts = response.couts ?? [];
        // Mettre à jour les statistiques depuis le backend
        if (response.stats) {
          this.totalCout = response.stats.totalCout || 0;
          this.totalNonPaye = response.stats.totalNonPaye || 0;
          this.totalPaye = response.stats.totalPaye || 0;
        } else {
          this.calculateStats();
        }
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Erreur lors du chargement des coûts:', error);
        this.couts = [];
        this.resetStats();
        const status = error?.status;
        const message = error?.error?.message || error?.message || 'Erreur inconnue';
        this.coutsError = status ? `Erreur API (${status}) : ${message}` : `Erreur API : ${message}`;
        this.isLoading = false;
      }
    });
  }


  calculateStats() {
    this.totalCout = this.couts.reduce((sum, c) => sum + (c.coutTotal || 0), 0);
    this.totalNonPaye = this.couts
      .filter(c => c.statutPaiement === 'NON_PAYE')
      .reduce((sum, c) => sum + (c.coutTotal || 0), 0);
    this.totalPaye = this.couts
      .filter(c => c.statutPaiement === 'PAYE')
      .reduce((sum, c) => sum + (c.coutTotal || 0), 0);
  }

  resetStats() {
    this.totalCout = 0;
    this.totalNonPaye = 0;
    this.totalPaye = 0;
  }

  formatMontant(montant: number | undefined): string {
    if (!montant) return '0 F';
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'XOF',
      minimumFractionDigits: 0
    }).format(montant).replace('XOF', 'F');
  }

  formatDate(date: string | undefined): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  getStatutPaiementClass(statut: string | undefined): string {
    if (!statut) return 'statut-badge';
    return statut === 'PAYE' ? 'statut-badge paye' : 'statut-badge non-paye';
  }

  getStatutPaiementLabel(statut: string | undefined): string {
    if (!statut) return 'N/A';
    return statut === 'PAYE' ? 'Payé' : 'Non payé';
  }

  isComptable(): boolean {
    return this.authService.isComptable();
  }

  openEditPrixModal(c: CoutTransport) {
    this.editingCout = c;
    this.voyageRefToPass = {
      id: c.id!,
      numeroVoyage: c.numeroVoyage,
      quantite: c.quantite,
      prixUnitaire: c.prixUnitaire
    };
  }

  closeEditPrixModal() {
    this.editingCout = null;
    this.voyageRefToPass = null;
  }

  onSavePrixUnitaire(prixUnitaire: number) {
    if (!this.editingCout?.id || prixUnitaire <= 0) {
      this.toastService.warning('Veuillez saisir un prix valide');
      return;
    }
    this.voyagesService.getVoyageById(this.editingCout.id).subscribe({
      next: (voyage: Voyage) => {
        const payload = { ...voyage, prixUnitaire };
        this.voyagesService.updateVoyage(this.editingCout!.id!, payload).subscribe({
          next: () => {
            this.toastService.success('Prix unitaire mis à jour');
            this.closeEditPrixModal();
            if (this.selectedFournisseurId) this.loadCouts();
          },
          error: (err) => {
            this.toastService.error(err?.error?.message || 'Erreur lors de la mise à jour');
          }
        });
      },
      error: () => this.toastService.error('Impossible de charger le voyage')
    });
  }
}
