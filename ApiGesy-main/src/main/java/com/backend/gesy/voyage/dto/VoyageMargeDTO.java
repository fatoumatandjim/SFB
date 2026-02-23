package com.backend.gesy.voyage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoyageMargeDTO {
    // Informations du voyage
    private Long voyageId;
    private String numeroVoyage;
    private Double quantite;
    
    // Coûts
    private BigDecimal prixUnitaireAchat; // Prix d'achat par litre
    private BigDecimal coutVoyage; // Coût total du voyage (quantité * prixUnitaire)
    private BigDecimal fraisTotaux; // Somme de toutes les transactions du voyage
    private BigDecimal coutReelTotal; // Coût total réel = coutVoyage + fraisTotaux
    private BigDecimal coutReelParLitre; // Coût réel par litre
    
    // Vente
    private BigDecimal prixVenteUnitaire; // Prix de vente par litre (depuis la facture)
    private BigDecimal montantVenteTotal; // Montant total de la vente
    
    // Marge
    private BigDecimal margeBrute; // Marge avant frais = (prixVente - prixAchat) * quantite
    private BigDecimal margeNet; // Marge nette = montantVente - coutReelTotal
    private BigDecimal margePourcentage; // Pourcentage de marge = (margeNet / montantVente) * 100
    
    // Statut
    private Boolean hasFacture; // Indique si le voyage a une facture
}

