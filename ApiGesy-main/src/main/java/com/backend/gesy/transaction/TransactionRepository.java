package com.backend.gesy.transaction;

import com.backend.gesy.camion.Camion;
import com.backend.gesy.caisse.Caisse;
import com.backend.gesy.comptebancaire.CompteBancaire;
import com.backend.gesy.facture.Facture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCompte(CompteBancaire compte);
    List<Transaction> findByCamion(Camion camion);
    List<Transaction> findByFacture(Facture facture);
    List<Transaction> findByCaisse(Caisse caisse);
    
    @Query("SELECT t FROM Transaction t ORDER BY t.date DESC")
    List<Transaction> findRecentTransactions(Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE DATE(t.date) = DATE(:date) ORDER BY t.date DESC")
    Page<Transaction> findByDate(@Param("date") LocalDateTime date, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.date >= :startDate AND t.date <= :endDate ORDER BY t.date DESC")
    Page<Transaction> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t ORDER BY t.date DESC")
    Page<Transaction> findAllOrderedByDate(Pageable pageable);
    
    // Filtrer par compte bancaire avec pagination
    @Query("SELECT t FROM Transaction t WHERE t.compte.id = :compteId ORDER BY t.date DESC")
    Page<Transaction> findByCompteId(@Param("compteId") Long compteId, Pageable pageable);
    
    // Filtrer par caisse avec pagination
    @Query("SELECT t FROM Transaction t WHERE t.caisse.id = :caisseId ORDER BY t.date DESC")
    Page<Transaction> findByCaisseId(@Param("caisseId") Long caisseId, Pageable pageable);
    
    // Filtrer uniquement les transactions de comptes bancaires (pas de caisse) avec pagination
    @Query("SELECT t FROM Transaction t WHERE t.compte IS NOT NULL AND t.caisse IS NULL ORDER BY t.date DESC")
    Page<Transaction> findByComptesBancairesOnly(Pageable pageable);
    
    // Filtrer uniquement les transactions de caisses (pas de compte bancaire) avec pagination
    @Query("SELECT t FROM Transaction t WHERE t.caisse IS NOT NULL AND t.compte IS NULL ORDER BY t.date DESC")
    Page<Transaction> findByCaissesOnly(Pageable pageable);
    
    // Filtrer par compte bancaire avec date et pagination
    @Query("SELECT t FROM Transaction t WHERE t.compte.id = :compteId AND DATE(t.date) = DATE(:date) ORDER BY t.date DESC")
    Page<Transaction> findByCompteIdAndDate(@Param("compteId") Long compteId, @Param("date") LocalDateTime date, Pageable pageable);
    
    // Filtrer par caisse avec date et pagination
    @Query("SELECT t FROM Transaction t WHERE t.caisse.id = :caisseId AND DATE(t.date) = DATE(:date) ORDER BY t.date DESC")
    Page<Transaction> findByCaisseIdAndDate(@Param("caisseId") Long caisseId, @Param("date") LocalDateTime date, Pageable pageable);
    
    // Filtrer par compte bancaire avec plage de dates et pagination
    @Query("SELECT t FROM Transaction t WHERE t.compte.id = :compteId AND t.date >= :startDate AND t.date <= :endDate ORDER BY t.date DESC")
    Page<Transaction> findByCompteIdAndDateRange(@Param("compteId") Long compteId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);
    
    // Filtrer par caisse avec plage de dates et pagination
    @Query("SELECT t FROM Transaction t WHERE t.caisse.id = :caisseId AND t.date >= :startDate AND t.date <= :endDate ORDER BY t.date DESC")
    Page<Transaction> findByCaisseIdAndDateRange(@Param("caisseId") Long caisseId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);
    
    // Méthodes pour récupérer toutes les transactions (sans pagination) pour l'export
    @Query("SELECT t FROM Transaction t WHERE DATE(t.date) = DATE(:date) ORDER BY t.date DESC")
    List<Transaction> findByDateAll(@Param("date") LocalDateTime date);
    
    @Query("SELECT t FROM Transaction t WHERE t.date >= :startDate AND t.date <= :endDate ORDER BY t.date DESC")
    List<Transaction> findByDateRangeAll(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Récupérer les transactions de type FRAIS_DOUANE ou FRAIS_T1 dans un intervalle de dates
    // Note: Avec @Enumerated(EnumType.STRING), les valeurs sont stockées comme strings
    @Query("SELECT t FROM Transaction t WHERE (t.type = 'FRAIS_DOUANE' OR t.type = 'FRAIS_T1') AND t.date >= :startDate AND t.date <= :endDate ORDER BY t.date DESC")
    List<Transaction> findFraisDouaniersByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Récupérer uniquement les transactions FRAIS_DOUANE dans un intervalle de dates
    @Query("SELECT t FROM Transaction t WHERE t.type = 'FRAIS_DOUANE' AND t.date >= :startDate AND t.date <= :endDate AND t.voyage IS NOT NULL ORDER BY t.date DESC")
    List<Transaction> findFraisDouaneByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Compter le nombre de voyages distincts avec des transactions FRAIS_DOUANE dans un intervalle de dates
    @Query("SELECT COUNT(DISTINCT t.voyage.id) FROM Transaction t WHERE t.type = 'FRAIS_DOUANE' AND t.date >= :startDate AND t.date <= :endDate AND t.voyage IS NOT NULL")
    Long countDistinctVoyagesAvecFraisDouane(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Récupérer les transactions FRAIS_DOUANE ou FRAIS_T1 pour un voyage spécifique dans un intervalle de dates
    @Query("SELECT t FROM Transaction t WHERE t.voyage.id = :voyageId AND (t.type = 'FRAIS_DOUANE' OR t.type = 'FRAIS_T1') AND t.date >= :startDate AND t.date <= :endDate ORDER BY t.date DESC")
    List<Transaction> findFraisDouaniersByVoyageIdAndDateRange(@Param("voyageId") Long voyageId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /** Transaction(s) de type FRAIS pour un voyage (coût de transport créé à la création du voyage). */
    @Query("SELECT t FROM Transaction t WHERE t.voyage.id = :voyageId AND t.type = 'FRAIS'")
    List<Transaction> findByVoyageIdAndTypeFrais(@Param("voyageId") Long voyageId);

    // Filtre personnalisé: par type de transaction
    @Query("SELECT t FROM Transaction t WHERE t.type = :type ORDER BY t.date DESC")
    Page<Transaction> findByType(@Param("type") Transaction.TypeTransaction type, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.type = :type AND DATE(t.date) = DATE(:date) ORDER BY t.date DESC")
    Page<Transaction> findByTypeAndDate(@Param("type") Transaction.TypeTransaction type, @Param("date") LocalDateTime date, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.type = :type AND t.date >= :startDate AND t.date <= :endDate ORDER BY t.date DESC")
    Page<Transaction> findByTypeAndDateRange(@Param("type") Transaction.TypeTransaction type, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    // Somme des montants pour le filtre personnalisé
    @Query("SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t")
    java.math.BigDecimal sumMontantAll();

    @Query("SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t WHERE DATE(t.date) = DATE(:date)")
    java.math.BigDecimal sumMontantByDate(@Param("date") LocalDateTime date);

    @Query("SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t WHERE t.date >= :startDate AND t.date <= :endDate")
    java.math.BigDecimal sumMontantByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t WHERE t.type = :type")
    java.math.BigDecimal sumMontantByType(@Param("type") Transaction.TypeTransaction type);

    @Query("SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t WHERE t.type = :type AND DATE(t.date) = DATE(:date)")
    java.math.BigDecimal sumMontantByTypeAndDate(@Param("type") Transaction.TypeTransaction type, @Param("date") LocalDateTime date);

    @Query("SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t WHERE t.type = :type AND t.date >= :startDate AND t.date <= :endDate")
    java.math.BigDecimal sumMontantByTypeAndDateRange(@Param("type") Transaction.TypeTransaction type, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}

