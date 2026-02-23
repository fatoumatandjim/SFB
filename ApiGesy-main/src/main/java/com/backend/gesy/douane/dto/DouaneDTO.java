package com.backend.gesy.douane.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DouaneDTO {
    private Long id;
    private BigDecimal fraisParLitre;
    private BigDecimal fraisParLitreGasoil;
    private BigDecimal fraisT1;
}

