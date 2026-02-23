package com.backend.gesy.achat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchatPageDTO {
    private List<AchatDTO> achats;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int size;
}

