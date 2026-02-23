package com.backend.gesy.mouvement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MouvementRepository extends JpaRepository<Mouvement, Long> {
    List<Mouvement> findByStockId(Long stockId);
    
    @Query("SELECT m FROM Mouvement m ORDER BY m.dateMouvement DESC")
    List<Mouvement> findRecentMouvements(Pageable pageable);
    
    @Query("SELECT m FROM Mouvement m WHERE m.dateMouvement >= :startDate AND m.dateMouvement <= :endDate ORDER BY m.dateMouvement DESC")
    Page<Mouvement> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);
    
    @Query("SELECT m FROM Mouvement m ORDER BY m.dateMouvement DESC")
    Page<Mouvement> findAllOrderedByDate(Pageable pageable);
}

