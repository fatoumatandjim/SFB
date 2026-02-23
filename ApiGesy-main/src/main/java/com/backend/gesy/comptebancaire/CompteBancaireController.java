package com.backend.gesy.comptebancaire;

import com.backend.gesy.comptebancaire.dto.BanqueCaisseStatsDTO;
import com.backend.gesy.comptebancaire.dto.CompteBancaireDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comptes-bancaires")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CompteBancaireController {
    private final CompteBancaireService compteBancaireService;

    @GetMapping
    public ResponseEntity<List<CompteBancaireDTO>> getAllComptes() {
        return ResponseEntity.ok(compteBancaireService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompteBancaireDTO> getCompteById(@PathVariable Long id) {
        return compteBancaireService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<CompteBancaireDTO> getCompteByNumero(@PathVariable String numero) {
        return compteBancaireService.findByNumero(numero)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CompteBancaireDTO> createCompte(@RequestBody CompteBancaireDTO compteDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compteBancaireService.save(compteDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompteBancaireDTO> updateCompte(@PathVariable Long id, @RequestBody CompteBancaireDTO compteDTO) {
        return ResponseEntity.ok(compteBancaireService.update(id, compteDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompte(@PathVariable Long id) {
        compteBancaireService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<BanqueCaisseStatsDTO> getStats() {
        return ResponseEntity.ok(compteBancaireService.getStats());
    }
}

