package com.backend.gesy.achat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAchatWithFactureDTO {
    private Long depotId;
    private Long produitId;
    private Long fournisseurId;
    private Double quantite;
    private BigDecimal prixUnitaire;
    private String description;
    private String notes;
    private String unite;
}

