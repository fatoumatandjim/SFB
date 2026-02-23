package com.backend.gesy.capitale;

import com.backend.gesy.capitale.dto.CapitaleDTO;

import java.time.LocalDate;

public interface CapitaleService {
    CapitaleDTO calculateCapitale();
    CapitaleDTO calculateCapitaleByMonth(int year, int month);
    CapitaleDTO calculateCapitaleByDateRange(LocalDate startDate, LocalDate endDate);
}
