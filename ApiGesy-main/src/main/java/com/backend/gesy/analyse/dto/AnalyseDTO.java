package com.backend.gesy.analyse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyseDTO {
    private KPIs kpis;
    private List<DonneeHebdomadaireDTO> donneesHebdomadaires;
    private List<TendanceDTO> tendances;
    private List<PerformanceDTO> performances;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KPIs {
        private Croissance croissance;
        private Efficacite efficacite;
        private Satisfaction satisfaction;
        private Rentabilite rentabilite;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Croissance {
        private BigDecimal valeur;
        private String evolution;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Efficacite {
        private BigDecimal valeur;
        private String evolution;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Satisfaction {
        private BigDecimal valeur;
        private String evolution;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rentabilite {
        private BigDecimal valeur;
        private String evolution;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DonneeHebdomadaireDTO {
        private String semaine;
        private BigDecimal ventes;
        private Integer clients;
        private Integer camions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TendanceDTO {
        private String categorie;
        private BigDecimal evolution;
        private String tendance; // "hausse", "baisse", "stable"
        private String couleur;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceDTO {
        private String indicateur;
        private BigDecimal valeur;
        private BigDecimal cible;
        private BigDecimal pourcentage;
        private String couleur;
    }
}

