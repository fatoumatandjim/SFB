package com.backend.gesy.pays.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoriquePaysDTO {
    private Long id;
    private Long paysId;
    private String paysNom;
    private BigDecimal ancienFraisParLitre;
    private BigDecimal nouveauFraisParLitre;
    private BigDecimal ancienFraisParLitreGasoil;
    private BigDecimal nouveauFraisParLitreGasoil;
    private BigDecimal ancienFraisT1;
    private BigDecimal nouveauFraisT1;
    private String dateModification;
    private String modifiePar;
    private String commentaire;
}
