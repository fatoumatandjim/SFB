package com.backend.gesy.categoriedepense;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories_depenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategorieDepense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatutCategorie statut = StatutCategorie.ACTIF;

    public enum StatutCategorie {
        ACTIF,
        INACTIF
    }
}

