/**
 * Source unique des libellés et styles des statuts de voyage (DRY).
 * Tous les statuts du backend (Voyage.StatutVoyage) doivent être présents.
 */

/** Ordre d'affichage des statuts (aligné backend). */
export const STATUTS_VOYAGE_ORDER: readonly string[] = [
  'EN_ATTENTE_CHARGEMENT',
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
  EN_ATTENTE_CHARGEMENT: 'En route (chargement)',
  CHARGEMENT: 'Chargement',
  CHARGE: 'Chargé',
  DEPART: 'Départ',
  ARRIVER: 'Arriver à la frontière',
  DOUANE: 'Douane',
  RECEPTIONNER: 'Sortie de douane',
  LIVRE: 'Attribué',
  PARTIELLEMENT_DECHARGER: 'Partiellement Déchargé',
  DECHARGER: 'Décharger'
} as const;

export const STATUT_VOYAGE_CLASSES: Readonly<Record<string, string>> = {
  EN_ATTENTE_CHARGEMENT: 'badge-gray',
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

/** Étend le voyage pour la logique d’affichage (déclaré + état Décharger validé = afficher "Décharger"). */
export interface VoyagePourStatut extends VoyageLiberer {
  declarer?: boolean;
  etats?: Array<{ etat?: string; valider?: boolean }>;
}

/** Statuts considérés "en chargement" (bon d'enlèvement, Excel, etc.). */
export const STATUTS_EN_CHARGEMENT: readonly string[] = ['EN_ATTENTE_CHARGEMENT', 'CHARGEMENT'];

/** Statuts considérés "en cours" pour les indicateurs (dashboard, camion). */
export const STATUTS_EN_COURS: readonly string[] = [
  'EN_ATTENTE_CHARGEMENT',
  'CHARGEMENT',
  'CHARGE',
  'DEPART',
  'ARRIVER',
  'DOUANE'
];

/** True si le statut correspond à un voyage en (attente de) chargement. */
export function isVoyageEnChargement(statut: string | undefined): boolean {
  return statut != null && STATUTS_EN_CHARGEMENT.includes(statut);
}

/** True si le statut correspond à un voyage "en cours" (avant réception/décharge). */
export function isVoyageEnCours(statut: string | undefined): boolean {
  return statut != null && STATUTS_EN_COURS.includes(statut);
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

/** True si le voyage est déclaré et a l’état "Décharger" validé → à afficher comme "Décharger" et à exclure de la liste En cours (Archives uniquement). */
export function isDechargerValide(voyage?: VoyagePourStatut): boolean {
  if (!voyage?.declarer || !voyage.etats?.length) return false;
  return voyage.etats.some(
    (e) => (e.etat === 'Décharger' || e.etat === 'Decharger') && e.valider === true
  );
}

/** True si le voyage doit apparaître dans la liste Archives (statut DECHARGER ou déclaré + état Décharger validé). */
export function isVoyageInArchives(voyage?: VoyagePourStatut & { statut?: string }): boolean {
  if (!voyage) return false;
  return voyage.statut === 'DECHARGER' || isDechargerValide(voyage);
}

/** True si le voyage doit apparaître dans la liste En cours (ni DECHARGER ni déclaré + Décharger validé). */
export function isVoyageInEnCours(voyage?: VoyagePourStatut & { statut?: string }): boolean {
  return !isVoyageInArchives(voyage);
}

/**
 * Libellé affiché pour un statut de voyage.
 * DOUANE + liberer → "Sortie de douane" (cohérent avec RECEPTIONNER).
 * Si le voyage est déclaré et l’état "Décharger" est validé, on affiche "Décharger" même si le statut API est encore RECEPTIONNER/DOUANE.
 */
export function getVoyageStatutLabel(
  statut: string | undefined,
  voyage?: VoyageLiberer | VoyagePourStatut
): string {
  if (!statut) return 'N/A';
  const v = voyage as VoyagePourStatut | undefined;
  if (isDechargerValide(v)) return STATUT_VOYAGE_LABELS['DECHARGER'];
  if (statut === 'DOUANE' && voyage?.liberer) return LABEL_SORTIE_DOUANE;
  return STATUT_VOYAGE_LABELS[statut] ?? statut;
}

/**
 * Classe CSS pour le badge du statut.
 * DOUANE + liberer → même style que RECEPTIONNER.
 * Si déclaré + état "Décharger" validé, style "Décharger".
 */
export function getVoyageStatutClass(
  statut: string | undefined,
  voyage?: VoyageLiberer | VoyagePourStatut
): string {
  if (!statut) return DEFAULT_CLASS;
  const v = voyage as VoyagePourStatut | undefined;
  if (isDechargerValide(v)) return STATUT_VOYAGE_CLASSES['DECHARGER'];
  if (statut === 'DOUANE' && voyage?.liberer) return CLASS_SORTIE_DOUANE;
  return STATUT_VOYAGE_CLASSES[statut] ?? DEFAULT_CLASS;
}
