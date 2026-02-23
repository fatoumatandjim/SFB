package com.backend.gesy.douane;

import com.backend.gesy.douane.dto.DouaneDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/douane")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DouaneController {
    private final DouaneServiceImpl douaneService;

    @GetMapping
    public ResponseEntity<DouaneDTO> getDouane() {
        try {
            DouaneDTO douane = douaneService.getDouane();
            return ResponseEntity.ok(douane);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping
    public ResponseEntity<DouaneDTO> updateDouane(@RequestBody DouaneDTO douaneDTO) {
        try {
            DouaneDTO updated = douaneService.update(douaneDTO);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/historique")
    public ResponseEntity<List<HistoriqueDouane>> getHistorique() {
        List<HistoriqueDouane> historique = douaneService.getHistorique();
        return ResponseEntity.ok(historique);
    }
}

