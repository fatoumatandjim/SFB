package com.backend.gesy.transaction;

/**
 * Noms de paramètres d’API pour les transactions (alignés avec le client, ex. menu Paiements).
 */
public final class TransactionApiQueryParams {

    private TransactionApiQueryParams() {
    }

    /** Filtre : exclure les transactions liées à un voyage encore {@code EN_ATTENTE_CHARGEMENT}. */
    public static final String EXCLURE_VOYAGE_EN_ATTENTE_CHARGEMENT = "exclureVoyageEnAttenteChargement";
}
