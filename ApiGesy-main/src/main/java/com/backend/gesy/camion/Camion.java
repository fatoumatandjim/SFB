package com.backend.gesy.camion;

import com.backend.gesy.compte.Compte;
import com.backend.gesy.fournisseur.Fournisseur;
import com.backend.gesy.transaction.Transaction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "camions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Camion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String immatriculation;

    @Column
    private String modele;

    @Column
    private String marque;

    @Column
    private Integer annee;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Double capacite; // en litres

    @Column(name = "dernier_controle")
    private LocalDate dernierControle;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatutCamion statut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id", nullable = true)
    private Fournisseur fournisseur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id", nullable = true)
    private Compte responsable;

    @OneToMany(mappedBy = "camion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    public enum StatutCamion {
        DISPONIBLE,
        EN_ROUTE,
        EN_MAINTENANCE,
        HORS_SERVICE
    }
}
