package com.backend.gesy.transaction;

import com.backend.gesy.transaction.dto.TransactionDTO;
import com.backend.gesy.transaction.dto.TransactionFilterResultDTO;
import com.backend.gesy.transaction.dto.TransactionPageDTO;
import com.backend.gesy.transaction.dto.TransactionStatsDTO;
import com.backend.gesy.transaction.dto.VirementRequestDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionService {
    List<TransactionDTO> findAll();
    Optional<TransactionDTO> findById(Long id);
    List<TransactionDTO> findByCompteId(Long compteId);
    List<TransactionDTO> findByCamionId(Long camionId);
    List<TransactionDTO> findByFactureId(Long factureId);
    TransactionDTO save(TransactionDTO transactionDTO);
    TransactionDTO update(Long id, TransactionDTO transactionDTO);
    void deleteById(Long id);
    
    // Nouvelle méthode pour créer des transactions liées avec mise à jour des soldes
    List<TransactionDTO> createTransactionLiee(VirementRequestDTO request);
    
    // Méthode pour créer un paiement (débite automatiquement le compte bancaire ou la caisse)
    TransactionDTO createPaiement(TransactionDTO transactionDTO);
    
    // Méthodes de pagination
    List<TransactionDTO> findRecentTransactions(int limit);
    TransactionPageDTO findAllPaginated(int page, int size);
    TransactionPageDTO findByDate(LocalDate date, int page, int size);
    TransactionPageDTO findByDateRange(LocalDate startDate, LocalDate endDate, int page, int size);
    
    // Méthodes pour récupérer toutes les transactions (sans pagination) pour l'export
    List<TransactionDTO> findByDateAll(LocalDate date);
    List<TransactionDTO> findByDateRangeAll(LocalDate startDate, LocalDate endDate);
    
    // Méthodes de pagination filtrées par compte bancaire ou caisse
    TransactionPageDTO findByCompteIdPaginated(Long compteId, int page, int size);
    TransactionPageDTO findByCaisseIdPaginated(Long caisseId, int page, int size);
    TransactionPageDTO findByComptesBancairesOnlyPaginated(int page, int size);
    TransactionPageDTO findByCaissesOnlyPaginated(int page, int size);
    TransactionPageDTO findByCompteIdAndDate(Long compteId, LocalDate date, int page, int size);
    TransactionPageDTO findByCaisseIdAndDate(Long caisseId, LocalDate date, int page, int size);
    TransactionPageDTO findByCompteIdAndDateRange(Long compteId, LocalDate startDate, LocalDate endDate, int page, int size);
    TransactionPageDTO findByCaisseIdAndDateRange(Long caisseId, LocalDate startDate, LocalDate endDate, int page, int size);
    
    // Méthode pour obtenir les statistiques des transactions
    TransactionStatsDTO getStats();

    /**
     * Filtre personnalisé: par type de transaction, optionnellement par date ou intervalle de dates.
     * Retourne les transactions paginées avec le nombre total et le montant total des éléments filtrés.
     */
    TransactionFilterResultDTO filterByCustom(Transaction.TypeTransaction type, LocalDate date, LocalDate startDate, LocalDate endDate, int page, int size);
}

