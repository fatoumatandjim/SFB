package com.backend.gesy.comptebancaire.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompteBancaireDTO {
    private Long id;
    private String numero;
    private String type;
    private BigDecimal solde;
    private String banque;
    private String numeroCompteBancaire;
    private String statut;
    private String description;
}

