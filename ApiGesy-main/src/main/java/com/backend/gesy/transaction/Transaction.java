package com.backend.gesy.transaction;

import com.backend.gesy.camion.Camion;
import com.backend.gesy.comptebancaire.CompteBancaire;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TypeTransaction type;

    @Column(nullable = false)
    private BigDecimal montant;

    @Column(nullable = false)
    private LocalDateTime date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_id")
    private CompteBancaire compte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camion_id")
    private Camion camion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id")
    private com.backend.gesy.facture.Facture facture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voyage_id")
    private com.backend.gesy.voyage.Voyage voyage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caisse_id")
    private com.backend.gesy.caisse.Caisse caisse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_liee_id")
    private Transaction transactionLiee;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatutTransaction statut;

    private String description;
    private String reference;
    private String beneficiaire;

    public enum TypeTransaction {
        DEPOT,
        RETRAIT,
        VIREMENT_ENTRANT,
        VIREMENT_SORTANT,
        VIREMENT_SIMPLE,
        FRAIS,
        INTERET,
        FRAIS_LOCATION,
        FRAIS_FRONTIERE,
        TS_FRAIS_PRESTATIONS,
        FRAIS_REPERTOIRE,
        FRAIS_CHAMBRE_COMMERCE,
        SALAIRE,
        FRAIS_DOUANE,
        FRAIS_T1,
    }

    public enum StatutTransaction {
        EN_ATTENTE,
        VALIDE,
        REJETE,
        ANNULE
    }
}
