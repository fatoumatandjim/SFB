package com.backend.gesy.produit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProduitDTO {
    private Long id;
    private String nom;
    private String typeProduit;
    private String description;
    private String dateCreation;
}

