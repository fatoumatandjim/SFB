package com.backend.gesy.depense;

import com.backend.gesy.categoriedepense.CategorieDepense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DepenseRepository extends JpaRepository<Depense, Long> {
    
    // Liste paginée de toutes les dépenses
    Page<Depense> findAllByOrderByDateDepenseDesc(Pageable pageable);
    
    // Liste paginée par catégorie
    Page<Depense> findByCategorieOrderByDateDepenseDesc(CategorieDepense categorie, Pageable pageable);
    
    // Liste paginée par date exacte
    @Query("SELECT d FROM Depense d WHERE DATE(d.dateDepense) = DATE(:date) ORDER BY d.dateDepense DESC")
    Page<Depense> findByDate(@Param("date") LocalDateTime date, Pageable pageable);
    
    // Liste paginée par intervalle de dates
    @Query("SELECT d FROM Depense d WHERE d.dateDepense >= :startDate AND d.dateDepense <= :endDate ORDER BY d.dateDepense DESC")
    Page<Depense> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);
    
    // Liste paginée par catégorie et date exacte
    @Query("SELECT d FROM Depense d WHERE d.categorie = :categorie AND DATE(d.dateDepense) = DATE(:date) ORDER BY d.dateDepense DESC")
    Page<Depense> findByCategorieAndDate(@Param("categorie") CategorieDepense categorie, @Param("date") LocalDateTime date, Pageable pageable);
    
    // Liste paginée par catégorie et intervalle de dates
    @Query("SELECT d FROM Depense d WHERE d.categorie = :categorie AND d.dateDepense >= :startDate AND d.dateDepense <= :endDate ORDER BY d.dateDepense DESC")
    Page<Depense> findByCategorieAndDateRange(@Param("categorie") CategorieDepense categorie, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);
    
    // Somme des dépenses par catégorie
    @Query("SELECT SUM(d.montant) FROM Depense d WHERE d.categorie = :categorie")
    java.math.BigDecimal sumByCategorie(@Param("categorie") CategorieDepense categorie);
    
    // Somme des dépenses par intervalle de dates
    @Query("SELECT SUM(d.montant) FROM Depense d WHERE d.dateDepense >= :startDate AND d.dateDepense <= :endDate")
    java.math.BigDecimal sumByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Liste sans pagination
    List<Depense> findByOrderByDateDepenseDesc();
    
    // Liste par catégorie sans pagination
    List<Depense> findByCategorieOrderByDateDepenseDesc(CategorieDepense categorie);
    
    // Liste par catégorie et intervalle de dates sans pagination
    @Query("SELECT d FROM Depense d WHERE d.categorie = :categorie AND d.dateDepense >= :startDate AND d.dateDepense <= :endDate ORDER BY d.dateDepense DESC")
    List<Depense> findByCategorieAndDateRangeList(@Param("categorie") CategorieDepense categorie, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}

