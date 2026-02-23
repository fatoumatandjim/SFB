import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AnalysesService, Analyse, DonneeHebdomadaire, Tendance, Performance } from '../../services/analyses.service';


@Component({
  selector: 'app-analyse',
  templateUrl: './analyse.component.html',
  styleUrls: ['./analyse.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class AnalyseComponent implements OnInit {
  selectedPeriod: string = 'mois';
  selectedView: 'overview' | 'ventes' | 'operations' | 'performance' = 'overview';
  isLoading: boolean = false;

  stats = {
    croissance: {
      valeur: 0,
      evolution: '0%'
    },
    efficacite: {
      valeur: 0,
      evolution: '0%'
    },
    satisfaction: {
      valeur: 0,
      evolution: '0%'
    },
    rentabilite: {
      valeur: 0,
      evolution: '0%'
    }
  };

  donneesHebdomadaires: DonneeHebdomadaire[] = [];
  performances: Performance[] = [];
  tendances: Tendance[] = [];

  constructor(private analysesService: AnalysesService) { }

  ngOnInit() {
    this.loadAnalyse();
  }

  loadAnalyse() {
    this.isLoading = true;
    
    this.analysesService.getAnalyse(this.selectedPeriod).subscribe({
      next: (data: Analyse) => {
        this.stats = data.kpis;
        this.donneesHebdomadaires = data.donneesHebdomadaires;
        this.performances = data.performances;
        this.tendances = data.tendances;
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Erreur lors du chargement de l\'analyse:', error);
        this.isLoading = false;
      }
    });
  }

  onPeriodChange() {
    this.loadAnalyse();
  }

  get maxVentes(): number {
    return Math.max(...this.donneesHebdomadaires.map(d => d.ventes));
  }

  get maxClients(): number {
    return Math.max(...this.donneesHebdomadaires.map(d => d.clients));
  }

  get maxCamions(): number {
    return Math.max(...this.donneesHebdomadaires.map(d => d.camions));
  }

  getBarHeight(value: number, max: number): number {
    return (value / max) * 100;
  }

  getPerformanceColor(performance: Performance): string {
    if (performance.pourcentage >= 95) return 'green';
    if (performance.pourcentage >= 80) return 'orange';
    return 'red';
  }

  setView(view: 'overview' | 'ventes' | 'operations' | 'performance') {
    this.selectedView = view;
  }

  exporterAnalyse() {
  }
}
