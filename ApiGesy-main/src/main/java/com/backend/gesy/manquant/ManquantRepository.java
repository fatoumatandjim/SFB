package com.backend.gesy.manquant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ManquantRepository extends JpaRepository<Manquant, Long> {
    
    // Liste paginée de tous les manquants triés par date (plus récent en premier)
    Page<Manquant> findAllByOrderByDateCreationDesc(Pageable pageable);
    
    // Liste paginée par date exacte
    @Query("SELECT m FROM Manquant m WHERE DATE(m.dateCreation) = DATE(:date) ORDER BY m.dateCreation DESC")
    Page<Manquant> findByDate(@Param("date") LocalDateTime date, Pageable pageable);
    
    // Liste paginée par intervalle de dates
    @Query("SELECT m FROM Manquant m WHERE m.dateCreation >= :startDate AND m.dateCreation <= :endDate ORDER BY m.dateCreation DESC")
    Page<Manquant> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);
    
    // Liste par voyage
    List<Manquant> findByVoyageIdOrderByDateCreationDesc(Long voyageId);
    
    // Trouver par voyage et client
    java.util.Optional<Manquant> findByVoyageIdAndClientId(Long voyageId, Long clientId);
    
    // Liste sans pagination
    List<Manquant> findAllByOrderByDateCreationDesc();
}

