package com.backend.gesy.facture;

import com.backend.gesy.facture.dto.CreanceDTO;
import com.backend.gesy.facture.dto.FactureDTO;
import com.backend.gesy.facture.dto.FacturePageDto;
import com.backend.gesy.facture.dto.FactureStatsDTO;
import com.backend.gesy.facture.dto.RecouvrementStatsDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/factures")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FactureController {
    private final FactureService factureService;

    @GetMapping
    public ResponseEntity<List<FactureDTO>> getAllFactures() {
        return ResponseEntity.ok(factureService.findAll());
    }

    @GetMapping("/paginated")
    public ResponseEntity<FacturePageDto> getAllFacturesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(factureService.findAllPaginated(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FactureDTO> getFactureById(@PathVariable Long id) {
        return factureService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/numero/{numero}")
    public ResponseEntity<FactureDTO> getFactureByNumero(@PathVariable String numero) {
        return factureService.findByNumero(numero)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<FactureDTO>> getFacturesByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(factureService.findByClientId(clientId));
    }

    @GetMapping("/stats")
    public ResponseEntity<FactureStatsDTO> getStats() {
        return ResponseEntity.ok(factureService.getStats());
    }

    @GetMapping("/recouvrement/creances")
    public ResponseEntity<List<CreanceDTO>> getUnpaidFactures() {
        return ResponseEntity.ok(factureService.getUnpaidFactures());
    }

    @GetMapping("/recouvrement/stats")
    public ResponseEntity<RecouvrementStatsDTO> getRecouvrementStats() {
        return ResponseEntity.ok(factureService.getRecouvrementStats());
    }

    @PostMapping
    public ResponseEntity<FactureDTO> createFacture(@RequestBody FactureDTO factureDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(factureService.save(factureDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FactureDTO> updateFacture(@PathVariable Long id, @RequestBody FactureDTO factureDTO) {
        return ResponseEntity.ok(factureService.update(id, factureDTO));
    }

    @PutMapping("/{id}/statut")
    public ResponseEntity<FactureDTO> updateStatut(@PathVariable Long id, @RequestParam String statut) {
        return ResponseEntity.ok(factureService.updateStatut(id, statut));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFacture(@PathVariable Long id) {
        factureService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/client/{clientId}/export-pdf")
    public ResponseEntity<org.springframework.core.io.Resource> exportFacturesPdf(@PathVariable Long clientId) {
        try {
            byte[] pdfBytes = factureService.generateFacturesPdf(clientId);
            org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(pdfBytes);
            
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"factures_client_" + clientId + ".pdf\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

