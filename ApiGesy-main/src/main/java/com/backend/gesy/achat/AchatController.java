package com.backend.gesy.achat;

import com.backend.gesy.achat.dto.AchatDTO;
import com.backend.gesy.achat.dto.AchatPageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/achats")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AchatController {
    private final AchatService achatService;

    @GetMapping
    public ResponseEntity<List<AchatDTO>> getAllAchats() {
        return ResponseEntity.ok(achatService.findAll());
    }

    @GetMapping("/paginated")
    public ResponseEntity<AchatPageDTO> getAllAchatsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(achatService.findAllPaginated(page, size));
    }

    @GetMapping("/paginated/date")
    public ResponseEntity<AchatPageDTO> getAchatsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(achatService.findByDate(date, page, size));
    }

    @GetMapping("/paginated/date-range")
    public ResponseEntity<AchatPageDTO> getAchatsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(achatService.findByDateRange(startDate, endDate, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AchatDTO> getAchatById(@PathVariable Long id) {
        return ResponseEntity.ok(achatService.findById(id));
    }

    @GetMapping("/depot/{depotId}")
    public ResponseEntity<List<AchatDTO>> getAchatsByDepot(@PathVariable Long depotId) {
        return ResponseEntity.ok(achatService.findByDepotId(depotId));
    }

    @GetMapping("/depot/{depotId}/paginated")
    public ResponseEntity<AchatPageDTO> getAchatsByDepotPaginated(
            @PathVariable Long depotId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(achatService.findByDepotIdPaginated(depotId, page, size));
    }

    @GetMapping("/depot/{depotId}/paginated/date")
    public ResponseEntity<AchatPageDTO> getAchatsByDepotAndDate(
            @PathVariable Long depotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(achatService.findByDepotIdAndDate(depotId, date, page, size));
    }

    @GetMapping("/depot/{depotId}/paginated/date-range")
    public ResponseEntity<AchatPageDTO> getAchatsByDepotAndDateRange(
            @PathVariable Long depotId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(achatService.findByDepotIdAndDateRange(depotId, startDate, endDate, page, size));
    }

    @GetMapping("/produit/{produitId}")
    public ResponseEntity<List<AchatDTO>> getAchatsByProduit(@PathVariable Long produitId) {
        return ResponseEntity.ok(achatService.findByProduitId(produitId));
    }

    @PostMapping
    public ResponseEntity<AchatDTO> createAchat(@RequestBody AchatDTO achatDTO) {
        return ResponseEntity.ok(achatService.save(achatDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAchat(@PathVariable Long id) {
        achatService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/marge")
    public ResponseEntity<com.backend.gesy.achat.dto.AchatMargeDTO> getMargeAchat(@PathVariable Long id) {
        return ResponseEntity.ok(achatService.calculateMarge(id));
    }

    @PostMapping("/with-facture")
    public ResponseEntity<AchatDTO> createAchatWithFacture(@RequestBody com.backend.gesy.achat.dto.CreateAchatWithFactureDTO dto) {
        return ResponseEntity.ok(achatService.createAchatWithFacture(dto));
    }

    @PostMapping("/payer")
    public ResponseEntity<AchatDTO> payerAchat(@RequestBody com.backend.gesy.achat.dto.PayerAchatDTO dto) {
        return ResponseEntity.ok(achatService.payerAchat(dto));
    }

    @GetMapping("/statut/{statut}/paginated")
    public ResponseEntity<AchatPageDTO> getAchatsByStatutFacture(
            @PathVariable String statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(achatService.findByStatutFacture(statut, page, size));
    }

    @GetMapping("/statut/{statut}/paginated/date")
    public ResponseEntity<AchatPageDTO> getAchatsByStatutFactureAndDate(
            @PathVariable String statut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(achatService.findByStatutFactureAndDate(statut, date, page, size));
    }

    @GetMapping("/statut/{statut}/paginated/date-range")
    public ResponseEntity<AchatPageDTO> getAchatsByStatutFactureAndDateRange(
            @PathVariable String statut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(achatService.findByStatutFactureAndDateRange(statut, startDate, endDate, page, size));
    }

    @PostMapping("/cession")
    public ResponseEntity<AchatDTO> createAchatCession(@RequestBody com.backend.gesy.achat.dto.CreateAchatCessionDTO dto) {
        return ResponseEntity.ok(achatService.createAchatCession(dto));
    }

    @GetMapping("/cession/paginated")
    public ResponseEntity<AchatPageDTO> getAchatsCessionPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(achatService.findCessionPaginated(page, size));
    }

    @GetMapping("/cession/paginated/date")
    public ResponseEntity<AchatPageDTO> getAchatsCessionByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(achatService.findCessionByDate(date, page, size));
    }

    @GetMapping("/cession/paginated/date-range")
    public ResponseEntity<AchatPageDTO> getAchatsCessionByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(achatService.findCessionByDateRange(startDate, endDate, page, size));
    }
}

