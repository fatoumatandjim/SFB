package com.backend.gesy.voyage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoyagePageDto {
    private List<VoyageDTO> voyages;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int size;
}

