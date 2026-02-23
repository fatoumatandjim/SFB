package com.backend.gesy.facture;

import com.backend.gesy.client.Client;
import com.backend.gesy.transaction.Transaction;
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
@Table(name = "factures")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Facture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numero;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private BigDecimal montant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatutFacture statut;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "montant_paye")
    private BigDecimal montantPaye = BigDecimal.ZERO;

    @Column(name = "taux_tva")
    private BigDecimal tauxTVA;

    @Column(name = "montant_ht")
    private BigDecimal montantHT;

    @Column(name = "montant_ttc")
    private BigDecimal montantTTC;

    private String description;
    private String notes;

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneFacture> lignes = new ArrayList<>();

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voyage_id")
    private Voyage voyage;

    public enum StatutFacture {
        BROUILLON,
        EMISE,
        PAYEE,
        PARTIELLEMENT_PAYEE,
        ANNULEE,
        EN_RETARD
    }

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (montantPaye == null) {
            montantPaye = BigDecimal.ZERO;
        }
    }
}
