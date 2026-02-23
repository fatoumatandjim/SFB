package com.backend.gesy.achat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAchatCessionDTO {
    private Long clientId;
    private Long depotId;
    private Long produitId;
    private Double quantite;
    private String description;
    private String notes;
    private String unite;
}
