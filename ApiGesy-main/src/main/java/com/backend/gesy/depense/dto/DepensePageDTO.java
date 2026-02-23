package com.backend.gesy.depense.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepensePageDTO {
    private List<DepenseDTO> depenses;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}

