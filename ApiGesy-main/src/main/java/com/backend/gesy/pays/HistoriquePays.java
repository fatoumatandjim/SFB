package com.backend.gesy.pays;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "historique_pays")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoriquePays {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pays_id", nullable = false)
    private Pays pays;

    @Column(name = "ancien_frais_par_litre")
    private BigDecimal ancienFraisParLitre;

    @Column(name = "nouveau_frais_par_litre")
    private BigDecimal nouveauFraisParLitre;

    @Column(name = "ancien_frais_par_litre_gasoil")
    private BigDecimal ancienFraisParLitreGasoil;

    @Column(name = "nouveau_frais_par_litre_gasoil")
    private BigDecimal nouveauFraisParLitreGasoil;

    @Column(name = "ancien_frais_t1")
    private BigDecimal ancienFraisT1;

    @Column(name = "nouveau_frais_t1")
    private BigDecimal nouveauFraisT1;

    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;

    @Column(name = "modifie_par")
    private String modifiePar;

    @Column(name = "commentaire", length = 500)
    private String commentaire;
}
