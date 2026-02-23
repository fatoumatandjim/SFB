import { Injectable } from '@angular/core';
import { jsPDF } from 'jspdf';
import autoTable from 'jspdf-autotable';

export interface PdfTableColumn {
  header: string;
  dataKey: string;
  width?: number;
}

export interface PdfExportOptions {
  title: string;
  subtitle?: string;
  filename?: string;
  columns: PdfTableColumn[];
  data: any[];
  dateRange?: {
    startDate?: string;
    endDate?: string;
    date?: string;
  };
}

/** Options pour le rapport PDF "Camions attribués aux clients" */
export interface RapportCamionsClientsOptions {
  date?: string;
  startDate?: string;
  endDate?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PdfService {

  constructor() { }

  exportTable(options: PdfExportOptions): void {
    const doc = new jsPDF('l', 'mm', 'a4'); // Landscape orientation for tables

    // Add title
    doc.setFontSize(18);
    doc.setFont('helvetica', 'bold');
    doc.text(options.title, 14, 15);

    // Add subtitle if provided
    if (options.subtitle) {
      doc.setFontSize(12);
      doc.setFont('helvetica', 'normal');
      doc.text(options.subtitle, 14, 22);
    }

    // Add date range info if provided
    let yPosition = options.subtitle ? 29 : 22;
    if (options.dateRange) {
      doc.setFontSize(10);
      doc.setFont('helvetica', 'italic');
      let dateInfo = '';
      if (options.dateRange.date) {
        dateInfo = `Date: ${this.formatDate(options.dateRange.date)}`;
      } else if (options.dateRange.startDate && options.dateRange.endDate) {
        dateInfo = `Période: Du ${this.formatDate(options.dateRange.startDate)} au ${this.formatDate(options.dateRange.endDate)}`;
      }
      if (dateInfo) {
        doc.text(dateInfo, 14, yPosition);
        yPosition += 7;
      }
    }

    // Add generation date
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    const now = new Date();
    doc.text(`Généré le: ${now.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' })}`, 14, yPosition);

    // Prepare table data
    const tableData = options.data.map(item => {
      return options.columns.map(col => {
        const value = this.getNestedValue(item, col.dataKey);
        return value !== null && value !== undefined ? String(value) : '';
      });
    });

    const headers = options.columns.map(col => col.header);

    // Add table
    autoTable(doc, {
      head: [headers],
      body: tableData,
      startY: yPosition + 5,
      styles: {
        fontSize: 8,
        cellPadding: 3,
        overflow: 'linebreak',
        cellWidth: 'wrap'
      },
      headStyles: {
        fillColor: [59, 130, 246], // Blue color
        textColor: 255,
        fontStyle: 'bold',
        fontSize: 9
      },
      alternateRowStyles: {
        fillColor: [249, 250, 251] // Light gray
      },
      margin: { top: yPosition + 5, left: 14, right: 14 },
      didDrawPage: (data: any) => {
        // Add page number
        doc.setFontSize(8);
        doc.text(
          `Page ${data.pageNumber}`,
          doc.internal.pageSize.getWidth() / 2,
          doc.internal.pageSize.getHeight() - 10,
          { align: 'center' }
        );
      }
    });

    // Save PDF
    const filename = options.filename || `${options.title.toLowerCase().replace(/\s+/g, '_')}_${now.toISOString().split('T')[0]}.pdf`;
    doc.save(filename);
  }

  private getNestedValue(obj: any, path: string): any {
    return path.split('.').reduce((current, prop) => current?.[prop], obj);
  }

  private formatDate(dateString: string): string {
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('fr-FR', {
        day: 'numeric',
        month: 'long',
        year: 'numeric'
      });
    } catch (e) {
      return dateString;
    }
  }

  formatCurrency(amount: number | undefined | null): string {
    if (amount === null || amount === undefined) return '0 FCFA';
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'XOF',
      minimumFractionDigits: 0
    }).format(amount);
  }
  private formatMontant(value: number | undefined | null): string {
    const num = value || 0;
    // Format sans décimales, avec espaces pour les milliers
    const formatted = Math.round(num).toString();
    // Ajouter des espaces comme séparateurs de milliers
    return formatted.replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
  }

  formatDateForPdf(dateString: string | undefined): string {
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

  /**
   * Génère un rapport PDF des camions attribués aux clients.
   * Colonnes : Immatriculation, Nom chauffeur, N° chauffeur, Clients, Date départ.
   * @param voyages Liste des voyages (avec au moins un client)
   * @param options Optionnel : filtre par date ou intervalle (pour l'en-tête). Si absent, affiche "Voyages en cours (non déchargés)".
   */
  exportRapportCamionsClients(voyages: any[], options?: RapportCamionsClientsOptions): void {
    const doc = new jsPDF('l', 'mm', 'a4');
    const pageWidth = doc.internal.pageSize.getWidth();
    const pageHeight = doc.internal.pageSize.getHeight();

    const blueColor: [number, number, number] = [30, 64, 175];
    const orangeColor: [number, number, number] = [249, 115, 22];
    const lightGray: [number, number, number] = [248, 250, 252];

    // En-tête avec bandeau
    doc.setFillColor(blueColor[0], blueColor[1], blueColor[2]);
    doc.rect(0, 0, pageWidth, 22, 'F');
    doc.setFillColor(orangeColor[0], orangeColor[1], orangeColor[2]);
    doc.rect(0, 0, 18, 22, 'F');

    doc.setTextColor(255, 255, 255);
    doc.setFontSize(16);
    doc.setFont('helvetica', 'bold');
    doc.text('Rapport - Camions attribués aux clients', 24, 14);

    let subtitleY = 28;
    let periodText = '';
    if (options?.date) {
      periodText = `Date : ${this.formatDate(options.date)}`;
    } else if (options?.startDate && options?.endDate) {
      periodText = `Période : du ${this.formatDate(options.startDate)} au ${this.formatDate(options.endDate)}`;
    } else {
      periodText = 'Voyages en cours (non déchargés)';
    }
    doc.setFontSize(11);
    doc.setFont('helvetica', 'normal');
    doc.text(periodText, 24, subtitleY);
    subtitleY += 8;

    doc.setTextColor(0, 0, 0);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    const now = new Date();
    doc.text(`Généré le ${now.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric', hour: '2-digit', minute: '2-digit' })}`, 24, subtitleY);
    const startY = subtitleY + 10;

    // En-têtes du tableau
    const headers = ['Immatriculation', 'Nom du chauffeur', 'N° chauffeur', 'Clients attribués', 'Date départ'];
    const tableData = voyages.map(v => {
      const clients = (v.clientVoyages || [])
        .map((cv: any) => cv.clientNom || '—')
        .filter((n: string) => n !== '—');
      const clientsStr = clients.length ? clients.join(', ') : '—';
      const dateDepart = v.dateDepart ? this.formatDate(v.dateDepart) : '—';
      return [
        v.camionImmatriculation || '—',
        v.chauffeur || '—',
        v.numeroChauffeur || '—',
        clientsStr,
        dateDepart
      ];
    });

    autoTable(doc, {
      head: [headers],
      body: tableData,
      startY,
      styles: {
        fontSize: 9,
        cellPadding: 4,
        overflow: 'linebreak',
        cellWidth: 'wrap'
      },
      headStyles: {
        fillColor: blueColor,
        textColor: [255, 255, 255],
        fontStyle: 'bold',
        fontSize: 9
      },
      alternateRowStyles: {
        fillColor: lightGray
      },
      margin: { left: 14, right: 14 },
      columnStyles: {
        0: { cellWidth: 38 },
        1: { cellWidth: 42 },
        2: { cellWidth: 32 },
        3: { cellWidth: 65 },
        4: { cellWidth: 38 }
      },
      didDrawPage: (data: any) => {
        doc.setFontSize(8);
        doc.setTextColor(100, 100, 100);
        doc.text(
          `Page ${data.pageNumber}`,
          pageWidth / 2,
          pageHeight - 12,
          { align: 'center' }
        );
      }
    });

    // Pied de page
    doc.setDrawColor(blueColor[0], blueColor[1], blueColor[2]);
    doc.setLineWidth(0.4);
    doc.line(14, pageHeight - 18, pageWidth - 14, pageHeight - 18);
    doc.setFontSize(8);
    doc.setTextColor(0, 0, 0);
    doc.text('SFB SARL — Torokorobougou Pont Roi Fadh. — Tél: +223 77771616', 14, pageHeight - 10);

    const filename = options?.date
      ? `Rapport_camions_clients_${options.date}.pdf`
      : options?.startDate && options?.endDate
        ? `Rapport_camions_clients_${options.startDate}_${options.endDate}.pdf`
        : `Rapport_camions_clients_en_cours_${now.toISOString().split('T')[0]}.pdf`;
    doc.save(filename);
  }

  /**
   * Génère un bon d'enlèvement pour un ou plusieurs voyages
   * @param voyages Liste des voyages à inclure dans le bon
   * @param bonNumber Numéro du bon (optionnel, si non fourni, utilise le numéro du voyage)
   */
  async generateBonEnlevement(voyages: any[], bonNumber?: string): Promise<void> {
    if (!voyages || voyages.length === 0) {
      console.error('Aucun voyage fourni pour le bon d\'enlèvement');
      return;
    }

    // Grouper les voyages par camion
    const voyagesByCamion = new Map<string, any[]>();
    voyages.forEach(voyage => {
      const camionKey = voyage.camionImmatriculation || 'UNKNOWN';
      if (!voyagesByCamion.has(camionKey)) {
        voyagesByCamion.set(camionKey, []);
      }
      voyagesByCamion.get(camionKey)!.push(voyage);
    });

    // Générer un PDF par camion
    const promises: Promise<void>[] = [];
    voyagesByCamion.forEach((camionVoyages, camionImmatriculation) => {
      promises.push(this.generateBonEnlevementForCamion(camionVoyages, camionImmatriculation, bonNumber));
    });

    await Promise.all(promises);
  }

  private async generateBonEnlevementForCamion(voyages: any[], camionImmatriculation: string, bonNumber?: string): Promise<void> {
    const doc = new jsPDF('p', 'mm', 'a4');
    const pageWidth = doc.internal.pageSize.getWidth();
    const pageHeight = doc.internal.pageSize.getHeight();

    // Couleurs (tuples pour TypeScript)
    const blueColor: [number, number, number] = [59, 130, 246]; // #3b82f6
    const orangeColor: [number, number, number] = [249, 115, 22]; // #f97316
    const darkBlue: [number, number, number] = [30, 64, 175]; // #1e40af

    // En-tête avec barre bleue et orange (diagonale)
    doc.setFillColor(blueColor[0], blueColor[1], blueColor[2]);
    doc.rect(0, 0, pageWidth, 12, 'F');
    // Barre orange diagonale en haut à gauche
    doc.setFillColor(orangeColor[0], orangeColor[1], orangeColor[2]);
    doc.rect(0, 0, 25, 12, 'F');

    // Charger et ajouter l'image bon.png
    try {
      const logoImg = await this.loadImage('/assets/bon.png');
      // Positionner l'image en haut à droite
      doc.addImage(logoImg, 'PNG', pageWidth - 35, 2, 30, 10);
    } catch (error) {
      console.error('Erreur lors du chargement de l\'image bon.png:', error);
      // Fallback sur le texte si l'image ne charge pas
      doc.setTextColor(255, 255, 255);
      doc.setFontSize(18);
      doc.setFont('helvetica', 'bold');
      doc.text('SFB', pageWidth - 30, 8);
      doc.setFontSize(7);
      doc.setFont('helvetica', 'normal');
      doc.text('SARL', pageWidth - 30, 12);
    }

    // Date (centrée, en gras)
    const today = new Date();
    const dateStr = today.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
    doc.setFontSize(11);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(0, 0, 0);
    doc.text(`DATE : ${dateStr}`, pageWidth / 2, 28, { align: 'center' });

    // Numéro de bon d'enlèvement (centré, en gras, souligné)
    // Utiliser le numéro du voyage s'il existe, sinon générer un numéro
    const bonNum = bonNumber || voyages[0]?.numeroBonEnlevement || this.generateBonNumber(voyages[0]?.id || 0, today.getFullYear());
    doc.setFontSize(13);
    doc.setFont('helvetica', 'bold');
    doc.text(`BON D'ENLEVEMENT N°: ${bonNum}`, pageWidth / 2, 36, { align: 'center' });

    // Ligne de soulignement sous le titre
    doc.setLineWidth(0.5);
    doc.setDrawColor(0, 0, 0);
    doc.line(30, 39, pageWidth - 30, 39);

    // Tableau des voyages
    const tableData = voyages.map(voyage => [
      voyage.produitNom || voyage.typeProduit || 'N/A',
      voyage.camionImmatriculation || 'N/A',
      voyage.quantite ? this.formatMontant(voyage.quantite) : '0',
      voyage.depotNom || 'N/A'
    ]);

    autoTable(doc, {
      head: [['PRODUIT', 'N° D\'IMMATRICULATION', 'VOLUME EN LITRE', 'DEPOT']],
      body: tableData,
      startY: 45,
      styles: {
        fontSize: 9,
        cellPadding: 4,
        textColor: [0, 0, 0],
        font: 'helvetica',
        lineWidth: 0.1
      },
      headStyles: {
        fillColor: [darkBlue[0], darkBlue[1], darkBlue[2]] as [number, number, number],
        textColor: [255, 255, 255],
        fontStyle: 'bold',
        fontSize: 9,
        lineWidth: 0.1
      },
      alternateRowStyles: {
        fillColor: [255, 255, 255]
      },
      margin: { top: 45, left: 20, right: 20 },
      columnStyles: {
        0: { cellWidth: 45 },
        1: { cellWidth: 55 },
        2: { cellWidth: 40 },
        3: { cellWidth: 40 }
      }
    });

    // Section signature
    const finalY = (doc as any).lastAutoTable.finalY || 150;
    doc.setFontSize(11);
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(0, 0, 0);
    doc.text('Signature', pageWidth - 25, finalY + 25, { align: 'right' });
    doc.setLineWidth(0.5);
    doc.setDrawColor(0, 0, 0);
    doc.line(pageWidth - 50, finalY + 27, pageWidth - 20, finalY + 27);

    // Tampon de signature avec image bon.png
    try {
      const stampImg = await this.loadImage('/assets/bon.png');
      // Tampon rectangulaire avec l'image
      doc.setFillColor(240, 240, 240);
      doc.roundedRect(pageWidth - 52, finalY + 30, 48, 28, 2, 2, 'F');
      // Ajouter l'image dans le tampon (plus petite)
      // doc.addImage(stampImg, 'PNG', pageWidth - 50, finalY + 32, 44, 24);
    } catch (error) {
      console.error('Erreur lors du chargement de l\'image pour le tampon:', error);
      // Fallback sur le texte si l'image ne charge pas
      doc.setFillColor(240, 240, 240);
      doc.roundedRect(pageWidth - 52, finalY + 30, 48, 28, 2, 2, 'F');
      doc.setFontSize(7);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(0, 0, 0);
      doc.text('SFB - SARL', pageWidth - 28, finalY + 37, { align: 'center' });
      doc.text('DIRECTEUR GENERAL', pageWidth - 28, finalY + 42, { align: 'center' });
      doc.setFontSize(6);
      doc.setFont('helvetica', 'normal');
      doc.text('Tél: 77 77 16 16 / 44 90 59 11', pageWidth - 28, finalY + 47, { align: 'center' });
      doc.text('Torokorobougou, Bamako, Mali', pageWidth - 28, finalY + 52, { align: 'center' });
    }

    // Footer
    doc.setLineWidth(0.5);
    doc.setDrawColor(blueColor[0], blueColor[1], blueColor[2]);
    doc.line(0, pageHeight - 30, pageWidth, pageHeight - 30);

    doc.setFontSize(8);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(0, 0, 0);
    doc.text('Adresse: Torokorobougou Pont Roi Fadh.', 20, pageHeight - 25);
    doc.text('Tel: +223 77771616', 20, pageHeight - 20);
    doc.text('Email: hyattassaye87@gmail.com', 20, pageHeight - 15);
    doc.text('RC N.0 MA-BKO-2019-B-13952.-NIF: 085143483V', 20, pageHeight - 10);
    doc.text('- ECOBANK-ML090 01001 151824705001', 20, pageHeight - 5);
    doc.text('- BMS-ML 102 01 001 056373202001-28', 20, pageHeight);

    // Barre bleue et orange en bas
    doc.setFillColor(blueColor[0], blueColor[1], blueColor[2]);
    doc.rect(0, pageHeight - 2, pageWidth, 2, 'F');
    doc.setFillColor(orangeColor[0], orangeColor[1], orangeColor[2]);
    doc.rect(0, pageHeight - 2, 20, 2, 'F');

    // Sauvegarder le PDF
    const filename = `Bon_Enlevement_${camionImmatriculation}_${bonNum.replace(/\//g, '_')}.pdf`;
    doc.save(filename);
  }

  private generateBonNumber(voyageId: number, year: number): string {
    // Générer un numéro de bon au format: 00291-SFB/2025
    const number = String(voyageId).padStart(5, '0');
    return `${number}-SFB/${year}`;
  }

  private loadImage(src: string): Promise<string> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext('2d');
        if (ctx) {
          ctx.drawImage(img, 0, 0);
          const dataURL = canvas.toDataURL('image/png');
          resolve(dataURL);
        } else {
          reject(new Error('Impossible de créer le contexte canvas'));
        }
      };
      img.onerror = () => reject(new Error(`Erreur lors du chargement de l'image: ${src}`));
      img.src = src;
    });
  }
}

