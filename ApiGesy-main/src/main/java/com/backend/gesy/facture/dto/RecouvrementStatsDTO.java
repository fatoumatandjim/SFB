package com.backend.gesy.facture.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecouvrementStatsDTO {
    private TotalCreances totalCreances;
    private EnRetard enRetard;
    private Recouvre recouvre;
    private Impaye impaye;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TotalCreances {
        private BigDecimal montant;
        private Long nombre;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnRetard {
        private BigDecimal montant;
        private Long nombre;
        private Long joursMoyen;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Recouvre {
        private BigDecimal montant;
        private Long nombre;
        private String pourcentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Impaye {
        private BigDecimal montant;
        private Long nombre;
    }
}

