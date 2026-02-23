package com.backend.gesy.depot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepotDTO {
    private Long id;
    private String nom;
    private String adresse;
    private Double capacite;
    private Double capaciteUtilisee;
    private String statut;
    private String ville;
    private String pays;
    private String responsable;
    private String telephone;
}

