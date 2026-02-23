package com.backend.gesy.caisse;

import com.backend.gesy.caisse.dto.CaisseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/caisses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CaisseController {
    private final CaisseService caisseService;

    @GetMapping
    public ResponseEntity<List<CaisseDTO>> getAllCaisses() {
        return ResponseEntity.ok(caisseService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CaisseDTO> getCaisseById(@PathVariable Long id) {
        return caisseService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nom/{nom}")
    public ResponseEntity<CaisseDTO> getCaisseByNom(@PathVariable String nom) {
        return caisseService.findByNom(nom)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CaisseDTO> createCaisse(@RequestBody CaisseDTO caisseDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(caisseService.save(caisseDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CaisseDTO> updateCaisse(@PathVariable Long id, @RequestBody CaisseDTO caisseDTO) {
        return ResponseEntity.ok(caisseService.update(id, caisseDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCaisse(@PathVariable Long id) {
        caisseService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

