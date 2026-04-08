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
     * <p>
     * Littéral volontaire (exigence des {@code @Query} Spring qui concatènent une constante de compilation).
     */
    public static final String JPQL_TRANSACTION_VISIBLE =
        "((t.voyage IS NULL OR t.voyage.statut <> 'EN_ATTENTE_CHARGEMENT') "
            + "AND (t.facture IS NULL OR t.facture.voyage IS NULL OR t.facture.voyage.statut <> 'EN_ATTENTE_CHARGEMENT'))";

    /**
     * Prédicat JPQL pour l’alias {@code p} (entité {@link Paiement}) : aligné sur
     * {@link #collectVoyagesLinkedToPaiement(Paiement)} — voyage direct, facture, et transactions liées.
     */
    public static final String JPQL_PAIEMENT_VISIBLE =
        "((p.voyage IS NULL OR p.voyage.statut <> 'EN_ATTENTE_CHARGEMENT') "
            + "AND (p.facture IS NULL OR p.facture.voyage IS NULL OR p.facture.voyage.statut <> 'EN_ATTENTE_CHARGEMENT') "
            + "AND NOT EXISTS (SELECT t FROM Transaction t WHERE t MEMBER OF p.transactions "
            + "AND t.voyage IS NOT NULL AND t.voyage.statut = 'EN_ATTENTE_CHARGEMENT'))";

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
