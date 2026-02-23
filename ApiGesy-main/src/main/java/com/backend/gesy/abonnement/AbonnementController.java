package com.backend.gesy.abonnement;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/abonnements")
@RequiredArgsConstructor
public class AbonnementController {
    private final AbonnementService abonnementService;

    @GetMapping
    public ResponseEntity<List<Abonnement>> getAllAbonnements() {
        return ResponseEntity.ok(abonnementService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Abonnement> getAbonnementById(@PathVariable Long id) {
        return abonnementService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Abonnement> createAbonnement(@RequestBody Abonnement abonnement) {
        return ResponseEntity.status(HttpStatus.CREATED).body(abonnementService.save(abonnement));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Abonnement> updateAbonnement(@PathVariable Long id, @RequestBody Abonnement abonnement) {
        return ResponseEntity.ok(abonnementService.update(id, abonnement));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAbonnement(@PathVariable Long id) {
        abonnementService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

