package com.backend.gesy.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VirementRequestDTO {
    private BigDecimal montant;
    private LocalDateTime date;
    private Long compteSourceId; // Pour virement ou retrait
    private Long compteDestinationId; // Pour virement ou dépôt
    private Long caisseId; // Pour dépôt ou retrait (caisse)
    private String type; // "VIREMENT", "DEPOT", "RETRAIT"
    private String statut; // "EN_ATTENTE", "VALIDE", etc.
    private String description;
    private String reference;
}

