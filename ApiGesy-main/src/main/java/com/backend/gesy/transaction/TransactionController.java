package com.backend.gesy.transaction;

import com.backend.gesy.transaction.dto.TransactionDTO;
import com.backend.gesy.transaction.dto.TransactionFilterResultDTO;
import com.backend.gesy.transaction.dto.TransactionPageDTO;
import com.backend.gesy.transaction.dto.TransactionStatsDTO;
import com.backend.gesy.transaction.dto.VirementRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransactionController {
    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getAllTransactions() {
        return ResponseEntity.ok(transactionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> getTransactionById(@PathVariable Long id) {
        return transactionService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/camion/{camionId}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByCamion(@PathVariable Long camionId) {
        return ResponseEntity.ok(transactionService.findByCamionId(camionId));
    }

    @GetMapping("/facture/{factureId}")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByFacture(@PathVariable Long factureId) {
        return ResponseEntity.ok(transactionService.findByFactureId(factureId));
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> createTransaction(@RequestBody TransactionDTO transactionDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.save(transactionDTO));
    }

    @PostMapping("/paiement")
    public ResponseEntity<TransactionDTO> createPaiement(@RequestBody TransactionDTO transactionDTO) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.createPaiement(transactionDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/virement")
    public ResponseEntity<List<TransactionDTO>> createVirement(@RequestBody VirementRequestDTO request) {
        try {
            List<TransactionDTO> transactions = transactionService.createTransactionLiee(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(transactions);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionDTO> updateTransaction(@PathVariable Long id, @RequestBody TransactionDTO transactionDTO) {
        return ResponseEntity.ok(transactionService.update(id, transactionDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recent")
    public ResponseEntity<List<TransactionDTO>> getRecentTransactions(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(transactionService.findRecentTransactions(limit));
    }

    @GetMapping("/paginated")
    public ResponseEntity<TransactionPageDTO> getTransactionsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(transactionService.findAllPaginated(page, size));
    }

    @GetMapping("/paginated/date")
    public ResponseEntity<TransactionPageDTO> getTransactionsByDate(
            @RequestParam String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        LocalDate localDate = LocalDate.parse(date);
        return ResponseEntity.ok(transactionService.findByDate(localDate, page, size));
    }

    @GetMapping("/paginated/range")
    public ResponseEntity<TransactionPageDTO> getTransactionsByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return ResponseEntity.ok(transactionService.findByDateRange(start, end, page, size));
    }

    @GetMapping("/stats")
    public ResponseEntity<TransactionStatsDTO> getStats() {
        return ResponseEntity.ok(transactionService.getStats());
    }

    /**
     * Filtre personnalisé: par type de transaction, optionnellement par date ou intervalle de dates.
     * Retourne les transactions paginées avec le nombre total et le montant total des éléments filtrés.
     */
    @GetMapping("/filter")
    public ResponseEntity<TransactionFilterResultDTO> filterByCustom(
            @RequestParam(required = false) Transaction.TypeTransaction type,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        LocalDate dateParsed = date != null && !date.isEmpty() ? LocalDate.parse(date) : null;
        LocalDate startParsed = startDate != null && !startDate.isEmpty() ? LocalDate.parse(startDate) : null;
        LocalDate endParsed = endDate != null && !endDate.isEmpty() ? LocalDate.parse(endDate) : null;
        return ResponseEntity.ok(transactionService.filterByCustom(type, dateParsed, startParsed, endParsed, page, size));
    }

    // Endpoints pour filtrer par compte bancaire avec pagination
    @GetMapping("/compte/{compteId}/paginated")
    public ResponseEntity<TransactionPageDTO> getTransactionsByCompteIdPaginated(
            @PathVariable Long compteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(transactionService.findByCompteIdPaginated(compteId, page, size));
    }

    @GetMapping("/compte/{compteId}/paginated/date")
    public ResponseEntity<TransactionPageDTO> getTransactionsByCompteIdAndDate(
            @PathVariable Long compteId,
            @RequestParam String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        LocalDate localDate = LocalDate.parse(date);
        return ResponseEntity.ok(transactionService.findByCompteIdAndDate(compteId, localDate, page, size));
    }

    @GetMapping("/compte/{compteId}/paginated/range")
    public ResponseEntity<TransactionPageDTO> getTransactionsByCompteIdAndDateRange(
            @PathVariable Long compteId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return ResponseEntity.ok(transactionService.findByCompteIdAndDateRange(compteId, start, end, page, size));
    }

    // Endpoints pour filtrer par caisse avec pagination
    @GetMapping("/caisse/{caisseId}/paginated")
    public ResponseEntity<TransactionPageDTO> getTransactionsByCaisseIdPaginated(
            @PathVariable Long caisseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(transactionService.findByCaisseIdPaginated(caisseId, page, size));
    }

    @GetMapping("/caisse/{caisseId}/paginated/date")
    public ResponseEntity<TransactionPageDTO> getTransactionsByCaisseIdAndDate(
            @PathVariable Long caisseId,
            @RequestParam String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        LocalDate localDate = LocalDate.parse(date);
        return ResponseEntity.ok(transactionService.findByCaisseIdAndDate(caisseId, localDate, page, size));
    }

    @GetMapping("/caisse/{caisseId}/paginated/range")
    public ResponseEntity<TransactionPageDTO> getTransactionsByCaisseIdAndDateRange(
            @PathVariable Long caisseId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return ResponseEntity.ok(transactionService.findByCaisseIdAndDateRange(caisseId, start, end, page, size));
    }

    // Endpoints pour filtrer uniquement les comptes bancaires ou les caisses
    @GetMapping("/comptes-bancaires/paginated")
    public ResponseEntity<TransactionPageDTO> getTransactionsByComptesBancairesOnly(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(transactionService.findByComptesBancairesOnlyPaginated(page, size));
    }

    @GetMapping("/caisses/paginated")
    public ResponseEntity<TransactionPageDTO> getTransactionsByCaissesOnly(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(transactionService.findByCaissesOnlyPaginated(page, size));
    }

    // Endpoints pour récupérer toutes les transactions (sans pagination) pour l'export
    @GetMapping("/date")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByDateAll(@RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        return ResponseEntity.ok(transactionService.findByDateAll(localDate));
    }

    @GetMapping("/range")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByDateRangeAll(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        return ResponseEntity.ok(transactionService.findByDateRangeAll(start, end));
    }

    /**
     * Recalcule les soldes de tous les comptes bancaires et caisses à partir des transactions validées.
     * À appeler une fois pour corriger les soldes des transactions créées avant la mise à jour automatique.
     */
    @PostMapping("/recalculer-soldes")
    public ResponseEntity<Void> recalculerSoldes() {
        try {
            transactionService.recalculerSoldesDepuisTransactions();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

