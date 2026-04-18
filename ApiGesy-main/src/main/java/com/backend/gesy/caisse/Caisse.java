package com.backend.gesy.caisse;

import com.backend.gesy.compte.Compte;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

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

    /** Utilisateurs applicatifs autorisés à gérer cette caisse (hors admin). Vide = pas de restriction (compatibilité). */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "caisse_responsables",
            joinColumns = @JoinColumn(name = "caisse_id"),
            inverseJoinColumns = @JoinColumn(name = "compte_id"))
    private Set<Compte> responsables = new HashSet<>();

    public enum StatutCaisse {
        ACTIF,
        FERME,
        SUSPENDU
    }
}

