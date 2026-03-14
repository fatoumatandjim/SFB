/**
 * Utilitaires liés à la date des voyages (DRY).
 * Tri par défaut : du plus récent au plus ancien sur tout le projet.
 */

/** Objet avec date de départ optionnelle (voyage, DTO, etc.) */
export interface WithDateDepart {
  dateDepart?: string;
}

/**
 * Trie une liste par date/heure de départ : du plus récent au plus ancien.
 * Retourne une nouvelle liste (ne mute pas l'original).
 */
export function sortByDateDepartDesc<T extends WithDateDepart>(voyages: T[]): T[] {
  return [...voyages].sort((a, b) => {
    const da = a.dateDepart ? new Date(a.dateDepart).getTime() : 0;
    const db = b.dateDepart ? new Date(b.dateDepart).getTime() : 0;
    return db - da;
  });
}
