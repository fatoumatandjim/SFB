package com.backend.gesy.stock;

import com.backend.gesy.depot.Depot;
import com.backend.gesy.produit.Produit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    private boolean citerne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(nullable = false)
    private Double quantite;

    /** Quantité en cession (achats de cession, sans paiement) */
    @Column(name = "quantity_cession")
    private Double quantityCession = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id")
    private Depot depot;

    @Column(name = "seuil_minimum")
    private Double seuilMinimum;

    @Column(name = "prix_unitaire")
    private Double prixUnitaire;

    @Column(name = "date_derniere_mise_a_jour")
    private LocalDateTime dateDerniereMiseAJour = LocalDateTime.now();

    private String unite; // litres, kg, etc.
}
