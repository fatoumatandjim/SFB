package com.backend.gesy.paiement;

import com.backend.gesy.paiement.dto.PaiementDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paiements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaiementController {
    private final PaiementService paiementService;

    @GetMapping
    public ResponseEntity<List<PaiementDTO>> getAllPaiements() {
        return ResponseEntity.ok(paiementService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaiementDTO> getPaiementById(@PathVariable Long id) {
        return paiementService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/facture/{factureId}")
    public ResponseEntity<List<PaiementDTO>> getPaiementsByFacture(@PathVariable Long factureId) {
        return ResponseEntity.ok(paiementService.findByFactureId(factureId));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<PaiementDTO>> getPaiementsByStatut(@PathVariable String statut) {
        Paiement.StatutPaiement statutPaiement = Paiement.StatutPaiement.valueOf(statut);
        return ResponseEntity.ok(paiementService.findByStatut(statutPaiement));
    }

    @PostMapping
    public ResponseEntity<PaiementDTO> createPaiement(@RequestBody PaiementDTO paiementDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paiementService.save(paiementDTO));
    }

    @PutMapping("/{id}/valider")
    public ResponseEntity<PaiementDTO> validerPaiement(
            @PathVariable Long id,
            @RequestParam(required = false) Long compteId,
            @RequestParam(required = false) Long caisseId) {
        return ResponseEntity.ok(paiementService.validerPaiement(id, compteId, caisseId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaiementDTO> updatePaiement(@PathVariable Long id, @RequestBody PaiementDTO paiementDTO) {
        return ResponseEntity.ok(paiementService.update(id, paiementDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaiement(@PathVariable Long id) {
        paiementService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

