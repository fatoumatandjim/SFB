package com.backend.gesy.voyage;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "etats_voyage")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EtatVoyage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String etat;

    @Column(name = "date_heure", nullable = false)
    private LocalDateTime dateHeure;

    @Column(nullable = false)
    private Boolean valider = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voyage_id", nullable = false)
    private Voyage voyage;
}

