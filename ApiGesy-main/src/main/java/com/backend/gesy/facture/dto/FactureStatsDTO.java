package com.backend.gesy.facture.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FactureStatsDTO {
    private FacturesEmises facturesEmises;
    private FacturesPayees facturesPayees;
    private FacturesImpayees facturesImpayees;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacturesEmises {
        private Long total;
        private BigDecimal montant;
        private String periode;
        private String evolution;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacturesPayees {
        private Long total;
        private BigDecimal montant;
        private String pourcentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacturesImpayees {
        private Long total;
        private BigDecimal montant;
        private Long enRetard;
        private Boolean urgent;
    }
}

