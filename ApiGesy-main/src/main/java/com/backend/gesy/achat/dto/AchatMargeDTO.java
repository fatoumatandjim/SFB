package com.backend.gesy.achat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchatMargeDTO {
    // Informations de l'achat
    private Long achatId;
    private Double quantiteAchetee;
    private BigDecimal prixUnitaireAchat;
    private BigDecimal montantTotalAchat;
    
    // Frais
    private BigDecimal fraisTotaux; // Frais totaux pour tous les achats du produit
    private BigDecimal fraisProportionnels; // Frais répartis proportionnellement pour cet achat
    private BigDecimal coutReelParLitre; // Prix d'achat + frais proportionnels par litre
    
    // Ventes
    private Double quantiteVendue; // Quantité vendue provenant de cet achat
    private BigDecimal prixVenteMoyen; // Prix de vente moyen par litre
    private BigDecimal montantVenteTotal; // Montant total des ventes
    
    // Marge
    private BigDecimal margeBrute; // Marge avant frais
    private BigDecimal margeNet; // Marge nette (après déduction des frais)
    private BigDecimal margePourcentage; // Pourcentage de marge
    
    // Stock
    private Double stockRestant; // Quantité restante en stock
    private BigDecimal valeurStockRestant; // Valeur du stock restant au coût réel
}

