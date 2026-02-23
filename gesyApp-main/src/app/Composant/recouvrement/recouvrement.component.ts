import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FacturesService, Creance as CreanceAPI, RecouvrementStats } from '../../services/factures.service';

interface CreanceDisplay {
  id: string;
  facture: string;
  client: {
    nom: string;
    email: string;
    telephone: string;
    initiales: string;
    couleur: string;
  };
  montant: number;
  dateEmission: string;
  dateEcheance: string;
  joursRetard: number;
  statut: 'en-retard' | 'recouvre' | 'en-cours' | 'impaye';
  priorite: 'haute' | 'moyenne' | 'basse';
  dernierContact: string;
  relances: number;
}

@Component({
  selector: 'app-recouvrement',
  templateUrl: './recouvrement.component.html',
  styleUrls: ['./recouvrement.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class RecouvrementComponent implements OnInit {
  searchTerm: string = '';
  activeFilter: string = 'tous';
  isLoading: boolean = false;
  isLoadingStats: boolean = false;

  stats: RecouvrementStats = {
    totalCreances: {
      montant: 0,
      nombre: 0
    },
    enRetard: {
      montant: 0,
      nombre: 0,
      joursMoyen: 0
    },
    recouvre: {
      montant: 0,
      nombre: 0,
      pourcentage: '0%'
    },
    impaye: {
      montant: 0,
      nombre: 0
    }
  };

  creances: CreanceDisplay[] = [];

  constructor(private facturesService: FacturesService) { }

  ngOnInit() {
    this.loadCreances();
    this.loadStats();
  }

  loadCreances() {
    this.isLoading = true;
    this.facturesService.getUnpaidFactures().subscribe({
      next: (data: CreanceAPI[]) => {
        this.creances = data.map(c => this.mapCreanceToDisplay(c));
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Erreur lors du chargement des créances:', error);
        this.isLoading = false;
      }
    });
  }

  loadStats() {
    this.isLoadingStats = true;
    this.facturesService.getRecouvrementStats().subscribe({
      next: (data: RecouvrementStats) => {
        this.stats = data;
        this.isLoadingStats = false;
      },
      error: (error: any) => {
        console.error('Erreur lors du chargement des statistiques:', error);
        this.isLoadingStats = false;
      }
    });
  }

  mapCreanceToDisplay(creance: CreanceAPI): CreanceDisplay {
    // Extraire les initiales du client
    const initiales = creance.clientNom.split(' ').map((n: string) => n[0]).join('').toUpperCase().substring(0, 2) || '??';
    const couleurs = ['blue', 'purple', 'red', 'green', 'orange', 'teal', 'pink', 'yellow'];
    const hash = creance.clientNom.split('').reduce((acc: number, char: string) => acc + char.charCodeAt(0), 0);
    const couleur = couleurs[hash % couleurs.length];

    return {
      id: creance.id.toString(),
      facture: creance.facture,
      client: {
        nom: creance.clientNom,
        email: creance.clientEmail,
        telephone: creance.clientTelephone || '',
        initiales: initiales,
        couleur: couleur
      },
      montant: creance.montant,
      dateEmission: this.formatDate(creance.dateEmission),
      dateEcheance: this.formatDate(creance.dateEcheance),
      joursRetard: creance.joursRetard,
      statut: creance.statut,
      priorite: creance.priorite,
      dernierContact: this.formatDate(creance.dernierContact),
      relances: creance.relances
    };
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    try {
      const date = new Date(dateString);
      if (isNaN(date.getTime())) {
        return dateString;
      }
      return date.toLocaleDateString('fr-FR', {
        day: 'numeric',
        month: 'long',
        year: 'numeric'
      });
    } catch (e) {
      return dateString;
    }
  }

  setFilter(filter: string) {
    this.activeFilter = filter;
  }

  get filteredCreances(): CreanceDisplay[] {
    let filtered = this.creances;

    if (this.activeFilter === 'en-retard') {
      filtered = filtered.filter((c: CreanceDisplay) => c.statut === 'en-retard');
    } else if (this.activeFilter === 'recouvre') {
      filtered = filtered.filter((c: CreanceDisplay) => c.statut === 'recouvre');
    } else if (this.activeFilter === 'impaye') {
      filtered = filtered.filter((c: CreanceDisplay) => c.statut === 'impaye');
    } else if (this.activeFilter === 'haute-priorite') {
      filtered = filtered.filter((c: CreanceDisplay) => c.priorite === 'haute');
    }

    if (this.searchTerm) {
      filtered = filtered.filter((c: CreanceDisplay) =>
        c.facture.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        c.client.nom.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        c.client.email.toLowerCase().includes(this.searchTerm.toLowerCase())
      );
    }

    return filtered;
  }

  relancer(creance: CreanceDisplay) {
  }

  contacter(creance: CreanceDisplay) {
  }

  marquerRecouvre(creance: CreanceDisplay) {
  }

  viewCreance(creance: CreanceDisplay) {
  }
}
