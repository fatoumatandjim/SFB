package com.backend.gesy.capitale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapitaleDTO {
    // Fonds disponibles
    private FondsDTO fonds;
    
    // Valeur des stocks
    private StocksDTO stocks;
    
    // Dépenses investissement
    private DepensesInvestissementDTO depensesInvestissement;
    
    // Total capital
    private String totalCapital;
    private BigDecimal totalCapitalValue;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FondsDTO {
        private String totalBanques; // Total des comptes bancaires
        private BigDecimal totalBanquesValue;
        private String totalCaisses; // Total des caisses
        private BigDecimal totalCaissesValue;
        private String totalGeneral; // Total banques + caisses
        private BigDecimal totalGeneralValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StocksDTO {
        private List<StockParProduitDTO> stocksDepot; // Stocks en dépôt par produit
        private String totalStocksDepot; // Total valeur stocks en dépôt
        private BigDecimal totalStocksDepotValue;
        private List<StockParProduitDTO> stocksCamion; // Stocks en camion (voyages non livrés) par produit
        private String totalStocksCamion; // Total valeur stocks en camion
        private BigDecimal totalStocksCamionValue;
        private String totalStocks; // Total stocks dépôt + camion
        private BigDecimal totalStocksValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockParProduitDTO {
        private Long produitId;
        private String produitNom;
        private String typeProduit;
        private Double quantite;
        private String valeur; // Valeur formatée
        private BigDecimal valeurValue; // Valeur numérique
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepensesInvestissementDTO {
        private String total; // Total formaté
        private BigDecimal totalValue; // Total numérique
    }
}
