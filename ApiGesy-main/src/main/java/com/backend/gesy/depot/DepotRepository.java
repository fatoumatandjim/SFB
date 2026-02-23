package com.backend.gesy.depot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepotRepository extends JpaRepository<Depot, Long> {
    Optional<Depot> findByNom(String nom);
    List<Depot> findByStatut(Depot.StatutDepot statut);
}

