package com.backend.gesy.depense.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepenseUnifiedPageDTO {
    private List<UnifiedLigneDepenseDTO> lignes;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
