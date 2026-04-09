package com.backend.gesy.voyage;

import com.backend.gesy.facture.Facture;
import com.backend.gesy.paiement.Paiement;
import com.backend.gesy.transaction.Transaction;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Règle unique pour l'affichage dans le menu Paiements : exclure les voyages dont le camion
 * n'est pas encore chargé ({@link Voyage.StatutVoyage#EN_ATTENTE_CHARGEMENT}).
 * <p>
 * IMPORTANT – Hibernate (avec {@code @Converter(autoApply=true)} sur {@code StatutVoyage}) génère
 * des INNER JOINs implicites si le prédicat utilise {@code IS NOT NULL} avant un accès à un champ
 * navigué. La forme {@code (x IS NULL OR x.champ <> ...)} force un LEFT JOIN implicite et garantit
 * que les lignes dont la FK est null ne sont pas éliminées.
 */
public final class VoyagePaiementMenuRules {

    private static final Voyage.StatutVoyage STATUT_VOYAGE_MASQUE_MENU = Voyage.StatutVoyage.EN_ATTENTE_CHARGEMENT;

    private VoyagePaiementMenuRules() {
    }

    /**
     * Prédicat JPQL pour l'alias {@code t} (entité {@code Transaction}).
     * Forme {@code IS NULL OR statut <>} : Hibernate génère un LEFT JOIN.
     * La colonne {@code statut} est {@code NOT NULL}, donc pas besoin de {@code statut IS NULL}.
     */
    public static final String JPQL_TRANSACTION_VISIBLE =
        "((t.voyage IS NULL OR t.voyage.statut <> 'EN_ATTENTE_CHARGEMENT')"
            + " AND (t.facture IS NULL OR t.facture.voyage IS NULL OR t.facture.voyage.statut <> 'EN_ATTENTE_CHARGEMENT'))";

    /**
     * Prédicat JPQL pour l'alias {@code p} (entité {@link Paiement}).
     * Sous-requête via {@code INNER JOIN px.transactions} (plus fiable que {@code MEMBER OF}).
     */
    public static final String JPQL_PAIEMENT_VISIBLE =
        "((p.voyage IS NULL OR p.voyage.statut <> 'EN_ATTENTE_CHARGEMENT')"
            + " AND (p.facture IS NULL OR p.facture.voyage IS NULL OR p.facture.voyage.statut <> 'EN_ATTENTE_CHARGEMENT')"
            + " AND NOT EXISTS (SELECT t FROM Paiement px INNER JOIN px.transactions t WHERE px.id = p.id"
            + " AND t.voyage IS NOT NULL AND t.voyage.statut = 'EN_ATTENTE_CHARGEMENT'))";

    static {
        String fragmentAttendu = "'" + STATUT_VOYAGE_MASQUE_MENU.name() + "'";
        if (!JPQL_TRANSACTION_VISIBLE.contains(fragmentAttendu) || !JPQL_TRANSACTION_VISIBLE.contains("t.facture")) {
            throw new IllegalStateException(
                "JPQL_TRANSACTION_VISIBLE doit couvrir t.voyage et t.facture.voyage avec le statut " + fragmentAttendu);
        }
        if (!JPQL_PAIEMENT_VISIBLE.contains(fragmentAttendu) || !JPQL_PAIEMENT_VISIBLE.contains("p.facture")) {
            throw new IllegalStateException(
                "JPQL_PAIEMENT_VISIBLE doit couvrir p.voyage, p.facture.voyage et les transactions liées (" + fragmentAttendu + ")");
        }
    }

    public static boolean isVisibleForPaiementMenu(Voyage voyage) {
        if (voyage == null || voyage.getStatut() == null) {
            return true;
        }
        return voyage.getStatut() != STATUT_VOYAGE_MASQUE_MENU;
    }

    public static Set<Voyage> collectVoyagesLinkedToPaiement(Paiement p) {
        if (p == null) {
            return Set.of();
        }
        return Stream.concat(
                Stream.concat(
                    Optional.ofNullable(p.getVoyage()).stream(),
                    Optional.ofNullable(p.getFacture()).map(Facture::getVoyage).stream()),
                Optional.ofNullable(p.getTransactions())
                    .stream()
                    .flatMap(List::stream)
                    .map(Transaction::getVoyage))
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static boolean isPaiementRowVisibleInMenu(Paiement p) {
        return collectVoyagesLinkedToPaiement(p).stream().allMatch(VoyagePaiementMenuRules::isVisibleForPaiementMenu);
    }

    public static boolean isTransactionRowVisibleInPaiementMenu(Transaction t) {
        if (t == null) {
            return false;
        }
        if (!isVisibleForPaiementMenu(t.getVoyage())) {
            return false;
        }
        Facture facture = t.getFacture();
        return facture == null || isVisibleForPaiementMenu(facture.getVoyage());
    }
}
