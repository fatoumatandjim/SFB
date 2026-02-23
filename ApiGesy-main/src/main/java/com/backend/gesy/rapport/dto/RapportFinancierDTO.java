package com.backend.gesy.rapport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RapportFinancierDTO {
    private StatsFinancieres stats;
    private List<DonneeMensuelleDTO> donneesMensuelles;
    private List<CategorieDepenseDTO> categoriesDepenses;
    private FraisDouaniers fraisDouaniers;
    private Pertes pertes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsFinancieres {
        private ChiffreAffaires chiffreAffaires;
        private Depenses depenses;
        private Benefice benefice;
        private Marge marge;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChiffreAffaires {
        private BigDecimal total;
        private String evolution;
        private String periode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Depenses {
        private BigDecimal total;
        private String evolution;
        private String periode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Benefice {
        private BigDecimal total;
        private String evolution;
        private String periode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Marge {
        private BigDecimal pourcentage;
        private String evolution;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DonneeMensuelleDTO {
        private String mois;
        private BigDecimal chiffreAffaires;
        private BigDecimal depenses;
        private BigDecimal benefice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorieDepenseDTO {
        private String nom;
        private BigDecimal montant;
        private BigDecimal pourcentage;
        private String couleur;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FraisDouaniers {
        private BigDecimal total;
        private BigDecimal fraisDouane;
        private BigDecimal fraisT1;
        private String evolution;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pertes {
        private BigDecimal total;
        private BigDecimal quantiteTotale;
        private String evolution;
    }
}

