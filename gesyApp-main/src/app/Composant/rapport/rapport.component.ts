import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RapportsService, RapportFinancier, DonneeMensuelle, CategorieDepense } from '../../services/rapports.service';
import { jsPDF } from 'jspdf';


@Component({
  selector: 'app-rapport',
  templateUrl: './rapport.component.html',
  styleUrls: ['./rapport.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class RapportComponent implements OnInit {
  selectedPeriod: string = 'mois';
  selectedYear: string = '2024';
  isLoading: boolean = false;
  customStartDate: string = '';
  customEndDate: string = '';

  stats = {
    chiffreAffaires: {
      total: 0,
      evolution: '0%',
      periode: 'Ce mois'
    },
    depenses: {
      total: 0,
      evolution: '0%',
      periode: 'Ce mois'
    },
    benefice: {
      total: 0,
      evolution: '0%',
      periode: 'Ce mois'
    },
    marge: {
      pourcentage: 0,
      evolution: '0%'
    }
  };

  donneesMensuelles: DonneeMensuelle[] = [];
  categoriesDepenses: CategorieDepense[] = [];
  fraisDouaniers: any = null;
  pertes: any = null;

  constructor(private rapportsService: RapportsService) { }

  ngOnInit() {
    this.loadRapport();
  }

  loadRapport() {
    this.isLoading = true;

    let dateDebut: string | undefined;
    let dateFin: string | undefined;

    if (this.selectedPeriod === 'personnalise') {
      dateDebut = this.customStartDate;
      dateFin = this.customEndDate;
    }

    const annee = this.selectedPeriod === 'annee' ? parseInt(this.selectedYear) : undefined;

    this.rapportsService.getRapportFinancier(this.selectedPeriod, annee, dateDebut, dateFin).subscribe({
      next: (data: RapportFinancier) => {
        this.stats = data.stats;
        this.donneesMensuelles = data.donneesMensuelles;
        this.categoriesDepenses = data.categoriesDepenses;
        this.fraisDouaniers = data.fraisDouaniers || null;
        this.pertes = data.pertes || null;
        this.isLoading = false;
      },
      error: (error: any) => {
        console.error('Erreur lors du chargement du rapport:', error);
        this.isLoading = false;
      }
    });
  }

  onPeriodChange() {
    this.loadRapport();
  }

  onYearChange() {
    if (this.selectedPeriod === 'annee') {
      this.loadRapport();
    }
  }

  onCustomDateChange() {
    if (this.selectedPeriod === 'personnalise' && this.customStartDate && this.customEndDate) {
      this.loadRapport();
    }
  }

  get maxChiffreAffaires(): number {
    return Math.max(...this.donneesMensuelles.map(d => d.chiffreAffaires));
  }

  get maxDepenses(): number {
    return Math.max(...this.donneesMensuelles.map(d => d.depenses));
  }

  getBarHeight(value: number, max: number): number {
    return (value / max) * 100;
  }

  exporterRapport() {
    this.generateRapportPdf();
  }

  private generateRapportPdf() {
    const doc = new jsPDF('p', 'mm', 'a4');
    const pageWidth = doc.internal.pageSize.getWidth();
    const pageHeight = doc.internal.pageSize.getHeight();
    const margin = 15;
    let currentY = margin;

    // Couleurs
    const colorGreen = [16, 185, 129]; // #10b981
    const colorRed = [239, 68, 68]; // #ef4444
    const colorBlue = [59, 130, 246]; // #3b82f6
    const colorPurple = [139, 92, 246]; // #8b5cf6
    const colorOrange = [249, 115, 22]; // #f97316
    const colorGrey = [107, 114, 128]; // #6b7280
    const colorLightGrey = [243, 244, 246]; // #f3f4f6

    // --- En-tête entreprise ---
    doc.setFontSize(20);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(0, 0, 0);
    doc.text('SFB', margin, currentY);
    currentY += 6;
    doc.setFontSize(10);
    doc.setFont('helvetica', 'normal');
    doc.text('Gestion logistique & facturation', margin, currentY);
    currentY += 4;
    doc.text('Rapport Financier', margin, currentY);
    currentY += 10;

    // --- Période du rapport ---
    const periodeText = this.getPeriodeText();
    doc.setFontSize(12);
    doc.setFont('helvetica', 'bold');
    doc.text('Période:', margin, currentY);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(11);
    doc.text(periodeText, margin + 25, currentY);
    currentY += 10;

    // --- Statistiques principales ---
    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text('Résumé Financier', margin, currentY);
    currentY += 8;

    const statsY = currentY;
    const statBoxWidth = (pageWidth - 2 * margin - 15) / 4;
    const statBoxHeight = 25;

    // Chiffre d'Affaires
    doc.setFillColor(colorGreen[0], colorGreen[1], colorGreen[2]);
    doc.rect(margin, currentY, statBoxWidth, statBoxHeight, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.text('Chiffre d\'Affaires', margin + 3, currentY + 5);
    doc.setFontSize(12);
    doc.setFont('helvetica', 'bold');
    doc.text(this.formatMontant(this.stats.chiffreAffaires.total) + ' F', margin + 3, currentY + 12);
    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    doc.text(this.stats.chiffreAffaires.evolution, margin + 3, currentY + 18);
    doc.text(this.stats.chiffreAffaires.periode, margin + 3, currentY + 22);

    // Dépenses
    doc.setFillColor(colorRed[0], colorRed[1], colorRed[2]);
    doc.rect(margin + statBoxWidth + 5, currentY, statBoxWidth, statBoxHeight, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.text('Dépenses', margin + statBoxWidth + 8, currentY + 5);
    doc.setFontSize(12);
    doc.setFont('helvetica', 'bold');
    doc.text(this.formatMontant(this.stats.depenses.total) + ' F', margin + statBoxWidth + 8, currentY + 12);
    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    doc.text(this.stats.depenses.evolution, margin + statBoxWidth + 8, currentY + 18);
    doc.text(this.stats.depenses.periode, margin + statBoxWidth + 8, currentY + 22);

    // Bénéfice Net
    doc.setFillColor(colorBlue[0], colorBlue[1], colorBlue[2]);
    doc.rect(margin + (statBoxWidth + 5) * 2, currentY, statBoxWidth, statBoxHeight, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.text('Bénéfice Net', margin + (statBoxWidth + 5) * 2 + 3, currentY + 5);
    doc.setFontSize(12);
    doc.setFont('helvetica', 'bold');
    doc.text(this.formatMontant(this.stats.benefice.total) + ' F', margin + (statBoxWidth + 5) * 2 + 3, currentY + 12);
    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    doc.text(this.stats.benefice.evolution, margin + (statBoxWidth + 5) * 2 + 3, currentY + 18);
    doc.text(this.stats.benefice.periode, margin + (statBoxWidth + 5) * 2 + 3, currentY + 22);

    // Marge Bénéficiaire
    doc.setFillColor(colorPurple[0], colorPurple[1], colorPurple[2]);
    doc.rect(margin + (statBoxWidth + 5) * 3, currentY, statBoxWidth, statBoxHeight, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.text('Marge', margin + (statBoxWidth + 5) * 3 + 3, currentY + 5);
    doc.setFontSize(12);
    doc.setFont('helvetica', 'bold');
    doc.text(this.stats.marge.pourcentage + '%', margin + (statBoxWidth + 5) * 3 + 3, currentY + 12);
    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    doc.text(this.stats.marge.evolution, margin + (statBoxWidth + 5) * 3 + 3, currentY + 18);
    doc.text('Marge nette', margin + (statBoxWidth + 5) * 3 + 3, currentY + 22);

    currentY += statBoxHeight + 15;
    doc.setTextColor(0, 0, 0);

    // --- Évolution Mensuelle (Graphique) ---
    if (currentY > pageHeight - 80) {
      doc.addPage();
      currentY = margin;
    }

    // Réduire les marges latérales pour le graphique pour le rendre plus large
    const chartMargin = 10;
    const chartWidth = pageWidth - 2 * chartMargin;

    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text('Évolution Mensuelle', chartMargin, currentY);
    currentY += 8;

    const chartY = currentY;
    const chartHeight = 50;
    // Calculer maxValue avec protection contre les cas limites
    const maxCA = this.donneesMensuelles.length > 0
      ? Math.max(...this.donneesMensuelles.map(d => d.chiffreAffaires || 0), 0)
      : 0;
    const maxDep = this.donneesMensuelles.length > 0
      ? Math.max(...this.donneesMensuelles.map(d => d.depenses || 0), 0)
      : 0;
    const maxValue = Math.max(maxCA, maxDep, 1); // Au minimum 1 pour éviter division par 0
    const numMonths = this.donneesMensuelles.length;

    // Calculer l'espace disponible pour chaque mois (paire de barres + espace)
    const spacePerMonth = chartWidth / numMonths;
    const barWidth = spacePerMonth * 0.35; // Largeur de chaque barre
    const gapBetweenBars = spacePerMonth * 0.1; // Espace entre les deux barres d'un mois
    const startOffset = spacePerMonth * 0.1; // Décalage initial

    // Fond du graphique
    doc.setFillColor(colorLightGrey[0], colorLightGrey[1], colorLightGrey[2]);
    doc.rect(chartMargin, currentY, chartWidth, chartHeight, 'F');

    // Barres
    this.donneesMensuelles.forEach((donnee, index) => {
      // Conversion robuste en nombre
      let caValue = 0;
      let depValue = 0;

      if (donnee.chiffreAffaires != null) {
        caValue = typeof donnee.chiffreAffaires === 'number'
          ? donnee.chiffreAffaires
          : parseFloat(String(donnee.chiffreAffaires)) || 0;
      }

      if (donnee.depenses != null) {
        depValue = typeof donnee.depenses === 'number'
          ? donnee.depenses
          : parseFloat(String(donnee.depenses)) || 0;
      }
      // Calculer les hauteurs avec minimum de 6 pour les valeurs > 0
      const minBarHeight = 6;
      let caHeight = (caValue / maxValue) * chartHeight;
      let depHeight = (depValue / maxValue) * chartHeight;

      // Appliquer la hauteur minimum si la valeur est > 0
      if (caValue > 0 && caHeight < minBarHeight) {
        caHeight = minBarHeight;
      } else if (caValue === 0) {
        caHeight = 0;
      }

      if (depValue > 0 && depHeight < minBarHeight) {
        depHeight = minBarHeight;
      } else if (depValue === 0) {
        depHeight = 0;
      }

      // Position X pour ce mois (distribuée uniformément)
      const monthX = chartMargin + startOffset + (index * spacePerMonth);

      // Barre Chiffre d'Affaires (vert)
      doc.setFillColor(colorGreen[0], colorGreen[1], colorGreen[2]);
      if (caHeight > 0) {
        doc.rect(monthX, currentY + chartHeight - caHeight, barWidth, caHeight, 'F');
      }

      // Valeur sur la barre CA - toujours afficher si valeur > 0
      if (caValue > 0) {
        doc.setFontSize(6);
        doc.setFont('helvetica', 'bold');
        const caText = this.formatMontantK(caValue);
        const caTextX = monthX + barWidth / 2;

        // Si la barre est assez haute, mettre le texte à l'intérieur (blanc)
        if (caHeight >= 8) {
          doc.setTextColor(255, 255, 255);
          const caTextY = currentY + chartHeight - caHeight / 2;
          doc.text(caText, caTextX, caTextY, { align: 'center' });
        } else {
          // Si la barre est petite, mettre le texte au-dessus (noir)
          doc.setTextColor(0, 0, 0);
          const caTextY = currentY + chartHeight - caHeight - 2;
          doc.text(caText, caTextX, caTextY, { align: 'center' });
        }
      }

      // Barre Dépenses (rouge)
      doc.setFillColor(colorRed[0], colorRed[1], colorRed[2]);
      if (depHeight > 0) {
        doc.rect(monthX + barWidth + gapBetweenBars, currentY + chartHeight - depHeight, barWidth, depHeight, 'F');
      }

      // Valeur sur la barre Dépenses - toujours afficher si valeur > 0
      if (depValue > 0) {
        doc.setFontSize(6);
        doc.setFont('helvetica', 'bold');
        const depText = this.formatMontantK(depValue);
        const depTextX = monthX + barWidth + gapBetweenBars + barWidth / 2;

        // Si la barre est assez haute, mettre le texte à l'intérieur (blanc)
        if (depHeight >= 8) {
          doc.setTextColor(255, 255, 255);
          const depTextY = currentY + chartHeight - depHeight / 2;
          doc.text(depText, depTextX, depTextY, { align: 'center' });
        } else {
          // Si la barre est petite, mettre le texte au-dessus (noir)
          doc.setTextColor(0, 0, 0);
          const depTextY = currentY + chartHeight - depHeight - 2;
          doc.text(depText, depTextX, depTextY, { align: 'center' });
        }
      }

      // Label du mois (centré sur la paire de barres)
      doc.setFontSize(7);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(0, 0, 0);
      const monthLabelX = monthX + barWidth + gapBetweenBars / 2;
      doc.text(donnee.mois.substring(0, 3), monthLabelX, currentY + chartHeight + 5, { align: 'center' });
    });

    // Légende
    currentY += chartHeight + 12;
    doc.setFontSize(9);
    doc.setFillColor(colorGreen[0], colorGreen[1], colorGreen[2]);
    doc.rect(margin, currentY, 5, 5, 'F');
    doc.setTextColor(0, 0, 0);
    doc.text('Chiffre d\'Affaires', margin + 8, currentY + 4);
    doc.setFillColor(colorRed[0], colorRed[1], colorRed[2]);
    doc.rect(margin + 60, currentY, 5, 5, 'F');
    doc.text('Dépenses', margin + 68, currentY + 4);

    currentY += 15;

    // --- Répartition des Dépenses ---
    if (currentY > pageHeight - 60) {
      doc.addPage();
      currentY = margin;
    }

    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text('Répartition des Dépenses', margin, currentY);
    currentY += 8;

    const colorsMap: { [key: string]: number[] } = {
      'blue': [59, 130, 246],
      'green': [16, 185, 129],
      'purple': [139, 92, 246],
      'orange': [249, 115, 22],
      'red': [239, 68, 68],
      'teal': [20, 184, 166],
      'pink': [236, 72, 153],
      'indigo': [99, 102, 241]
    };

    this.categoriesDepenses.forEach((categorie) => {
      if (currentY > pageHeight - 20) {
        doc.addPage();
        currentY = margin;
      }

      const color = colorsMap[categorie.couleur] || colorGrey;
      const barWidthPercent = categorie.pourcentage;

      // Barre de progression
      doc.setFillColor(color[0], color[1], color[2]);
      doc.rect(margin, currentY, (chartWidth * barWidthPercent) / 100, 6, 'F');

      // Nom et montant
      doc.setFontSize(9);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(0, 0, 0);
      doc.text(categorie.nom, margin + 2, currentY + 4);
      doc.text(this.formatMontant(categorie.montant) + ' F', pageWidth - margin - 30, currentY + 4, { align: 'right' });
      doc.text(categorie.pourcentage + '%', pageWidth - margin - 5, currentY + 4, { align: 'right' });

      currentY += 10;
    });

    currentY += 10;

    // --- Frais Douaniers ---
    if (this.fraisDouaniers) {
      if (currentY > pageHeight - 60) {
        doc.addPage();
        currentY = margin;
      }

      doc.setFontSize(14);
      doc.setFont('helvetica', 'bold');
      doc.text('Frais Douaniers', margin, currentY);
      currentY += 8;

      // Frais de Douane
      doc.setFontSize(9);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(0, 0, 0);
      doc.setFillColor(colorOrange[0], colorOrange[1], colorOrange[2]);
      doc.rect(margin, currentY, 5, 5, 'F');
      doc.text('Frais de Douane', margin + 8, currentY + 4);
      doc.text(this.formatMontant(this.fraisDouaniers.fraisDouane) + ' F', pageWidth - margin - 2, currentY + 4, { align: 'right' });
      currentY += 8;

      // Frais T1
      doc.setFillColor(colorRed[0], colorRed[1], colorRed[2]);
      doc.rect(margin, currentY, 5, 5, 'F');
      doc.text('Frais T1', margin + 8, currentY + 4);
      doc.text(this.formatMontant(this.fraisDouaniers.fraisT1) + ' F', pageWidth - margin - 2, currentY + 4, { align: 'right' });
      currentY += 8;

      // Total
      doc.setFont('helvetica', 'bold');
      doc.setFontSize(10);
      doc.text('Total Frais Douaniers', margin, currentY + 4);
      doc.text(this.formatMontant(this.fraisDouaniers.total) + ' F', pageWidth - margin - 2, currentY + 4, { align: 'right' });

      // Évolution
      doc.setFontSize(8);
      doc.setFont('helvetica', 'normal');
      const evolutionFrais = this.fraisDouaniers.evolution || '0%';
      const isPositiveFrais = evolutionFrais.startsWith('+');
      doc.setTextColor(isPositiveFrais ? colorGreen[0] : colorRed[0], isPositiveFrais ? colorGreen[1] : colorRed[1], isPositiveFrais ? colorGreen[2] : colorRed[2]);
      doc.text('Évolution: ' + evolutionFrais, margin + 80, currentY + 4);
      doc.setTextColor(0, 0, 0);

      currentY += 12;
    }

    // --- Pertes (Manquants) ---
    if (this.pertes) {
      if (currentY > pageHeight - 50) {
        doc.addPage();
        currentY = margin;
      }

      doc.setFontSize(14);
      doc.setFont('helvetica', 'bold');
      doc.text('Pertes (Manquants)', margin, currentY);
      currentY += 8;

      // Quantité totale
      doc.setFontSize(9);
      doc.setFont('helvetica', 'normal');
      doc.setTextColor(0, 0, 0);
      doc.setFillColor(colorRed[0], colorRed[1], colorRed[2]);
      doc.rect(margin, currentY, 5, 5, 'F');
      doc.text('Quantité Totale Manquante', margin + 8, currentY + 4);
      doc.text(this.formatMontant(this.pertes.quantiteTotale) + ' L', pageWidth - margin - 2, currentY + 4, { align: 'right' });
      currentY += 8;

      // Évolution
      doc.setFontSize(8);
      const evolutionPertes = this.pertes.evolution || '0%';
      const isPositivePertes = evolutionPertes.startsWith('+');
      doc.setTextColor(isPositivePertes ? colorGreen[0] : colorRed[0], isPositivePertes ? colorGreen[1] : colorRed[1], isPositivePertes ? colorGreen[2] : colorRed[2]);
      doc.text('Évolution: ' + evolutionPertes, margin, currentY + 4);
      doc.setTextColor(0, 0, 0);

      currentY += 12;
    }

    // --- Tableau des Détails Mensuels ---
    if (currentY > pageHeight - 50) {
      doc.addPage();
      currentY = margin;
    }

    doc.setFontSize(14);
    doc.setFont('helvetica', 'bold');
    doc.text('Détails Mensuels', margin, currentY);
    currentY += 10;

    // En-tête du tableau
    doc.setFillColor(colorLightGrey[0], colorLightGrey[1], colorLightGrey[2]);
    doc.rect(margin, currentY, chartWidth, 8, 'F');
    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(0, 0, 0);
    doc.text('Mois', margin + 2, currentY + 5);
    doc.text('Chiffre d\'Affaires', margin + 40, currentY + 5);
    doc.text('Dépenses', margin + 90, currentY + 5);
    doc.text('Bénéfice', margin + 130, currentY + 5);
    doc.text('Marge', pageWidth - margin - 2, currentY + 5, { align: 'right' });
    currentY += 8;

    // Lignes du tableau
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8);
    this.donneesMensuelles.forEach((donnee) => {
      if (currentY > pageHeight - 15) {
        doc.addPage();
        currentY = margin;
        // Réafficher l'en-tête
        doc.setFillColor(colorLightGrey[0], colorLightGrey[1], colorLightGrey[2]);
        doc.rect(margin, currentY, chartWidth, 8, 'F');
        doc.setFontSize(9);
        doc.setFont('helvetica', 'bold');
        doc.setTextColor(0, 0, 0);
        doc.text('Mois', margin + 2, currentY + 5);
        doc.text('Chiffre d\'Affaires', margin + 40, currentY + 5);
        doc.text('Dépenses', margin + 90, currentY + 5);
        doc.text('Bénéfice', margin + 130, currentY + 5);
        doc.text('Marge', pageWidth - margin - 2, currentY + 5, { align: 'right' });
        currentY += 8;
        doc.setFont('helvetica', 'normal');
        doc.setFontSize(8);
      }

      // Ligne avec couleur alternée
      const rowIndex = this.donneesMensuelles.indexOf(donnee);
      if (rowIndex % 2 === 0) {
        doc.setFillColor(250, 250, 250);
        doc.rect(margin, currentY - 2, chartWidth, 6, 'F');
      }

      doc.setTextColor(0, 0, 0);
      doc.text(donnee.mois, margin + 2, currentY + 4);

      doc.setTextColor(colorGreen[0], colorGreen[1], colorGreen[2]);
      doc.text(this.formatMontant(donnee.chiffreAffaires) + ' F', margin + 40, currentY + 4);

      doc.setTextColor(colorRed[0], colorRed[1], colorRed[2]);
      doc.text(this.formatMontant(donnee.depenses) + ' F', margin + 90, currentY + 4);

      if (donnee.benefice >= 0) {
        doc.setTextColor(colorBlue[0], colorBlue[1], colorBlue[2]);
      } else {
        doc.setTextColor(colorRed[0], colorRed[1], colorRed[2]);
      }
      doc.text(this.formatMontant(donnee.benefice) + ' F', margin + 130, currentY + 4);

      const marge = donnee.chiffreAffaires > 0 ? ((donnee.benefice / donnee.chiffreAffaires) * 100) : 0;
      doc.setTextColor(0, 0, 0);
      doc.text(marge.toFixed(1) + '%', pageWidth - margin - 2, currentY + 4, { align: 'right' });

      currentY += 6;
    });

    // --- Pied de page ---
    const pageHeights = doc.internal.pageSize.getHeight();
    let footerY = pageHeights - 20;

    // Ligne de séparation
    doc.setDrawColor(200, 200, 200);
    doc.line(20, footerY, pageWidth - 20, footerY);
    footerY += 5;
    // Informations du pied de page
    doc.setFontSize(8);
    doc.setTextColor(colorGrey[0], colorGrey[1], colorGrey[2]);
    doc.text('Siège social: Torokorobougou immeuble Wad motors', 20, footerY);
    footerY += 4;
    doc.text('RCCM: Ma bko 2024-M-3968', 20, footerY);
    footerY += 4;
    doc.text('Email: hyattassaye87@gmail.com', 20, footerY);
    footerY += 4;

    doc.setFontSize(7);
    doc.setTextColor(150, 150, 150);
    doc.text('SFB - Rapport généré automatiquement par le système de gestion de la société SFB le ' + new Date().toLocaleDateString('fr-FR'), 20, footerY);

    const fileName = `Rapport_Financier_SFB_${this.getFileNameDate()}.pdf`;
    doc.save(fileName);
  }

  private getPeriodeText(): string {
    switch (this.selectedPeriod) {
      case 'mois':
        return `Ce mois (${new Date().toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' })})`;
      case 'trimestre':
        const currentQuarter = Math.floor(new Date().getMonth() / 3) + 1;
        return `Ce trimestre (T${currentQuarter} ${new Date().getFullYear()})`;
      case 'annee':
        return `Année ${this.selectedYear}`;
      case 'personnalise':
        if (this.customStartDate && this.customEndDate) {
          const start = new Date(this.customStartDate).toLocaleDateString('fr-FR');
          const end = new Date(this.customEndDate).toLocaleDateString('fr-FR');
          return `Du ${start} au ${end}`;
        }
        return 'Période personnalisée';
      default:
        return 'Période non spécifiée';
    }
  }

  private getFileNameDate(): string {
    const now = new Date();
    return now.toISOString().split('T')[0].replace(/-/g, '');
  }

  private formatMontant(value: number | undefined | null): string {
    const n = Math.round(value || 0);
    const str = n.toString();
    return str.replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
  }

  private formatMontantK(value: number | undefined | null): string {
    const n = Math.round(value || 0);
    if (n >= 1000000) {
      // Format en millions (M)
      const mValue = n / 1000000;
      // Arrondir à 1 décimale si nécessaire, sinon entier
      if (mValue % 1 === 0) {
        return Math.round(mValue) + 'M';
      } else {
        // Utiliser une virgule comme séparateur décimal (format français)
        return mValue.toFixed(1).replace('.', ',') + 'M';
      }
    } else if (n >= 1000) {
      // Format en milliers (k)
      const kValue = n / 1000;
      // Arrondir à 1 décimale si nécessaire, sinon entier
      if (kValue % 1 === 0) {
        return Math.round(kValue) + 'k';
      } else {
        // Utiliser une virgule comme séparateur décimal (format français)
        return kValue.toFixed(1).replace('.', ',') + 'k';
      }
    }
    return n.toString();
  }

  genererRapport() {
    this.loadRapport();
  }
}
