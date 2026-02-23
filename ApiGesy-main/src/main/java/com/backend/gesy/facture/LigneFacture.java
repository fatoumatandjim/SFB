package com.backend.gesy.facture;

import com.backend.gesy.produit.Produit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "lignes_facture")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneFacture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id", nullable = false)
    private Facture facture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(nullable = false)
    private Double quantite;

    @Column(name = "prix_unitaire", nullable = false)
    private BigDecimal prixUnitaire;

    @Column(nullable = false)
    private BigDecimal total;
}

