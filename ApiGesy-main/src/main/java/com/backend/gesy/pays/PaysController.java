package com.backend.gesy.pays;

import com.backend.gesy.pays.dto.HistoriquePaysDTO;
import com.backend.gesy.pays.dto.PaysDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pays")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaysController {

    private final PaysService paysService;

    @GetMapping
    public ResponseEntity<List<PaysDTO>> findAll() {
        return ResponseEntity.ok(paysService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaysDTO> findById(@PathVariable Long id) {
        return paysService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody PaysDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(paysService.save(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PaysDTO dto) {
        try {
            return ResponseEntity.ok(paysService.update(id, dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        try {
            paysService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/historique")
    public ResponseEntity<List<HistoriquePaysDTO>> getHistorique(@PathVariable Long id) {
        return ResponseEntity.ok(paysService.getHistoriqueByPaysId(id));
    }
}
