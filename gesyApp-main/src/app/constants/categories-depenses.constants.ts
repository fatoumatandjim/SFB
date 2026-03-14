/**
 * Constantes pour les catégories de dépenses (DRY — alignées avec le backend).
 * Utilisées pour les couleurs, libellés et tarifs par défaut.
 */
export const NOM_CATEGORIE_COUT_TRANSPORT = 'Coût de transport';
export const NOM_CATEGORIE_FRAIS_T1 = 'Frais T1';
export const NOM_CATEGORIE_DROIT_DOUANE = 'Droit de douane';

/** Tarifs transport par défaut (FCFA/L) — fallback si l’API ne renvoie rien. */
export const TARIFS_TRANSPORT_DEFAULT: number[] = [47, 50];
