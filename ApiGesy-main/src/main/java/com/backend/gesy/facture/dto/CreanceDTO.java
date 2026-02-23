package com.backend.gesy.facture.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreanceDTO {
    private Long id;
    private String facture;
    private Long clientId;
    private String clientNom;
    private String clientEmail;
    private String clientTelephone;
    private BigDecimal montant;
    private BigDecimal montantPaye;
    private BigDecimal resteAPayer;
    private LocalDate dateEmission;
    private LocalDate dateEcheance;
    private Long joursRetard;
    private String statut;
    private String priorite;
    private Integer relances;
    private LocalDate dernierContact;
}

