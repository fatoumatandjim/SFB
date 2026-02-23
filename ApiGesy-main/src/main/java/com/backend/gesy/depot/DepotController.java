package com.backend.gesy.depot;

import com.backend.gesy.depot.dto.DepotDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/depots")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DepotController {
    private final DepotService depotService;

    @GetMapping
    public ResponseEntity<List<DepotDTO>> getAllDepots() {
        return ResponseEntity.ok(depotService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepotDTO> getDepotById(@PathVariable Long id) {
        return depotService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<DepotDTO> createDepot(@RequestBody DepotDTO depotDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(depotService.save(depotDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepotDTO> updateDepot(@PathVariable Long id, @RequestBody DepotDTO depotDTO) {
        return ResponseEntity.ok(depotService.update(id, depotDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepot(@PathVariable Long id) {
        depotService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

