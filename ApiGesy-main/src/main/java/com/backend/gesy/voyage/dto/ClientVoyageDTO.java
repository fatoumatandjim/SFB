package com.backend.gesy.voyage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientVoyageDTO {
    private Long id;
    private Long voyageId;
    private Long clientId;
    private String clientNom;
    private String clientEmail;
    private Double quantite;
    private BigDecimal prixAchat;
    private String statut; // "LIVRER" ou "NON_LIVRE"
    private Double manquant;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}
