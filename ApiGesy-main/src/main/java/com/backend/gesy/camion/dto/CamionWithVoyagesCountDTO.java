package com.backend.gesy.camion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CamionWithVoyagesCountDTO {
    private Long id;
    private String immatriculation;
    private String modele;
    private String marque;
    private Integer annee;
    private String type;
    private Double capacite;
    private String statut;
    private Long nombreVoyages;
    /** Voyages hors cession (ceux pris en compte pour les coûts de transport) */
    private Long nombreVoyagesNonCession;
}

