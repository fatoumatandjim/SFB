package com.backend.gesy.voyage;

/**
 * Règle unique pour l’affichage dans le menu Paiements : exclure les voyages dont le camion
 * n’est pas encore chargé ({@link Voyage.StatutVoyage#EN_ATTENTE_CHARGEMENT}).
 * <p>
 * Le fragment JPQL doit rester aligné avec {@link #isVisibleForPaiementMenu(Voyage)}.
 */
public final class VoyagePaiementMenuRules {

    private VoyagePaiementMenuRules() {
    }

    /**
     * Prédicat JPQL pour l’alias {@code t} (entité {@code Transaction}) : transaction sans voyage
     * ou voyage déjà au-delà de l’attente de chargement.
     */
    public static final String JPQL_TRANSACTION_VISIBLE =
        "(t.voyage IS NULL OR t.voyage.statut <> 'EN_ATTENTE_CHARGEMENT')";

    public static boolean isVisibleForPaiementMenu(Voyage voyage) {
        if (voyage == null || voyage.getStatut() == null) {
            return true;
        }
        return voyage.getStatut() != Voyage.StatutVoyage.EN_ATTENTE_CHARGEMENT;
    }
}
