package com.backend.gesy.douane.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FraisDouaneAxeDTO {
    private Long id;
    private Long axeId;
    private String axeNom;
    private BigDecimal fraisParLitre;
    private BigDecimal fraisParLitreGasoil;
    private BigDecimal fraisT1;
}
