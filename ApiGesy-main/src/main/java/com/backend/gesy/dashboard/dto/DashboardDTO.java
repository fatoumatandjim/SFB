package com.backend.gesy.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    private CamionsActifsDTO camionsActifs;
    private ChiffreAffairesDTO chiffreAffaires;
    private FacturesAttenteDTO facturesAttente;
    private UnitesStockDTO unitesStock;
    private FinancesDTO finances;
    private VoyagesStatsDTO voyagesStats;
    private DouaneStatsDTO douaneStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CamionsActifsDTO {
        private Integer value; // Total camions actifs
        private String change; // Pourcentage de changement
        private Integer enRoute; // Camions en route
        private Integer disponibles; // Camions disponibles
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChiffreAffairesDTO {
        private String value; // Montant formaté
        private String currency; // Devise (F)
        private String change; // Pourcentage de changement
        private String period; // Période (Cette semaine, etc.)
        private String increase; // Augmentation formatée
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacturesAttenteDTO {
        private Integer value; // Nombre de factures en attente
        private Integer badge; // Nombre de factures en retard
        private String montant; // Montant total formaté
        private Integer enRetard; // Nombre de factures en retard
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnitesStockDTO {
        private String value; // Total unités en stock formaté (somme de tous les produits)
        private String stockRestant; // Stock restant formaté (somme de tous les produits)
        private Boolean alert; // Alerte si niveau critique
        private Integer niveauCritique; // Nombre de produits en niveau critique
        private Integer depots; // Nombre de dépôts
        private List<StockParProduitDTO> stocksParProduit; // Liste des stocks par produit
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockParProduitDTO {
        private Long produitId;
        private String produitNom;
        private String typeProduit;
        private String quantiteTotale; // Quantité totale formatée
        private Double quantiteTotaleValue; // Quantité totale en nombre
        private Boolean alert; // Alerte si niveau critique pour ce produit
        private Integer nombreDepots; // Nombre de dépôts contenant ce produit
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinancesDTO {
        private SoldeBanqueDTO soldeBanque;
        private SoldeCaisseDTO soldeCaisse;
        private CreancesClientsDTO creancesClients;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoldeBanqueDTO {
        private String value; // Montant formaté
        private String currency; // Devise
        private Integer comptes; // Nombre de comptes bancaires
        private String change; // Changement formaté
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoldeCaisseDTO {
        private String value; // Montant formaté
        private String currency; // Devise
        private String date; // Date d'arrêté
        private String entrees; // Entrées formatées
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreancesClientsDTO {
        private String value; // Montant formaté
        private String currency; // Devise
        private Integer clients; // Nombre de clients concernés
        private String retard; // Montant en retard formaté
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoyagesStatsDTO {
        private Integer totalVoyagesEnCours; // Voyages en cours (EN_CHARGEMENT, DEPART)
        private Integer voyagesArrives; // Voyages arrivés (EN_DEPOT_SORTIE_DEPOT)
        private Integer voyagesALaDouane; // Voyages à la douane (A_LA_DOUANE, SORTIE_A_LA_DOUANE, DECLARE)
        private Integer voyagesLivre; // Voyages livrés (LIVRE, DEPOTE_EN_STATION)
        private List<VoyageDetailDTO> voyagesRecents; // Liste des voyages récents
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoyageDetailDTO {
        private Long id;
        private String numeroVoyage;
        private String camionImmatriculation;
        private String clientNom;
        private String destination;
        private String statut;
        private String dateDepart;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DouaneStatsDTO {
        private Integer nombreCamionsDeclares;
        private Integer nombreCamionsNonDeclares;
        private String montantFraisDouane; // Frais de douane ce mois (formaté)
        private String montantT1; // Frais T1 ce mois (formaté)
        private String montantFraisPayes; // Total (formaté), pour compatibilité
        private String currency;
    }
}

