package com.backend.gesy.camion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CamionDTO {
    private Long id;
    private String immatriculation;
    private String modele;
    private String marque;
    private Integer annee;
    private String type;
    private Double capacite;
    private Double kilometrage;
    private LocalDate dernierControle;
    private String statut;
    private Boolean loue;
    private BigDecimal montantLocation;
    private BigDecimal montantLocationInitial;
    private Long fournisseurId;
    private String fournisseurNom;
    private String fournisseurEmail;
    private Long responsableId;
    private String responsableIdentifiant;
}

