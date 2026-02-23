package com.backend.gesy.camion;

import com.backend.gesy.fournisseur.Fournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CamionRepository extends JpaRepository<Camion, Long> {
    Optional<Camion> findByImmatriculation(String immatriculation);
    List<Camion> findByFournisseur(Fournisseur fournisseur);
}

