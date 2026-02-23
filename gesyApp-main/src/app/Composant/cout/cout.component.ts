import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FournisseursService, Fournisseur } from '../../services/fournisseurs.service';
import { VoyagesService, CoutTransport } from '../../services/voyages.service';


@Component({
  selector: 'app-cout',
  templateUrl: './cout.component.html',
  styleUrls: ['./cout.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
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

  constructor(
    private fournisseursService: FournisseursService,
    private voyagesService: VoyagesService
  ) {}

  ngOnInit() {
    this.loadFournisseurs();
  }

  loadFournisseurs() {
    this.fournisseursService.getAllFournisseurs().subscribe({
      next: (data) => {
        // Filtrer uniquement les fournisseurs de transport
        this.fournisseurs = data.filter(f => f.typeFournisseur === 'TRANSPORT');
      },
      error: (error) => {
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
    if (!this.selectedFournisseurId) {
      return;
    }

    this.isLoading = true;

    // Préparer les paramètres
    let filterOption = this.filterOption;
    if (filterOption === 'tous') {
      filterOption = 'tous';
    }

    let startDate: string | undefined;
    let endDate: string | undefined;

    if (this.filterOption === 'intervalle' && this.filterStartDate && this.filterEndDate) {
      startDate = this.filterStartDate;
      endDate = this.filterEndDate;
    }

    // Appeler le backend
    this.voyagesService.getCoutsTransport(this.selectedFournisseurId, filterOption, startDate, endDate).subscribe({
      next: (response) => {

        this.couts = response.couts;
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
}
