package com.backend.gesy.depense;

import com.backend.gesy.depense.dto.DepenseDTO;
import com.backend.gesy.depense.dto.DepensePageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/depenses")
@RequiredArgsConstructor
public class DepenseController {
    
    private final DepenseService depenseService;

    // CRUD
    @PostMapping
    public ResponseEntity<DepenseDTO> create(@RequestBody DepenseDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(depenseService.save(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepenseDTO> update(@PathVariable Long id, @RequestBody DepenseDTO dto) {
        return ResponseEntity.ok(depenseService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepenseDTO> getById(@PathVariable Long id) {
        return depenseService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        depenseService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Liste sans pagination
    @GetMapping("/all")
    public ResponseEntity<List<DepenseDTO>> getAll() {
        return ResponseEntity.ok(depenseService.findAll());
    }

    @GetMapping("/categorie/{categorieId}/all")
    public ResponseEntity<List<DepenseDTO>> getAllByCategorie(@PathVariable Long categorieId) {
        return ResponseEntity.ok(depenseService.findByCategorie(categorieId));
    }

    // Liste avec pagination
    @GetMapping
    public ResponseEntity<DepensePageDTO> getAllPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(depenseService.findAllPaginated(page, size));
    }

    @GetMapping("/categorie/{categorieId}")
    public ResponseEntity<DepensePageDTO> getByCategoriePaginated(
            @PathVariable Long categorieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(depenseService.findByCategoriePaginated(categorieId, page, size));
    }

    // Filtres par date
    @GetMapping("/date/{date}")
    public ResponseEntity<DepensePageDTO> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(depenseService.findByDate(date, page, size));
    }

    @GetMapping("/date-range")
    public ResponseEntity<DepensePageDTO> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(depenseService.findByDateRange(startDate, endDate, page, size));
    }

    // Filtres par catégorie et date
    @GetMapping("/categorie/{categorieId}/date/{date}")
    public ResponseEntity<DepensePageDTO> getByCategorieAndDate(
            @PathVariable Long categorieId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(depenseService.findByCategorieAndDate(categorieId, date, page, size));
    }

    @GetMapping("/categorie/{categorieId}/date-range")
    public ResponseEntity<DepensePageDTO> getByCategorieAndDateRange(
            @PathVariable Long categorieId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(depenseService.findByCategorieAndDateRange(categorieId, startDate, endDate, page, size));
    }

    // Statistiques
    @GetMapping("/stats/sum/categorie/{categorieId}")
    public ResponseEntity<BigDecimal> sumByCategorie(@PathVariable Long categorieId) {
        return ResponseEntity.ok(depenseService.sumByCategorie(categorieId));
    }

    @GetMapping("/stats/sum/date-range")
    public ResponseEntity<BigDecimal> sumByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(depenseService.sumByDateRange(startDate, endDate));
    }
}

