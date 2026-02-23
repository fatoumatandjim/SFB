package com.backend.gesy.mouvement;

import com.backend.gesy.mouvement.dto.MouvementDTO;
import com.backend.gesy.mouvement.dto.MouvementPageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/mouvements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MouvementController {
    private final MouvementService mouvementService;

    @GetMapping
    public ResponseEntity<List<MouvementDTO>> getAllMouvements() {
        return ResponseEntity.ok(mouvementService.findAll());
    }

    @GetMapping("/recent")
    public ResponseEntity<List<MouvementDTO>> getRecentMouvements(
        @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(mouvementService.findRecent(limit));
    }

    @GetMapping("/paginated")
    public ResponseEntity<MouvementPageDTO> getMouvementsPaginated(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(mouvementService.findAllPaginated(pageable));
    }

    @GetMapping("/by-date")
    public ResponseEntity<MouvementPageDTO> getMouvementsByDate(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(mouvementService.findByDate(date, pageable));
    }

    @GetMapping("/by-date-range")
    public ResponseEntity<MouvementPageDTO> getMouvementsByDateRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(mouvementService.findByDateRange(startDate, endDate, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MouvementDTO> getMouvementById(@PathVariable Long id) {
        return mouvementService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stock/{stockId}")
    public ResponseEntity<List<MouvementDTO>> getMouvementsByStock(@PathVariable Long stockId) {
        return ResponseEntity.ok(mouvementService.findByStockId(stockId));
    }

    @PostMapping
    public ResponseEntity<MouvementDTO> createMouvement(@RequestBody MouvementDTO mouvementDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mouvementService.save(mouvementDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MouvementDTO> updateMouvement(@PathVariable Long id, @RequestBody MouvementDTO mouvementDTO) {
        return ResponseEntity.ok(mouvementService.update(id, mouvementDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMouvement(@PathVariable Long id) {
        mouvementService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

