package com.backend.gesy.categoriedepense.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategorieDepenseDTO {
    private Long id;
    private String nom;
    private String description;
    private String statut;
}

