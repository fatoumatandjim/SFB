package com.backend.gesy.achat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.backend.gesy.facture.Facture;
import com.backend.gesy.transaction.Transaction;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AchatRepository extends JpaRepository<Achat, Long> {
    List<Achat> findByDepotId(Long depotId);
    List<Achat> findByProduitId(Long produitId);
    
    // Pagination pour tous les achats
    Page<Achat> findAll(Pageable pageable);

    /** Achats hors cession (liste « Tous » : tri par plus récent) */
    Page<Achat> findByCessionFalse(Pageable pageable);

    @Query("SELECT a FROM Achat a WHERE a.cession = false AND DATE(a.dateAchat) = DATE(:date)")
    Page<Achat> findByCessionFalseAndDateAchat(@Param("date") LocalDateTime date, Pageable pageable);

    @Query("SELECT a FROM Achat a WHERE a.cession = false AND a.dateAchat >= :startDate AND a.dateAchat <= :endDate")
    Page<Achat> findByCessionFalseAndDateAchatBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    // Pagination avec filtre par date
    @Query("SELECT a FROM Achat a WHERE DATE(a.dateAchat) = DATE(:date)")
    Page<Achat> findByDateAchat(@Param("date") LocalDateTime date, Pageable pageable);
    
    // Pagination avec filtre par intervalle de dates
    @Query("SELECT a FROM Achat a WHERE a.dateAchat >= :startDate AND a.dateAchat <= :endDate")
    Page<Achat> findByDateAchatBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    // Pagination avec filtre par dépôt
    Page<Achat> findByDepotId(Long depotId, Pageable pageable);
    
    // Pagination avec filtre par dépôt et date
    @Query("SELECT a FROM Achat a WHERE a.depot.id = :depotId AND DATE(a.dateAchat) = DATE(:date)")
    Page<Achat> findByDepotIdAndDateAchat(
        @Param("depotId") Long depotId,
        @Param("date") LocalDateTime date,
        Pageable pageable
    );
    
    // Pagination avec filtre par dépôt et intervalle de dates
    @Query("SELECT a FROM Achat a WHERE a.depot.id = :depotId AND a.dateAchat >= :startDate AND a.dateAchat <= :endDate")
    Page<Achat> findByDepotIdAndDateAchatBetween(
        @Param("depotId") Long depotId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    // Pagination avec filtre par statut de transaction (paiement)
    @Query("SELECT a FROM Achat a WHERE a.transaction.statut = :statut")
    Page<Achat> findByTransactionStatut(@Param("statut") Transaction.StatutTransaction statut, Pageable pageable);
    
    // Pagination avec filtre par statut de transaction et date
    @Query("SELECT a FROM Achat a WHERE a.transaction.statut = :statut AND DATE(a.dateAchat) = DATE(:date)")
    Page<Achat> findByTransactionStatutAndDateAchat(
        @Param("statut") Transaction.StatutTransaction statut,
        @Param("date") LocalDateTime date,
        Pageable pageable
    );
    
    // Pagination avec filtre par statut de transaction et intervalle de dates
    @Query("SELECT a FROM Achat a WHERE a.transaction.statut = :statut AND a.dateAchat >= :startDate AND a.dateAchat <= :endDate")
    Page<Achat> findByTransactionStatutAndDateAchatBetween(
        @Param("statut") Transaction.StatutTransaction statut,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    // Méthodes de compatibilité avec l'ancien système (facture) - à supprimer progressivement
    @Query("SELECT a FROM Achat a WHERE a.facture.statut = :statut")
    Page<Achat> findByFactureStatut(@Param("statut") Facture.StatutFacture statut, Pageable pageable);
    
    @Query("SELECT a FROM Achat a WHERE a.facture.statut = :statut AND DATE(a.dateAchat) = DATE(:date)")
    Page<Achat> findByFactureStatutAndDateAchat(
        @Param("statut") Facture.StatutFacture statut,
        @Param("date") LocalDateTime date,
        Pageable pageable
    );
    
    @Query("SELECT a FROM Achat a WHERE a.facture.statut = :statut AND a.dateAchat >= :startDate AND a.dateAchat <= :endDate")
    Page<Achat> findByFactureStatutAndDateAchatBetween(
        @Param("statut") Facture.StatutFacture statut,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    // Achats de cession (sans transaction)
    @Query("SELECT a FROM Achat a WHERE a.cession = true ORDER BY a.dateAchat DESC, a.id DESC")
    Page<Achat> findByCessionTrue(Pageable pageable);

    @Query("SELECT a FROM Achat a WHERE a.cession = true AND DATE(a.dateAchat) = DATE(:date) ORDER BY a.dateAchat DESC, a.id DESC")
    Page<Achat> findByCessionTrueAndDateAchat(@Param("date") LocalDateTime date, Pageable pageable);

    @Query("SELECT a FROM Achat a WHERE a.cession = true AND a.dateAchat >= :startDate AND a.dateAchat <= :endDate ORDER BY a.dateAchat DESC, a.id DESC")
    Page<Achat> findByCessionTrueAndDateAchatBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
}

