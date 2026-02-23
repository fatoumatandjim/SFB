package com.backend.gesy.alerte.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertePageDTO {
    private List<AlerteDTO> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
