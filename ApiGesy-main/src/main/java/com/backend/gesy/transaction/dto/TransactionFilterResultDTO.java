package com.backend.gesy.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionFilterResultDTO {
    private List<TransactionDTO> transactions;
    private long totalCount;
    private BigDecimal totalMontant;
    private int currentPage;
    private int totalPages;
    private int size;
}
