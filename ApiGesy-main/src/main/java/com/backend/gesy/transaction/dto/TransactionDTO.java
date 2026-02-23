package com.backend.gesy.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private Long id;
    private String type;
    private BigDecimal montant;
    private LocalDateTime date;
    private Long compteId;
    private Long camionId;
    private Long factureId;
    private Long voyageId;
    private Long caisseId;
    private Long transactionLieeId;
    private String statut;
    private String description;
    private String reference;
    private String beneficiaire;
}

