package com.backend.gesy.facture;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LigneFactureRepository extends JpaRepository<LigneFacture, Long> {
    List<LigneFacture> findByFactureId(Long factureId);
}
