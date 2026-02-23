package com.backend.gesy.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockStatsDTO {
    private Long totalUnites;
    private String evolutionTotalUnites;
    private String periodeTotalUnites;
    private Integer totalDepots;
    private Integer villesDepots;
    private Integer produitsCritiques;
    private Boolean urgentProduitsCritiques;
    private Double valeurStock;
    private String evolutionValeurStock;
    private String periodeValeurStock;
}

