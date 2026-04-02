/** Types autorisés pour un compte bancaire ; la caisse physique est l'entité Caisse (API /caisses). */
export const COMPTE_BANQUE_TYPES = ['BANQUE', 'MOBILE_MONEY'] as const;
export type CompteBanqueType = (typeof COMPTE_BANQUE_TYPES)[number];

export function isCompteBanqueType(type: string | undefined | null): boolean {
  return type === 'BANQUE' || type === 'MOBILE_MONEY';
}
