package com.backend.gesy.compte;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompteRepository extends JpaRepository<Compte, Long> {
    Optional<Compte> findByIdentifiant(String identifiant);
    boolean existsByIdentifiant(String identifiant);
}
