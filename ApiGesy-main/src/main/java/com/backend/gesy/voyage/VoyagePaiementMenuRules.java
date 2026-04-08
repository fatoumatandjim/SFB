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
     * Prédicat JPQL pour l’alias {@code t} (entité {@code Transaction}).
     * <p><strong>Priorité :</strong> si {@code t.voyage} est renseigné, seul son statut compte pour le masquage
     * (camion chargé sur cette ligne → visible), afin d’éviter qu’un {@code t.facture.voyage} désynchronisé ou
     * obsolète masque encore la transaction. Si {@code t.voyage} est null, on applique la règle sur
     * {@code t.facture.voyage}. Statut voyage null ⇒ visible ({@link #isVisibleForPaiementMenu(Voyage)}).
     * <p>
     * Littéraux concaténés = constantes de compilation pour les {@code @Query}.
     */
    private static final String JPQL_T_IF_VOYAGE_SET =
        "(t.voyage IS NOT NULL AND (t.voyage.statut IS NULL OR t.voyage.statut <> 'EN_ATTENTE_CHARGEMENT'))";
    private static final String JPQL_T_IF_VOYAGE_NULL_USE_FACTURE =
        "(t.voyage IS NULL AND (t.facture IS NULL OR t.facture.voyage IS NULL OR t.facture.voyage.statut IS NULL OR t.facture.voyage.statut <> 'EN_ATTENTE_CHARGEMENT'))";

    public static final String JPQL_TRANSACTION_VISIBLE =
        "(" + JPQL_T_IF_VOYAGE_SET + " OR " + JPQL_T_IF_VOYAGE_NULL_USE_FACTURE + ")";

    private static final String JPQL_P_IF_VOYAGE_SET =
        "(p.voyage IS NOT NULL AND (p.voyage.statut IS NULL OR p.voyage.statut <> 'EN_ATTENTE_CHARGEMENT'))";
    private static final String JPQL_P_IF_VOYAGE_NULL_USE_FACTURE =
        "(p.voyage IS NULL AND (p.facture IS NULL OR p.facture.voyage IS NULL OR p.facture.voyage.statut IS NULL OR p.facture.voyage.statut <> 'EN_ATTENTE_CHARGEMENT'))";

    /**
     * Aucune transaction liée ne doit être « bloquée » par la même règle que {@link #JPQL_TRANSACTION_VISIBLE}
     * ({@code NOT (visible)} dans la sous-requête).
     */
    private static final String JPQL_P_NOTEX_LINKED_TX_BLOCKED =
        " AND NOT EXISTS (SELECT t FROM Paiement px INNER JOIN px.transactions t WHERE px.id = p.id AND NOT ("
            + "(" + JPQL_T_IF_VOYAGE_SET + ") OR (" + JPQL_T_IF_VOYAGE_NULL_USE_FACTURE + ")))";

    /**
     * Prédicat JPQL pour l’alias {@code p} (entité {@link Paiement}) : même priorité voyage / facture que pour
     * les transactions, plus l’absence de transaction liée « bloquée ».
     */
    public static final String JPQL_PAIEMENT_VISIBLE =
        "((" + JPQL_P_IF_VOYAGE_SET + " OR " + JPQL_P_IF_VOYAGE_NULL_USE_FACTURE + ")" + JPQL_P_NOTEX_LINKED_TX_BLOCKED + ")";

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
     * Même logique que {@link #JPQL_PAIEMENT_VISIBLE} pour une entité chargée (tests, debug).
     */
    public static boolean isPaiementRowVisibleInMenu(Paiement p) {
        if (p == null) {
            return false;
        }
        boolean direct;
        if (p.getVoyage() != null) {
            direct = isVisibleForPaiementMenu(p.getVoyage());
        } else {
            direct = p.getFacture() == null || isVisibleForPaiementMenu(p.getFacture().getVoyage());
        }
        if (!direct) {
            return false;
        }
        if (p.getTransactions() == null) {
            return true;
        }
        return p.getTransactions().stream().allMatch(VoyagePaiementMenuRules::isTransactionRowVisibleInPaiementMenu);
    }

    /** Même logique que {@link #JPQL_TRANSACTION_VISIBLE} pour une entité chargée (stats, tests). */
    public static boolean isTransactionRowVisibleInPaiementMenu(Transaction t) {
        if (t == null) {
            return false;
        }
        if (t.getVoyage() != null) {
            return isVisibleForPaiementMenu(t.getVoyage());
        }
        Facture facture = t.getFacture();
        if (facture == null) {
            return true;
        }
        return isVisibleForPaiementMenu(facture.getVoyage());
    }
}
