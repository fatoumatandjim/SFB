package com.backend.gesy.categoriedepense;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategorieDepenseRepository extends JpaRepository<CategorieDepense, Long> {
    List<CategorieDepense> findByStatut(CategorieDepense.StatutCategorie statut);
    Optional<CategorieDepense> findByNom(String nom);
    boolean existsByNom(String nom);
}

