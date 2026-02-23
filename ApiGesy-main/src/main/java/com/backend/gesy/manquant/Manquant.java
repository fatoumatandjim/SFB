package com.backend.gesy.manquant;

import com.backend.gesy.voyage.Voyage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "manquants")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Manquant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voyage_id", nullable = false)
    private Voyage voyage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private com.backend.gesy.client.Client client;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal quantite;

    /**
     * Montant financier du manquant (quantite * prixAchat du ClientVoyage)
     */
    @Column(name = "montant_manquant", precision = 19, scale = 2)
    private BigDecimal montantManquant;

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    private LocalDateTime dateModification;

    private String description;

    private String creePar; // Identifiant de l'utilisateur qui a créé le manquant
}

