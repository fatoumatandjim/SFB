package com.backend.gesy.voyage;

import com.backend.gesy.axe.Axe;
import com.backend.gesy.camion.Camion;
import com.backend.gesy.compte.Compte;
import com.backend.gesy.depot.Depot;
import com.backend.gesy.facture.Facture;
import com.backend.gesy.produit.Produit;
import com.backend.gesy.transitaire.Transitaire;
import com.backend.gesy.transaction.Transaction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "voyages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Voyage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_voyage", unique = true, nullable = false)
    private String numeroVoyage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camion_id", nullable = false)
    private Camion camion;

    @OneToMany(mappedBy = "voyage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ClientVoyage> clientVoyages = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transitaire_id")
    private Transitaire transitaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "axe_id")
    private Axe axe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id")
    private Depot depot;

    /** Compte (utilisateur) responsable du voyage (obligatoire à la création) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Compte responsable;

    @Column(name = "date_depart")
    private LocalDateTime dateDepart;

    @Column(name = "date_arrivee")
    private LocalDateTime dateArrivee;

    @Column
    private String destination;

    @Column(name = "lieu_depart")
    private String lieuDepart;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private StatutVoyage statut;

    @Column(nullable = false)
    private Double quantite; // en litres (45000 par défaut)
    private Double manquant;

    @Column(name = "prix_unitaire")
    private java.math.BigDecimal prixUnitaire; // prix par litre de transport

    @Column(name = "cout_voyage")
    private java.math.BigDecimal coutVoyage; // calculé = quantite * prixUnitaire

    @Column(name = "numero_bon_enlevement", unique = true)
    private String numeroBonEnlevement; // Numéro du bon d'enlèvement (unique)

    @Column(nullable = false)
    private Boolean declarer = false; // Indique si le voyage a été déclaré

    @Column(name = "passager")
    private String passager; // passer_declarer, passer_non_declarer ou null

    @Column(name = "chauffeur")
    private String chauffeur; // Nom du chauffeur

    @Column(name = "numero_chauffeur")
    private String numeroChauffeur;

    /** Vente de type cession : pas de cout du voyage, client issu des achats */
    @Column(name = "cession")
    private boolean cession = false;

    /** Indique si le voyage a été libéré (par le transitaire) */
    @Column(nullable = false)
    private Boolean liberer = false;

    private String notes;

    @OneToMany(mappedBy = "voyage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "voyage", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<EtatVoyage> etats = new ArrayList<>();

    @OneToMany(mappedBy = "voyage", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.LAZY)
    private List<Facture> factures = new ArrayList<>();


    public enum StatutVoyage {
        CHARGEMENT,
        CHARGE,
        DEPART,
        ARRIVER,
        DOUANE,
        RECEPTIONNER,
        LIVRE,
        PARTIELLEMENT_DECHARGER,
        DECHARGER
    }
}
