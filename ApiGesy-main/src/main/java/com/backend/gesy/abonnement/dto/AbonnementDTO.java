package com.backend.gesy.abonnement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbonnementDTO {
    private Long id;
    private String type;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Boolean actif;
    private BigDecimal prix;
    private String description;
}

