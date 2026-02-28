package com.backend.gesy.voyage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoutTransportDTO {
    private Long id;
    private String numeroVoyage;
    private String camionImmatriculation;
    private LocalDateTime dateDepart;
    private String destination;
    private Double quantite;
    /** Prix unitaire transport (FCFA/litre) — modifiable par le comptable */
    private BigDecimal prixUnitaire;
    // Coût brut = prixUnitaire * quantite (avant manquants)
    private BigDecimal coutVoyage;
    // Frais supplémentaires éventuels (non utilisés pour l'instant)
    private BigDecimal fraisTotaux;
    // Coût total réel après déduction des montants de manquants
    private BigDecimal coutTotal;
    private String statutPaiement; // PAYE ou NON_PAYE
    private LocalDateTime datePaiement;
}

