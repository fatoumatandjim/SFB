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
     * Prédicat JPQL pour l’alias {@code t} (entité {@code Transaction}) : transaction sans voyage
     * ou voyage déjà au-delà de l’attente de chargement.
     * Littéral volontaire (exigence des {@code @Query} Spring qui concatènent une constante de compilation).
     */
    public static final String JPQL_TRANSACTION_VISIBLE =
        "(t.voyage IS NULL OR t.voyage.statut <> 'EN_ATTENTE_CHARGEMENT')";

    static {
        String fragmentAttendu = "'" + STATUT_VOYAGE_MASQUE_MENU.name() + "'";
        if (!JPQL_TRANSACTION_VISIBLE.contains(fragmentAttendu)) {
            throw new IllegalStateException(
                "JPQL_TRANSACTION_VISIBLE doit contenir le statut " + fragmentAttendu);
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
}
