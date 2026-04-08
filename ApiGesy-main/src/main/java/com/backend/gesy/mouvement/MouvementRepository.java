package com.backend.gesy.mouvement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MouvementRepository extends JpaRepository<Mouvement, Long> {
    List<Mouvement> findByStockId(Long stockId);

    List<Mouvement> findByDescriptionStartingWith(String prefix);

    boolean existsByDescription(String description);

    Optional<Mouvement> findFirstByDescriptionOrderByIdAsc(String description);

    List<Mouvement> findByDescriptionEndingWithAndTypeMouvement(String suffix, Mouvement.TypeMouvement typeMouvement);

    /**
     * Repère les annulations test « déchargement » sans dépendre du tiret (– vs -) ni de la casse.
     */
    @Query("SELECT m FROM Mouvement m WHERE " +
            "LOWER(COALESCE(m.description, '')) LIKE LOWER(CONCAT(CONCAT('%', :f1), '%')) AND " +
            "LOWER(COALESCE(m.description, '')) LIKE LOWER(CONCAT(CONCAT('%', :f2), '%')) AND " +
            "LOWER(COALESCE(m.description, '')) LIKE LOWER(CONCAT(CONCAT('%', :f3), '%'))")
    List<Mouvement> findMouvementsAnnulationTestRetourCiterne(
            @Param("f1") String fragmentAnnulation,
            @Param("f2") String fragmentRetourCiterne,
            @Param("f3") String fragmentVoyage);

    @Query("SELECT m FROM Mouvement m ORDER BY m.dateMouvement DESC")
    List<Mouvement> findRecentMouvements(Pageable pageable);
    
    @Query("SELECT m FROM Mouvement m WHERE m.dateMouvement >= :startDate AND m.dateMouvement <= :endDate ORDER BY m.dateMouvement DESC")
    Page<Mouvement> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);
    
    @Query("SELECT m FROM Mouvement m ORDER BY m.dateMouvement DESC")
    Page<Mouvement> findAllOrderedByDate(Pageable pageable);
}

