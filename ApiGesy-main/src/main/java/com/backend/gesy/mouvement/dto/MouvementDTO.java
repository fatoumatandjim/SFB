package com.backend.gesy.mouvement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MouvementDTO {
    private Long id;
    private Long stockId;
    private String typeMouvement;
    private Double quantite;
    private String unite;
    private String description;
    private String dateMouvement;
}

