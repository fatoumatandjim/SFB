package com.backend.gesy.stock;

import com.backend.gesy.stock.dto.StockDTO;
import com.backend.gesy.stock.dto.StockStatsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StockController {
    private final StockService stockService;

    @GetMapping
    public ResponseEntity<List<StockDTO>> getAllStocks() {
        return ResponseEntity.ok(stockService.findAll());
    }

    @GetMapping("/stats")
    public ResponseEntity<StockStatsDTO> getStats() {
        return ResponseEntity.ok(stockService.getStats());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockDTO> getStockById(@PathVariable Long id) {
        return stockService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/depot/{depotId}")
    public ResponseEntity<List<StockDTO>> getStocksByDepot(@PathVariable Long depotId) {
        return ResponseEntity.ok(stockService.findByDepotId(depotId));
    }

    @GetMapping("/produit/{produitId}")
    public ResponseEntity<List<StockDTO>> getStocksByProduit(@PathVariable Long produitId) {
        return ResponseEntity.ok(stockService.findByProduitId(produitId));
    }

    @PostMapping
    public ResponseEntity<StockDTO> createStock(@RequestBody StockDTO stockDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.save(stockDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StockDTO> updateStock(@PathVariable Long id, @RequestBody StockDTO stockDTO) {
        return ResponseEntity.ok(stockService.update(id, stockDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStock(@PathVariable Long id) {
        stockService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

