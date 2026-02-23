package com.backend.gesy.axe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AxeRepository extends JpaRepository<Axe, Long> {
    Optional<Axe> findByNom(String nom);
    boolean existsByNom(String nom);
}

