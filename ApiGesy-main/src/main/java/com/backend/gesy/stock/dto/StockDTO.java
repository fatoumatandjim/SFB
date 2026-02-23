package com.backend.gesy.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDTO {
    private Long id;
    private Long produitId;
    private String produitNom;
    private String typeProduit;
    private Double quantite;
    /** Quantité en cession (achats de cession, sans paiement) */
    private Double quantityCession;
    private Long depotId;
    private String depotNom;
    private Double seuilMinimum;
    private Double prixUnitaire;
    private String unite;
    private String dateDerniereMiseAJour;
}

