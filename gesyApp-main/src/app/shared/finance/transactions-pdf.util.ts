import { PdfService, PdfExportOptions, PdfTableColumn } from '../../services/pdf.service';
import { Transaction } from '../../services/transactions.service';

/** Colonnes identiques au menu Paiements → export PDF des transactions. */
export const TRANSACTIONS_PDF_LIST_COLUMNS: PdfTableColumn[] = [
  { header: 'N°', dataKey: 'numero', width: 30 },
  { header: 'Référence', dataKey: 'reference', width: 50 },
  { header: 'Description', dataKey: 'description', width: 80 },
  { header: 'Date', dataKey: 'date', width: 50 },
  { header: 'Montant', dataKey: 'montant', width: 40 },
  { header: 'Type', dataKey: 'type', width: 50 },
  { header: 'Statut', dataKey: 'statut', width: 40 }
];

export function formatTransactionDateForPdfList(dateString: string | undefined): string {
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
  } catch {
    return dateString;
  }
}

export function transactionStatutLabelForPdf(statut: string): string {
  const labels: Record<string, string> = {
    VALIDE: 'Effectué',
    EN_ATTENTE: 'En attente',
    REJETE: 'Échec',
    ANNULE: 'Annulé'
  };
  return labels[statut] || statut;
}

export function transactionTypeLabelForPdf(type: string): string {
  const labels: Record<string, string> = {
    VIREMENT_SORTANT: 'Virement sortant',
    VIREMENT_ENTRANT: 'Virement entrant',
    VIREMENT_SIMPLE: 'Virement simple',
    RETRAIT: 'Retrait',
    DEPOT: 'Dépôt',
    FRAIS: 'Frais',
    FRAIS_LOCATION: 'Frais de location',
    FRAIS_FRONTIERE: 'Frais frontière',
    FRAIS_DOUANE: 'Frais douane',
    FRAIS_T1: 'Frais T1',
    TS_FRAIS_PRESTATIONS: 'Frais prestations',
    FRAIS_REPERTOIRE: 'Frais répertoire',
    FRAIS_CHAMBRE_COMMERCE: 'Frais chambre de commerce',
    INTERET: 'Intérêt',
    SALAIRE: 'Salaire'
  };
  return labels[type] || type;
}

/** Même format que l’export Paiements (espaces milliers, sans suffixe devise dans la cellule). */
export function formatMontantSpacedForPdf(value: number | undefined | null): string {
  const n = Math.round(value || 0);
  return n.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
}

export function buildTransactionsPdfTableRows(transactions: Transaction[]): Record<string, string>[] {
  return transactions.map((t) => ({
    numero: t.id?.toString() || 'N/A',
    reference: t.reference || '-',
    description: t.description || '-',
    date: formatTransactionDateForPdfList(t.date),
    montant: formatMontantSpacedForPdf(t.montant),
    type: transactionTypeLabelForPdf(t.type || ''),
    statut: transactionStatutLabelForPdf(t.statut || '')
  }));
}

export interface ExportTransactionsPdfReportArgs {
  pdfService: PdfService;
  transactions: Transaction[];
  subtitle: string;
  filename: string;
  dateRange?: PdfExportOptions['dateRange'];
}

/**
 * Export tableau PDF (template {@link PdfService.exportTable}) — aligné menu Paiements.
 */
export function exportTransactionsPdfReport(args: ExportTransactionsPdfReportArgs): void {
  args.pdfService.exportTable({
    title: 'Liste des Transactions',
    subtitle: args.subtitle,
    filename: args.filename,
    dateRange: args.dateRange,
    columns: TRANSACTIONS_PDF_LIST_COLUMNS,
    data: buildTransactionsPdfTableRows(args.transactions)
  });
}

/**
 * Filtre les transactions liées aux comptes bancaires et/ou caisses (menu Banque & Caisse).
 */
export function filterTransactionsBanqueCaisse(
  transactions: Transaction[],
  scope: 'banque' | 'caisse' | 'les-deux',
  compteId: number | null,
  caisseId: number | null
): Transaction[] {
  return transactions.filter((t) => {
    const hasCompte = t.compteId != null && t.compteId > 0;
    const hasCaisse = t.caisseId != null && t.caisseId > 0;

    if (scope === 'banque') {
      if (!hasCompte) return false;
      if (compteId != null && t.compteId !== compteId) return false;
      return true;
    }

    if (scope === 'caisse') {
      if (!hasCaisse) return false;
      if (caisseId != null && t.caisseId !== caisseId) return false;
      return true;
    }

    // les-deux : mouvements banque et/ou caisse, avec filtrage optionnel
    const matchBanque = hasCompte && (compteId == null || t.compteId === compteId);
    const matchCaisse = hasCaisse && (caisseId == null || t.caisseId === caisseId);

    if (compteId == null && caisseId == null) {
      return hasCompte || hasCaisse;
    }
    if (compteId != null && caisseId != null) {
      return matchBanque || matchCaisse;
    }
    if (compteId != null) {
      return matchBanque;
    }
    return matchCaisse;
  });
}
