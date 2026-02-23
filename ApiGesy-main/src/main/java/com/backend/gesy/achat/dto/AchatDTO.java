package com.backend.gesy.achat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchatDTO {
    private Long id;
    private Long depotId;
    private String depotNom;
    private Long produitId;
    private String produitNom;
    private String typeProduit;
    private Double quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal montantTotal;
    private LocalDateTime dateAchat;
    private String description;
    private String notes;
    private String unite;
    private Long factureId;
    private String factureNumero;
    private String statutFacture; // PAYEE, EMISE, etc. (pour compatibilité)
    private Long transactionId;
    private String transactionReference;
    private String statutPaiement; // VALIDE, EN_ATTENTE, REJETE, ANNULE
    private Boolean cession;
    private Long clientId;
    private String clientNom;
}

