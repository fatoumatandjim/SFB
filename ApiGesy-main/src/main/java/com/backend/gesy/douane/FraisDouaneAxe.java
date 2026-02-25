package com.backend.gesy.douane;

import com.backend.gesy.axe.Axe;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "frais_douane_axe", uniqueConstraints = @UniqueConstraint(columnNames = "axe_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraisDouaneAxe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "axe_id", nullable = false, unique = true)
    private Axe axe;

    @Column(name = "frais_par_litre", nullable = false)
    private BigDecimal fraisParLitre;

    @Column(name = "frais_par_litre_gasoil", nullable = false)
    private BigDecimal fraisParLitreGasoil;

    @Column(name = "frais_t1", nullable = false)
    private BigDecimal fraisT1;
}
