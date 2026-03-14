/**
 * Source unique des libellés et styles des statuts de voyage (DRY).
 * Tous les statuts du backend (Voyage.StatutVoyage) doivent être présents.
 */

/** Ordre d'affichage des statuts (aligné backend). */
export const STATUTS_VOYAGE_ORDER: readonly string[] = [
  'CHARGEMENT',
  'CHARGE',
  'DEPART',
  'ARRIVER',
  'DOUANE',
  'RECEPTIONNER',
  'LIVRE',
  'PARTIELLEMENT_DECHARGER',
  'DECHARGER'
];

export const STATUT_VOYAGE_LABELS: Readonly<Record<string, string>> = {
  CHARGEMENT: 'Chargement',
  CHARGE: 'Chargé',
  DEPART: 'Départ',
  ARRIVER: 'Arrivé',
  DOUANE: 'Douane',
  RECEPTIONNER: 'Sortie de douane',
  LIVRE: 'Attribué',
  PARTIELLEMENT_DECHARGER: 'Partiellement Déchargé',
  DECHARGER: 'Décharger'
} as const;

export const STATUT_VOYAGE_CLASSES: Readonly<Record<string, string>> = {
  CHARGEMENT: 'badge-blue',
  CHARGE: 'badge-orange',
  DEPART: 'badge-purple',
  ARRIVER: 'badge-green',
  DOUANE: 'badge-yellow',
  RECEPTIONNER: 'badge-teal',
  LIVRE: 'badge-teal',
  PARTIELLEMENT_DECHARGER: 'badge-gray',
  DECHARGER: 'badge-gray'
} as const;

const LABEL_SORTIE_DOUANE = 'Sortie de douane';
const CLASS_SORTIE_DOUANE = 'badge-teal';
const DEFAULT_CLASS = 'badge-gray';

export interface VoyageLiberer {
  liberer?: boolean;
}

/** True si le voyage est en "sortie de douane" (RECEPTIONNER ou DOUANE + libéré). */
export function isSortieDouane(
  statut: string | undefined,
  voyage?: VoyageLiberer
): boolean {
  return (
    statut === 'RECEPTIONNER' ||
    (statut === 'DOUANE' && !!voyage?.liberer)
  );
}

/**
 * Libellé affiché pour un statut de voyage.
 * DOUANE + liberer → "Sortie de douane" (cohérent avec RECEPTIONNER).
 */
export function getVoyageStatutLabel(
  statut: string | undefined,
  voyage?: VoyageLiberer
): string {
  if (!statut) return 'N/A';
  if (statut === 'DOUANE' && voyage?.liberer) return LABEL_SORTIE_DOUANE;
  return STATUT_VOYAGE_LABELS[statut] ?? statut;
}

/**
 * Classe CSS pour le badge du statut.
 * DOUANE + liberer → même style que RECEPTIONNER.
 */
export function getVoyageStatutClass(
  statut: string | undefined,
  voyage?: VoyageLiberer
): string {
  if (!statut) return DEFAULT_CLASS;
  if (statut === 'DOUANE' && voyage?.liberer) return CLASS_SORTIE_DOUANE;
  return STATUT_VOYAGE_CLASSES[statut] ?? DEFAULT_CLASS;
}
