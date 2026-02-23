package com.backend.gesy.fournisseur.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FournisseurDTO {
    private Long id;
    private String nom;
    private String email;
    private String telephone;
    private String adresse;
    private String codeFournisseur;
    private String ville;
    private String pays;
    private String contactPersonne;
    private String typeFournisseur;
}

