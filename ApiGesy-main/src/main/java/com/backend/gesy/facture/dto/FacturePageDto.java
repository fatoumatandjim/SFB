package com.backend.gesy.facture.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacturePageDto {
    private List<FactureDTO> factures;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int size;
}
