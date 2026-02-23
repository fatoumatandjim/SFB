package com.backend.gesy.voyage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoutTransportStatsDTO {
    private BigDecimal totalCout;
    private BigDecimal totalNonPaye;
    private BigDecimal totalPaye;
    private Long nombreVoyages;
}

