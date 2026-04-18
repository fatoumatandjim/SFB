package com.backend.gesy.caisse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaisseDTO {
    private Long id;
    private String nom;
    private BigDecimal solde;
    private String statut;
    private String description;
    /** Identifiants des comptes utilisateurs responsables (gestion de cette caisse). */
    private List<Long> responsableIds;
}

