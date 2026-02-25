package com.backend.gesy.pays.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaysDTO {
    private Long id;
    private String nom;
    private BigDecimal fraisParLitre;
    private BigDecimal fraisParLitreGasoil;
    private BigDecimal fraisT1;
}
