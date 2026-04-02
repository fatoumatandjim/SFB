package com.backend.gesy.comptebancaire;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "comptes_bancaires")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CompteBancaire {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numero;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TypeCompte type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal solde;

    @Column(nullable = false)
    private String banque;

    @Column(name = "numero_compte_bancaire")
    private String numeroCompteBancaire;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatutCompte statut;

    private String description;

    /** Compte bancaire ou portefeuille mobile ; la caisse physique est l'entité {@link com.backend.gesy.caisse.Caisse}. */
    public enum TypeCompte {
        BANQUE,
        MOBILE_MONEY
    }

    public enum StatutCompte {
        ACTIF,
        FERME,
        SUSPENDU
    }
}
