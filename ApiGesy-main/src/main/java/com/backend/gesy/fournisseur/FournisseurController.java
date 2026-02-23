package com.backend.gesy.fournisseur;

import com.backend.gesy.fournisseur.dto.FournisseurDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fournisseurs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FournisseurController {
    private final FournisseurService fournisseurService;

    @GetMapping
    public ResponseEntity<List<FournisseurDTO>> getAllFournisseurs() {
        return ResponseEntity.ok(fournisseurService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FournisseurDTO> getFournisseurById(@PathVariable Long id) {
        return fournisseurService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<FournisseurDTO>> getFournisseursByType(@PathVariable String type) {
        return ResponseEntity.ok(fournisseurService.findByType(type));
    }

    @PostMapping
    public ResponseEntity<FournisseurDTO> createFournisseur(@RequestBody FournisseurDTO fournisseurDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fournisseurService.save(fournisseurDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FournisseurDTO> updateFournisseur(@PathVariable Long id, @RequestBody FournisseurDTO fournisseurDTO) {
        return ResponseEntity.ok(fournisseurService.update(id, fournisseurDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFournisseur(@PathVariable Long id) {
        fournisseurService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

