package com.backend.gesy.abonnement;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "abonnements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Abonnement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TypeAbonnement type;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(nullable = false)
    private Boolean actif;

    @Column(precision = 15, scale = 2)
    private BigDecimal prix;

    private String description;

    public enum TypeAbonnement {
        MENSUEL,
        TRIMESTRIEL,
        SEMESTRIEL,
        ANNUEL
    }
}
