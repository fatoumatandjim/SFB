package com.backend.gesy.fournisseur;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {
    Optional<Fournisseur> findByEmail(String email);
    Optional<Fournisseur> findByCodeFournisseur(String codeFournisseur);
    List<Fournisseur> findByTypeFournisseur(Fournisseur.TypeFournisseur typeFournisseur);
}

