package com.backend.gesy.douane;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "douanes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Douane {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "frais_par_litre", nullable = false)
    private BigDecimal fraisParLitre;
    @Column(name = "frais_par_litre_gasoil", nullable = false)
    private BigDecimal fraisParLitreGasoil;

    @Column(name = "frais_t1", nullable = false)
    private BigDecimal fraisT1;
}

