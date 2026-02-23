package com.backend.gesy.douane;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "historique_douanes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoriqueDouane {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
