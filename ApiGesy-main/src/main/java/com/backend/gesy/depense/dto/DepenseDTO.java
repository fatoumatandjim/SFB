package com.backend.gesy.depense.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepenseDTO {
    private Long id;
    private String libelle;
    private BigDecimal montant;
    private LocalDateTime dateDepense;
    private Long categorieId;
    private String categorieNom;
    private String description;
    private String reference;
    private LocalDateTime dateCreation;
    private String creePar;
    /** Compte bancaire pour déduire le montant (exclusif avec caisseId). */
    private Long compteId;
    /** Caisse pour déduire le montant (exclusif avec compteId). */
    private Long caisseId;
}

