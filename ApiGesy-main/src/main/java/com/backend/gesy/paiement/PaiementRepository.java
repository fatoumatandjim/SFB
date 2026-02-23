package com.backend.gesy.paiement;

import com.backend.gesy.facture.Facture;
import com.backend.gesy.voyage.Voyage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    List<Paiement> findByFacture(Facture facture);
    Paiement findByVoyage(Voyage voyage);

    /** Paiement du coût de transport pour un voyage (référence PAY-COU-VOY-xxx). */
    @Query("SELECT p FROM Paiement p WHERE p.voyage.id = :voyageId AND p.reference LIKE 'PAY-COU-VOY-%'")
    List<Paiement> findCoutTransportByVoyageId(@Param("voyageId") Long voyageId);
}

