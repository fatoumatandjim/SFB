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
 * Règle unique pour l’affichage dans le menu Paiements : exclure les voyages dont le camion
 * n’est pas encore chargé ({@link Voyage.StatutVoyage#EN_ATTENTE_CHARGEMENT}).
 * <p>
 * Le fragment JPQL doit rester aligné avec {@link #isVisibleForPaiementMenu(Voyage)}
 * (même libellé {@link EnumType#STRING} qu’en base).
 */
public final class VoyagePaiementMenuRules {

    /** Statut masqué dans le menu ; doit correspondre au littéral JPQL {@link #JPQL_TRANSACTION_VISIBLE}. */
    private static final Voyage.StatutVoyage STATUT_VOYAGE_MASQUE_MENU = Voyage.StatutVoyage.EN_ATTENTE_CHARGEMENT;

    private VoyagePaiementMenuRules() {
    }

    /**
     * Prédicat JPQL pour l’alias {@code t} (entité {@code Transaction}) : exclut toute ligne liée à un voyage
     * encore en attente de chargement, que le lien soit direct ({@code t.voyage}) ou via la facture ({@code t.facture.voyage}).
     * Sinon une transaction VALIDE sans {@code voyage_id} mais avec une facture liée au voyage passait encore le filtre.
     * Aligné sur {@link #isVisibleForPaiementMenu(Voyage)} : voyage ou statut null ⇒ visible (évite UNKNOWN en SQL sur statut null).
     * <p>
     * Littéral volontaire (exigence des {@code @Query} Spring qui concatènent une constante de compilation).
     */
    /** Fragments littéraux uniquement : {@code @Query} exige des constantes de compilation (pas d’appels de méthodes). */
    private static final String JPQL_T_VOYAGE_OK =
        "(t.voyage IS NULL OR t.voyage.statut IS NULL OR t.voyage.statut <> 'EN_ATTENTE_CHARGEMENT')";
    private static final String JPQL_T_FACTURE_VOYAGE_OK =
        "(t.facture IS NULL OR t.facture.voyage IS NULL OR t.facture.voyage.statut IS NULL OR t.facture.voyage.statut <> 'EN_ATTENTE_CHARGEMENT')";

    public static final String JPQL_TRANSACTION_VISIBLE =
        "(" + JPQL_T_VOYAGE_OK + " AND " + JPQL_T_FACTURE_VOYAGE_OK + ")";

    private static final String JPQL_P_VOYAGE_OK =
        "(p.voyage IS NULL OR p.voyage.statut IS NULL OR p.voyage.statut <> 'EN_ATTENTE_CHARGEMENT')";
    private static final String JPQL_P_FACTURE_VOYAGE_OK =
        "(p.facture IS NULL OR p.facture.voyage IS NULL OR p.facture.voyage.statut IS NULL OR p.facture.voyage.statut <> 'EN_ATTENTE_CHARGEMENT')";
    /** Sous-requête en JOIN (plus fiable que {@code MEMBER OF} selon versions Hibernate). */
    private static final String JPQL_P_NOTEX_TX_EN_ATTENTE =
        " AND NOT EXISTS (SELECT t FROM Paiement px INNER JOIN px.transactions t WHERE px.id = p.id "
            + "AND t.voyage IS NOT NULL AND t.voyage.statut = 'EN_ATTENTE_CHARGEMENT')";

    /**
     * Prédicat JPQL pour l’alias {@code p} (entité {@link Paiement}) : aligné sur
     * {@link #collectVoyagesLinkedToPaiement(Paiement)} — voyage direct, facture, et transactions liées.
     */
    public static final String JPQL_PAIEMENT_VISIBLE =
        "((" + JPQL_P_VOYAGE_OK + " AND " + JPQL_P_FACTURE_VOYAGE_OK + ")" + JPQL_P_NOTEX_TX_EN_ATTENTE + ")";

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

    /**
     * Voyages pouvant conditionner l’affichage d’une ligne paiement : entité directe, facture, ou transactions liées.
     */
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

    /**
     * Une ligne « Paiement » est affichée dans le menu seulement si aucun voyage lié n’est encore en attente de chargement.
     */
    public static boolean isPaiementRowVisibleInMenu(Paiement p) {
        return collectVoyagesLinkedToPaiement(p).stream().allMatch(VoyagePaiementMenuRules::isVisibleForPaiementMenu);
    }

    /** Même logique que {@link #JPQL_TRANSACTION_VISIBLE} pour une entité chargée (stats, tests). */
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
