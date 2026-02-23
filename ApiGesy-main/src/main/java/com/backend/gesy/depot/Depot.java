package com.backend.gesy.depot;

import com.backend.gesy.achat.Achat;
import com.backend.gesy.stock.Stock;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "depots")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Depot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String adresse;

    @Column(nullable = false)
    private Double capacite; // en litres

    @Column(name = "capacite_utilisee")
    private Double capaciteUtilisee;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatutDepot statut;

    private String ville;
    private String pays;
    private String responsable;
    private String telephone;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "depot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Stock> stocks = new ArrayList<>();

    @OneToMany(mappedBy = "depot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Achat> achats = new ArrayList<>();

    public enum StatutDepot {
        ACTIF,
        INACTIF,
        EN_MAINTENANCE,
        PLEIN
    }
}
