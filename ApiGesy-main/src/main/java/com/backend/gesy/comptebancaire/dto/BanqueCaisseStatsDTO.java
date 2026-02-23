package com.backend.gesy.comptebancaire.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BanqueCaisseStatsDTO {
    private SoldeTotal soldeTotal;
    private SoldeCaisse soldeCaisse;
    private ComptesBancaires comptesBancaires;
    private TotalEntrees totalEntrees;
    private TotalSorties totalSorties;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoldeTotal {
        private BigDecimal montant;
        private String evolution;
        private String periode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoldeCaisse {
        private BigDecimal montant;
        private BigDecimal entrees;
        private BigDecimal sorties;
        private String date;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComptesBancaires {
        private Long total;
        private Long actifs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TotalEntrees {
        private BigDecimal montant;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TotalSorties {
        private BigDecimal montant;
    }
}

