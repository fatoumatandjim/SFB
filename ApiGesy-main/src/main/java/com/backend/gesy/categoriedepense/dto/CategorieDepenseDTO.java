package com.backend.gesy.categoriedepense.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategorieDepenseDTO {
    private Long id;
    private String nom;
    private String description;
    private String statut;
    /** Prix unitaires transport (FCFA/litre) pour les catégories type "Coût de transport". */
    private List<Integer> tarifsTransport;
}

