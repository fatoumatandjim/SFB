package com.backend.gesy.voyage.dto;

import com.backend.gesy.transaction.dto.TransactionDTO;
import com.backend.gesy.facture.dto.FactureDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoyageDTO {
    private Long id;
    private String numeroVoyage;
    private Long camionId;
    private String camionImmatriculation;
    private Long clientId;
    private String clientNom;
    private String clientEmail;
    private Long transitaireId;
    private String transitaireNom;
    private String transitaireIdentifiant;
    private String transitairePhone;
    private Long axeId;
    private String axeNom;
    private LocalDateTime dateDepart;
    private LocalDateTime dateArrivee;
    private String destination;
    private String lieuDepart;
    private String statut;
    private Double quantite;
    private Double manquant;
    private java.math.BigDecimal prixUnitaire;
    private Long produitId;
    private String produitNom;
    private String typeProduit;
    private Long depotId;
    private String depotNom;
    /** ID du compte (utilisateur) responsable du voyage */
    private Long responsableId;
    private String responsableIdentifiant;
    private java.math.BigDecimal coutVoyage;
    private Long compteId;
    private Long caisseId;
    private String notes;
    private List<TransactionDTO> transactions;
    private List<EtatVoyageDTO> etats;
    private Long factureId;
    private String factureNumero;
    private java.math.BigDecimal factureMontant;
    private java.math.BigDecimal factureMontantPaye;
    private String factureStatut;
    private String numeroBonEnlevement;
    private Boolean declarer;
    private String passager; // passer_declarer, passer_non_declarer ou null
    private String chauffeur; // Nom du chauffeur
    private String numeroChauffeur;
    private Boolean cession; // Vente de type cession (pas de cout du voyage)
    private Boolean liberer; // Voyage libéré par le transitaire
    private List<ClientVoyageDTO> clientVoyages; // Liste des clients associés au voyage
    private List<FactureDTO> factures; // Liste des factures associées au voyage
}
