package com.backend.gesy.caisse;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "caisses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Caisse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nom;

    @Column(nullable = false)
    private BigDecimal solde;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatutCaisse statut;

    private String description;

    public enum StatutCaisse {
        ACTIF,
        FERME,
        SUSPENDU
    }
}

