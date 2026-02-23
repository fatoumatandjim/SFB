package com.backend.gesy.comptebancaire;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompteBancaireRepository extends JpaRepository<CompteBancaire, Long> {
    Optional<CompteBancaire> findByNumero(String numero);
}

