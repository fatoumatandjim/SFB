package com.backend.gesy.produit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProduitAvecStocksDTO {
    private Long id;
    private String nom;
    private String typeProduit;
    private String description;
    private Double quantiteTotale;
    private List<StockInfoDTO> stocks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockInfoDTO {
        private Long stockId;
        private String depotNom;
        private String depotVille;
        private Double quantite;
        private String unite;
        private Double seuilMinimum;
        private Double prixUnitaire;
        private Boolean citerne; // Indique si c'est un stock citerne
        private String nom; // Nom du stock
    }
}

