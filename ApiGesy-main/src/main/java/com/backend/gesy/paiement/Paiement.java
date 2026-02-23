package com.backend.gesy.paiement;

import com.backend.gesy.facture.Facture;
import com.backend.gesy.transaction.Transaction;
import com.backend.gesy.comptebancaire.CompteBancaire;
import com.backend.gesy.caisse.Caisse;
import com.backend.gesy.voyage.Voyage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "paiements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Paiement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id", nullable = true)
    private Facture facture;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MethodePaiement methode;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatutPaiement statut;

    @Column(name = "numero_cheque")
    private String numeroCheque;

    @Column(name = "numero_compte")
    private String numeroCompte;

    private String reference;
    private String notes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "paiement_transactions",
        joinColumns = @JoinColumn(name = "paiement_id"),
        inverseJoinColumns = @JoinColumn(name = "transaction_id")
    )
    private List<Transaction> transactions = new ArrayList<>();

    @ManyToOne
    private Voyage voyage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_id")
    private CompteBancaire compte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caisse_id")
    private Caisse caisse;

    public enum MethodePaiement {
        ESPECES,
        CHEQUE,
        VIREMENT,
        CARTE_BANCAIRE,
        MOBILE_MONEY
    }

    public enum StatutPaiement {
        EN_ATTENTE,
        VALIDE,
        REJETE,
        ANNULE
    }
}
