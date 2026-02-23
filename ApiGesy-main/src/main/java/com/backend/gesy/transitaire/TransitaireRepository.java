package com.backend.gesy.transitaire;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransitaireRepository extends JpaRepository<Transitaire, Long> {
    Optional<Transitaire> findByIdentifiant(String identifiant);
    Optional<Transitaire> findByEmail(String email);
    
    // Récupérer tous les transitaires triés par date de création décroissante (pagination)
    @Query("SELECT t FROM Transitaire t ORDER BY t.createdAt DESC, t.id DESC")
    Page<Transitaire> findAllOrderByCreatedAtDesc(Pageable pageable);
}

