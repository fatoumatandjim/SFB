package com.backend.gesy.voyage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientQuantiteDTO {
    private Long clientId;
    private Double quantite;
    /** Optionnel : lors d'un DECHARGER, pour un client nouvellement attribué dans la même requête, manquant à appliquer tout de suite (livraison). */
    private Double manquant;
}
