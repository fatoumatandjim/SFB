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

    /** Noms de catégories utilisés pour les paiements (transport, T1, douane). */
    public static final String NOM_COUT_TRANSPORT = "Coût de transport";
    public static final String NOM_DROIT_DOUANE = "Droit de douane";
    public static final String NOM_FRAIS_T1 = "Frais T1";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatutCategorie statut = StatutCategorie.ACTIF;

    /** Prix unitaires transport (FCFA/litre) liés à cette catégorie, stockés en CSV (ex: "47,50"). */
    @Column(name = "tarifs_transport", length = 500)
    private String tarifsTransport;

    public enum StatutCategorie {
        ACTIF,
        INACTIF
    }
}

