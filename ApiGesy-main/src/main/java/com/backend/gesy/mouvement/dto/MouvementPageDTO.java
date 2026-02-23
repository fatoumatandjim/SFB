package com.backend.gesy.mouvement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MouvementPageDTO {
    private List<MouvementDTO> mouvements;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
}

