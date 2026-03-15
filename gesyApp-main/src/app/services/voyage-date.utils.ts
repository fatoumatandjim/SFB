/**
 * Utilitaires liés à la date des voyages (DRY).
 * Tri par défaut : du plus récent au plus ancien (date de création) sur tout le projet.
 */

/** Objet avec date de départ optionnelle (voyage, DTO, etc.) */
export interface WithDateDepart {
  dateDepart?: string;
}

/** Objet avec date de création optionnelle (voyage, DTO, etc.) */
export interface WithDateCreation {
  dateCreation?: string;
  dateDepart?: string;
  id?: number;
}

/**
 * Trie une liste par date de création (récent d'abord), puis date de départ, puis id.
 * Retourne une nouvelle liste (ne mute pas l'original).
 */
export function sortByDateCreationDesc<T extends WithDateCreation>(voyages: T[]): T[] {
  return [...voyages].sort((a, b) => {
    const da = (a.dateCreation || a.dateDepart) ? new Date(a.dateCreation || a.dateDepart!).getTime() : 0;
    const db = (b.dateCreation || b.dateDepart) ? new Date(b.dateCreation || b.dateDepart!).getTime() : 0;
    if (db !== da) return db - da;
    return (b.id ?? 0) - (a.id ?? 0);
  });
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
