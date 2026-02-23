package com.backend.gesy.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionPageDTO {
    private List<TransactionDTO> transactions;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int size;
}

