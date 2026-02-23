package com.backend.gesy.voyage;

import com.backend.gesy.client.Client;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "client_voyages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientVoyage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voyage_id", nullable = false)
    private Voyage voyage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private Double quantite; // Quantité livrée à ce client

    @Column(name = "prix_achat")
    private BigDecimal prixAchat; // Prix d'achat pour ce client

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private StatutLivraison statut; // Livrer ou NonLivre

    private Double manquant; // Manquant pour ce client

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    public enum StatutLivraison {
        LIVRER,
        NON_LIVRE
    }
}
