/**
 * Utilitaires d'affichage partagés pour les voyages (DRY).
 * Chauffeur, dates, client (initiales, couleur).
 */

/** Entité avec infos chauffeur (Voyage, VoyageDisplay, etc.) */
export interface WithChauffeur {
  chauffeur?: string;
  numeroChauffeur?: string;
}

/**
 * Affiche le chauffeur : nom + numéro si disponible.
 */
export function getChauffeurDisplay(entity: WithChauffeur | null | undefined): string {
  if (!entity) return '–';
  const nom = entity.chauffeur?.trim();
  const num = entity.numeroChauffeur?.trim();
  if (nom && num) return `${nom} (${num})`;
  if (nom) return nom;
  if (num) return num;
  return '-';
}

/**
 * Format date/heure fr-FR pour affichage (liste voyages, détails).
 */
export function formatDateFr(dateString: string | undefined): string {
  if (!dateString) return 'N/A';
  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return dateString;
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

/**
 * Initiales du client (2 caractères) pour avatar.
 */
export function getClientInitiales(clientNom: string | undefined): string {
  if (!clientNom) return '??';
  return clientNom
    .split(' ')
    .map((n) => n[0])
    .join('')
    .toUpperCase()
    .substring(0, 2);
}

/** Couleurs d'avatar client (cohérentes par nom). */
const CLIENT_AVATAR_COLORS = ['blue', 'purple', 'red', 'green', 'orange', 'teal', 'pink'];

/**
 * Classe couleur d'avatar pour un nom de client (déterministe).
 */
export function getClientColor(clientNom: string | undefined): string {
  if (!clientNom) return 'gray';
  const hash = clientNom.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
  return CLIENT_AVATAR_COLORS[hash % CLIENT_AVATAR_COLORS.length];
}
