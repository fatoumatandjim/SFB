package com.backend.gesy.caisse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CaisseRepository extends JpaRepository<Caisse, Long> {
    Optional<Caisse> findByNom(String nom);
    boolean existsByNom(String nom);
}

