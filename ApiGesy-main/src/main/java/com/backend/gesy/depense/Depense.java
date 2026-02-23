package com.backend.gesy.depense;

import com.backend.gesy.caisse.Caisse;
import com.backend.gesy.categoriedepense.CategorieDepense;
import com.backend.gesy.comptebancaire.CompteBancaire;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "depenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Depense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(nullable = false)
    private LocalDateTime dateDepense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id", nullable = false)
    private CategorieDepense categorie;

    /** Compte bancaire utilisé pour le paiement (exclusif avec caisse). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_bancaire_id")
    private CompteBancaire compteBancaire;

    /** Caisse utilisée pour le paiement (exclusif avec compteBancaire). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caisse_id")
    private Caisse caisse;

    private String description;

    private String reference; // Numéro de facture ou référence

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    private String creePar; // Identifiant de l'utilisateur qui a créé la dépense
}

