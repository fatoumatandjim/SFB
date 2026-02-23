package com.backend.gesy.paiement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaiementDTO {
    private Long id;
    private BigDecimal montant;
    private LocalDate date;
    private Long factureId;
    private String methode;
    private String statut;
    private String numeroCheque;
    private String numeroCompte;
    private String reference;
    private String notes;
}

