import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardService, DashboardStats } from '../../services/dashboard.service';
import { AuthService } from '../../services/auth.service';
import { getVoyageStatutLabel, STATUTS_VOYAGE_ORDER } from '../../services/voyage-statut.utils';

@Component({
  selector: 'app-dashbord',
  templateUrl: './dashbord.component.html',
  styleUrls: ['./dashbord.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class DashbordComponent implements OnInit {
  stats: DashboardStats | null = null;
  isLoading = true;
  error: string | null = null;
  isAdmin: boolean = false;
  isLogisticien: boolean = false;
  isResponsableLogistique: boolean = false;

  /** True si l'utilisateur doit voir la section voyages/statuts (admin, responsable logistique ou logisticien) */
  get showVoyagesSection(): boolean {
    return this.isAdmin || this.isLogisticien || this.isResponsableLogistique;
  }

  constructor(
    private dashboardService: DashboardService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.isAdmin = this.authService.hasRole('ROLE_ADMIN');
    this.isLogisticien = this.authService.isLogisticien();
    this.isResponsableLogistique = this.authService.isResponsableLogistique();
    this.loadDashboardStats();
  }

  loadDashboardStats() {
    this.isLoading = true;
    this.error = null;

    this.dashboardService.getDashboardStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des statistiques:', error);
        this.error = 'Erreur lors du chargement des statistiques';
        this.isLoading = false;
      }
    });
  }

  get statsData() {
    return this.stats || {
      camionsActifs: { value: 0, change: '0%', enRoute: 0, disponibles: 0 },
      chiffreAffaires: { value: '0', currency: 'F', change: '0%', period: '', increase: '0 F' },
      facturesAttente: { value: 0, badge: 0, montant: '0 F', enRetard: 0 },
      unitesStock: { 
        value: '0', 
        stockRestant: '0', 
        alert: false, 
        niveauCritique: 0, 
        depots: 0,
        stocksParProduit: []
      }
    };
  }

  get financesData() {
    return this.stats?.finances || {
      soldeBanque: { value: '0', currency: 'F', comptes: 0, change: '0%' },
      soldeCaisse: { value: '0', currency: 'F', date: '', entrees: '0 F' },
      creancesClients: { value: '0', currency: 'F', clients: 0, retard: '0 F' }
    };
  }

  get douaneData() {
    return this.stats?.douaneStats || {
      nombreCamionsDeclares: 0,
      nombreCamionsNonDeclares: 0,
      montantFraisDouane: '0 F',
      montantT1: '0 F',
      montantFraisPayes: '0 F',
      currency: 'F'
    };
  }

  get statutsMoisCourant() {
    return this.stats?.statutsVoyageMoisCourant || [];
  }

  /** Tous les statuts avec leur total du mois (0 si absent de l’API). */
  get statutsMoisCourantComplet(): { statut: string; count: number }[] {
    const fromApi = this.stats?.statutsVoyageMoisCourant || [];
    const countByStatut = new Map(fromApi.map((x) => [x.statut, x.count]));
    return STATUTS_VOYAGE_ORDER.map((statut) => ({
      statut,
      count: countByStatut.get(statut) ?? 0
    }));
  }

  getStatutVoyageLabel(statut: string): string {
    return getVoyageStatutLabel(statut);
  }
}
