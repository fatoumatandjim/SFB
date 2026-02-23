package com.backend.gesy.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatsDTO {
    private PaiementsEffectues paiementsEffectues;
    private PaiementsEnAttente paiementsEnAttente;
    private PaiementsEchec paiementsEchec;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaiementsEffectues {
        private Long total;
        private BigDecimal montant;
        private String periode;
        private String evolution;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaiementsEnAttente {
        private Long total;
        private BigDecimal montant;
        private String pourcentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaiementsEchec {
        private Long total;
        private BigDecimal montant;
        private Boolean urgent;
    }
}

