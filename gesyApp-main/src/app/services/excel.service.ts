import { Injectable } from '@angular/core';
import * as XLSX from 'xlsx';

export interface CamionExcelData {
  numero: number;
  transporteur: string;
  vehicule: string;
  capacite: number;
  axe: string;
  bonEnlevement: string;
}

export interface ListeCamionsExcelOptions {
  camions: CamionExcelData[];
  depotNom: string;
  date?: Date;
  filename?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ExcelService {

  constructor() { }

  /**
   * Génère un fichier Excel avec la liste des camions pour un dépôt
   * Format conforme à l'image fournie
   */
  generateListeCamions(options: ListeCamionsExcelOptions): void {
    const date = options.date || new Date();
    const dateStr = date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });

    // Créer un nouveau workbook
    const wb = XLSX.utils.book_new();

    // Calculer le total de capacité
    const totalCapacite = options.camions.reduce((sum, camion) => sum + (camion.capacite || 0), 0);

    // Créer les données complètes avec en-têtes et tableau
    const allData: any[][] = [];
    
    // En-têtes du document
    allData.push([`LISTE DE ${options.camions.length} CAMIONS CITERNES SFB PETROLEUM SA`]);
    allData.push([dateStr]);
    allData.push([`DEPOT : ${options.depotNom}`]);
    allData.push(['']); // Ligne vide
    
    // En-tête du tableau
    allData.push(['Numer', 'TRANSPORTEUR', 'VEHICULE', 'CAPACIT', 'AXE', 'N°BON D\'ENLEVEMENT']);
    
    // Données des camions
    options.camions.forEach((camion, index) => {
      allData.push([
        index + 1,
        camion.transporteur || 'SFB PETROLEUM SA',
        camion.vehicule,
        camion.capacite,
        camion.axe || '',
        camion.bonEnlevement
      ]);
    });
    
    // Ligne de total
    allData.push(['Total litrage', '', '', totalCapacite, '', '']);

    // Créer la feuille de calcul
    const ws = XLSX.utils.aoa_to_sheet(allData);

    // Définir les largeurs de colonnes
    ws['!cols'] = [
      { wch: 8 },   // Numer
      { wch: 25 },  // TRANSPORTEUR
      { wch: 30 },  // VEHICULE
      { wch: 12 },  // CAPACIT
      { wch: 20 },  // AXE
      { wch: 20 }   // N°BON D'ENLEVEMENT
    ];

    // Fusionner les cellules pour le titre, date et dépôt
    if (!ws['!merges']) ws['!merges'] = [];
    ws['!merges'].push({ s: { r: 0, c: 0 }, e: { r: 0, c: 5 } }); // Titre
    ws['!merges'].push({ s: { r: 1, c: 0 }, e: { r: 1, c: 5 } }); // Date
    ws['!merges'].push({ s: { r: 2, c: 0 }, e: { r: 2, c: 5 } }); // Dépôt

    // Ajouter la feuille au workbook
    XLSX.utils.book_append_sheet(wb, ws, 'Liste Camions');

    // Générer le nom du fichier
    const filename = options.filename || `Liste_Camions_${options.depotNom}_${dateStr.replace(/\//g, '_')}.xlsx`;

    // Télécharger le fichier
    XLSX.writeFile(wb, filename);
  }

  /**
   * Génère un fichier Excel simple avec des données tabulaires
   */
  exportToExcel(data: any[][], filename: string, sheetName: string = 'Sheet1'): void {
    const ws = XLSX.utils.aoa_to_sheet(data);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, sheetName);
    XLSX.writeFile(wb, filename);
  }
}

