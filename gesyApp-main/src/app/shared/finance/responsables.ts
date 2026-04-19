/**
 * Données communes API banque/caisse pour l’affichage des responsables.
 */
export interface ResponsableRef {
  id: number;
  nom: string;
}

export const FINANCE_RESPONSABLES_COPY = {
  sectionLabel: 'Responsables',
  editTitle: 'Modifier les responsables',
  noneDesignated: 'Aucun responsable désigné — accès élargi',
  adminOnlyEdit: 'Seuls les administrateurs peuvent modifier les responsables.',
  loadBanqueError: 'Impossible de charger le compte bancaire.',
  loadCaisseError: 'Impossible de charger la caisse.'
} as const;

/** Textes partagés pour les champs de sélection (comptables, etc.). */
export const FINANCE_SELECT_COPY = {
  comptablesMultiselectEmpty:
    'Aucun comptable disponible. Créez ou activez des utilisateurs avec le rôle Comptable.'
} as const;

export function formatResponsablesLine(responsables?: ResponsableRef[] | null): string {
  if (!responsables?.length) {
    return FINANCE_RESPONSABLES_COPY.noneDesignated;
  }
  return responsables
    .map((r) => r.nom)
    .filter(Boolean)
    .join(', ');
}
