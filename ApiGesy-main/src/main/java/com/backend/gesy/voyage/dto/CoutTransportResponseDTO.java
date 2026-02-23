package com.backend.gesy.voyage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoutTransportResponseDTO {
    private List<CoutTransportDTO> couts;
    private CoutTransportStatsDTO stats;
}

