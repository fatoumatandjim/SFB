package com.backend.gesy.transitaire.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransitairePageDto {
    private List<TransitaireDTO> transitaires;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int size;
}
