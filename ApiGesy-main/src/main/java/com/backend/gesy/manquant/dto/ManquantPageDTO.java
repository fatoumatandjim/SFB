package com.backend.gesy.manquant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManquantPageDTO {
    private List<ManquantDTO> manquants;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}

