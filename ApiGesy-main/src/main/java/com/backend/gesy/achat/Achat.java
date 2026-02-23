package com.backend.gesy.achat;

import com.backend.gesy.client.Client;
import com.backend.gesy.depot.Depot;
import com.backend.gesy.facture.Facture;
import com.backend.gesy.produit.Produit;
import com.backend.gesy.transaction.Transaction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "achats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Achat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Achat de cession (sans transaction/paiement, stock en quantityCession) */
    @Column(nullable = false)
    private boolean cession = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id", nullable = false)
    private Depot depot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(nullable = false)
    private Double quantite;

    @Column(name = "prix_unitaire")
    private BigDecimal prixUnitaire;

    @Column(name = "montant_total")
    private BigDecimal montantTotal;

    @Column(name = "date_achat", nullable = false)
    private LocalDateTime dateAchat = LocalDateTime.now();

    private String description;
    private String notes;

    @Column(name = "unite")
    private String unite; // L, kg, etc.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id")
    private Facture facture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;
}

