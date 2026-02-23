package com.backend.gesy.manquant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManquantDTO {
    private Long id;
    private Long voyageId;
    private String numeroVoyage;
    private BigDecimal quantite;
    private LocalDateTime dateCreation;
    private String description;
    private String creePar;
    private String camionImmatriculation;
    private String produitNom;
    private String depotNom;
}

