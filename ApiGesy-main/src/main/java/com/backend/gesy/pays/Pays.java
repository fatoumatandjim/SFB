package com.backend.gesy.pays;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "pays")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pays {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(name = "frais_par_litre", nullable = false)
    private BigDecimal fraisParLitre = BigDecimal.ZERO;

    @Column(name = "frais_par_litre_gasoil", nullable = false)
    private BigDecimal fraisParLitreGasoil = BigDecimal.ZERO;

    @Column(name = "frais_t1", nullable = false)
    private BigDecimal fraisT1 = BigDecimal.ZERO;
}
