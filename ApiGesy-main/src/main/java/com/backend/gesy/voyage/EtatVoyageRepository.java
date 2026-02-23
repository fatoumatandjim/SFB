package com.backend.gesy.voyage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EtatVoyageRepository extends JpaRepository<EtatVoyage, Long> {
    List<EtatVoyage> findByVoyageId(Long voyageId);
    List<EtatVoyage> findByVoyageAndEtat(Voyage voyage, String etat);
}

