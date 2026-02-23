package com.backend.gesy.transitaire;

import com.backend.gesy.transitaire.dto.TransitaireDTO;
import com.backend.gesy.transitaire.dto.TransitairePageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transitaires")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransitaireController {
    private final TransitaireService transitaireService;

    @GetMapping
    public ResponseEntity<List<TransitaireDTO>> getAllTransitaires() {
        return ResponseEntity.ok(transitaireService.findAll());
    }

    @GetMapping("/paginated")
    public ResponseEntity<TransitairePageDto> getAllTransitairesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(transitaireService.findAllPaginated(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransitaireDTO> getTransitaireById(@PathVariable Long id) {
        return transitaireService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/identifiant/{identifiant}")
    public ResponseEntity<TransitaireDTO> getTransitaireByIdentifiant(@PathVariable String identifiant) {
        return transitaireService.findByIdentifiant(identifiant)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<TransitaireDTO> getTransitaireByEmail(@PathVariable String email) {
        return transitaireService.findByEmail(email)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TransitaireDTO> createTransitaire(@RequestBody TransitaireDTO transitaireDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transitaireService.save(transitaireDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransitaireDTO> updateTransitaire(@PathVariable Long id, @RequestBody TransitaireDTO transitaireDTO) {
        return ResponseEntity.ok(transitaireService.update(id, transitaireDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransitaire(@PathVariable Long id) {
        transitaireService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
