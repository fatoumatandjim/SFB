package com.backend.gesy.facture.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneFactureDTO {
    private Long id;
    private Long produitId;
    private String produitNom;
    private String produitType;
    private Double quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal total;
}
