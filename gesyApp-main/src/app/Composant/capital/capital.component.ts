import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CapitaleService, Capitale } from '../../services/capitale.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-capital',
  templateUrl: './capital.component.html',
  styleUrls: ['./capital.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class CapitalComponent implements OnInit {
  capitale: Capitale | null = null;
  isLoading = true;
  error: string | null = null;
  isAdmin: boolean = false;

  // Filtres
  filterType: 'all' | 'month' | 'range' = 'all';
  selectedYear: number = new Date().getFullYear();
  selectedMonth: number = new Date().getMonth() + 1;
  startDate: string = '';
  endDate: string = '';

  constructor(
    private capitaleService: CapitaleService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.isAdmin = this.authService.hasRole('ROLE_ADMIN');
    if (!this.isAdmin) {
      this.error = 'Accès refusé. Seuls les administrateurs peuvent accéder à cette page.';
      this.isLoading = false;
      return;
    }
    this.loadCapitale();
  }

  loadCapitale() {
    this.isLoading = true;
    this.error = null;

    let request: any;
    
    if (this.filterType === 'month') {
      request = this.capitaleService.getCapitaleByMonth(this.selectedYear, this.selectedMonth);
    } else if (this.filterType === 'range') {
      if (!this.startDate || !this.endDate) {
        this.error = 'Veuillez sélectionner une date de début et une date de fin';
        this.isLoading = false;
        return;
      }
      request = this.capitaleService.getCapitaleByDateRange(this.startDate, this.endDate);
    } else {
      request = this.capitaleService.getCapitale();
    }

    request.subscribe({
      next: (data: Capitale) => {
        this.capitale = data;
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Erreur lors du chargement du capital:', error);
        this.error = 'Erreur lors du chargement du capital';
        this.isLoading = false;
      }
    });
  }

  onFilterChange() {
    this.loadCapitale();
  }

  get fondsData() {
    return this.capitale?.fonds || {
      totalBanques: '0 F',
      totalBanquesValue: 0,
      totalCaisses: '0 F',
      totalCaissesValue: 0,
      totalGeneral: '0 F',
      totalGeneralValue: 0
    };
  }

  get stocksData() {
    return this.capitale?.stocks || {
      stocksDepot: [],
      totalStocksDepot: '0 F',
      totalStocksDepotValue: 0,
      stocksCamion: [],
      totalStocksCamion: '0 F',
      totalStocksCamionValue: 0,
      totalStocks: '0 F',
      totalStocksValue: 0
    };
  }

  get depensesData() {
    return this.capitale?.depensesInvestissement || {
      total: '0 F',
      totalValue: 0
    };
  }

  get totalCapital() {
    return this.capitale?.totalCapital || '0 F';
  }

  getYears(): number[] {
    const currentYear = new Date().getFullYear();
    const years: number[] = [];
    for (let i = currentYear - 5; i <= currentYear + 1; i++) {
      years.push(i);
    }
    return years;
  }

  getMonths(): Array<{ value: number; label: string }> {
    return [
      { value: 1, label: 'Janvier' },
      { value: 2, label: 'Février' },
      { value: 3, label: 'Mars' },
      { value: 4, label: 'Avril' },
      { value: 5, label: 'Mai' },
      { value: 6, label: 'Juin' },
      { value: 7, label: 'Juillet' },
      { value: 8, label: 'Août' },
      { value: 9, label: 'Septembre' },
      { value: 10, label: 'Octobre' },
      { value: 11, label: 'Novembre' },
      { value: 12, label: 'Décembre' }
    ];
  }
}
