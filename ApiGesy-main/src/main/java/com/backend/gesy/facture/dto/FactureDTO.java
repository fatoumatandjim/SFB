package com.backend.gesy.facture.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FactureDTO {
    private Long id;
    private String numero;
    private LocalDate date;
    private BigDecimal montant;
    private BigDecimal montantHT;
    private BigDecimal montantTTC;
    private BigDecimal tauxTVA;
    private Long clientId;
    private String clientNom;
    private String clientEmail;
    private String statut;
    private LocalDate dateEcheance;
    private BigDecimal montantPaye;
    private String description;
    private String notes;
    private List<LigneFactureDTO> lignes;
}

