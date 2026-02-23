package com.backend.gesy.voyage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransitaireStatsDTO {
    private long nombreCamionsDeclaresCeMois;
    /** Total des frais de douane ce mois (FRAIS_DOUANE). */
    private BigDecimal totalFraisDouaneCeMois;
    /** Total des frais T1 ce mois (FRAIS_T1). */
    private BigDecimal totalMontantT1CeMois;
    /** Total global (douane + T1), pour compatibilité. */
    private BigDecimal totalFraisCeMois;
}

