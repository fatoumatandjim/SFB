package com.backend.gesy.alerte;

import com.backend.gesy.alerte.dto.AlertePageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alertes")
@RequiredArgsConstructor
public class AlerteController {
    private final AlerteService alerteService;

    @GetMapping
    public ResponseEntity<List<Alerte>> getAllAlertes() {
        return ResponseEntity.ok(alerteService.findAll());
    }

    /**
     * Pagination : alertes triées par date décroissante.
     * @param page numéro de page (0-based)
     * @param size nombre d'éléments par page
     */
    @GetMapping("/page")
    public ResponseEntity<AlertePageDTO> getAlertesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(alerteService.findPaginated(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Alerte> getAlerteById(@PathVariable Long id) {
        return alerteService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/non-lues")
    public ResponseEntity<List<Alerte>> getAlertesNonLues() {
        return ResponseEntity.ok(alerteService.findByLu(false));
    }

    @PostMapping
    public ResponseEntity<Alerte> createAlerte(@RequestBody Alerte alerte) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alerteService.save(alerte));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Alerte> updateAlerte(@PathVariable Long id, @RequestBody Alerte alerte) {
        return ResponseEntity.ok(alerteService.update(id, alerte));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlerte(@PathVariable Long id) {
        alerteService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

