package com.backend.gesy.depense.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ligne unifiée pour le menu Dépenses : dépense manuelle ou paiement (coût transport, T1, douane).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedLigneDepenseDTO {
    public enum TypeLigne { DEPENSE, PAIEMENT }

    private TypeLigne type;
    private Long id;
    private String libelle;
    private BigDecimal montant;
    private LocalDate date;
    private Long categorieId;
    private String categorieNom;
    private String reference;
    /** Pour PAIEMENT : voyage associé si présent. */
    private Long voyageId;
    private String numeroVoyage;
}
