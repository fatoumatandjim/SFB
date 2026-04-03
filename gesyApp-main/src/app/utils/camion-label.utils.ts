import { Camion } from '../services/camions.service';

/** Champs nécessaires pour affichage / recherche d’un camion */
export type CamionLabelFields = Pick<Camion, 'immatriculation' | 'marque' | 'modele'>;

/**
 * Libellé unique pour l’UI (liste, combobox, exports) : « immatriculation - marque modèle ».
 */
export function formatCamionLabel(camion: CamionLabelFields): string {
  return `${camion.immatriculation} - ${camion.marque || ''} ${camion.modele || ''}`.replace(/\s+/g, ' ').trim();
}

/**
 * Indique si le camion correspond au texte de recherche (immatriculation, marque, modèle ou libellé complet).
 */
export function camionMatchesSearch(camion: CamionLabelFields, searchTerm: string): boolean {
  const t = searchTerm.trim().toLowerCase();
  if (!t) {
    return true;
  }
  const imm = (camion.immatriculation || '').toLowerCase();
  const marque = (camion.marque || '').toLowerCase();
  const modele = (camion.modele || '').toLowerCase();
  const line = formatCamionLabel(camion).toLowerCase().replace(/\s+/g, ' ').trim();
  return line.includes(t) || imm.includes(t) || marque.includes(t) || modele.includes(t);
}
