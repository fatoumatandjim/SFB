package com.backend.gesy.stock;

import com.backend.gesy.stock.dto.StockDTO;
import com.backend.gesy.stock.dto.StockStatsDTO;

import java.util.List;
import java.util.Optional;

public interface StockService {
    List<StockDTO> findAll();
    Optional<StockDTO> findById(Long id);
    List<StockDTO> findByDepotId(Long depotId);
    List<StockDTO> findByProduitId(Long produitId);
    StockDTO save(StockDTO stockDTO);
    StockDTO update(Long id, StockDTO stockDTO);
    void deleteById(Long id);
    StockStatsDTO getStats();
}

