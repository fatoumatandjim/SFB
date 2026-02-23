package com.backend.gesy.manquant;

import com.backend.gesy.manquant.dto.ManquantDTO;
import com.backend.gesy.manquant.dto.ManquantPageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/manquants")
@RequiredArgsConstructor
public class ManquantController {
    
    private final ManquantService manquantService;

    // CRUD
    @PostMapping
    public ResponseEntity<ManquantDTO> create(@RequestBody ManquantDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(manquantService.save(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ManquantDTO> getById(@PathVariable Long id) {
        return manquantService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        manquantService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Liste sans pagination
    @GetMapping("/all")
    public ResponseEntity<List<ManquantDTO>> getAll() {
        return ResponseEntity.ok(manquantService.findAll());
    }

    @GetMapping("/voyage/{voyageId}")
    public ResponseEntity<List<ManquantDTO>> getByVoyageId(@PathVariable Long voyageId) {
        return ResponseEntity.ok(manquantService.findByVoyageId(voyageId));
    }

    // Liste avec pagination
    @GetMapping
    public ResponseEntity<ManquantPageDTO> getAllPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(manquantService.findAllPaginated(page, size));
    }

    // Filtres par date
    @GetMapping("/date/{date}")
    public ResponseEntity<ManquantPageDTO> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(manquantService.findByDate(date, page, size));
    }

    @GetMapping("/date-range")
    public ResponseEntity<ManquantPageDTO> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(manquantService.findByDateRange(startDate, endDate, page, size));
    }
}

