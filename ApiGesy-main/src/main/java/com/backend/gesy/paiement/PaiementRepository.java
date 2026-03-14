package com.backend.gesy.paiement;

import com.backend.gesy.categoriedepense.CategorieDepense;
import com.backend.gesy.facture.Facture;
import com.backend.gesy.voyage.Voyage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    List<Paiement> findByFacture(Facture facture);
    /**
     * Attention: un voyage peut avoir plusieurs paiements.
     * Utiliser de préférence {@link #findCoutTransportByVoyageId(Long)} pour le coût transport,
     * ou déclarer une méthode retournant une liste.
     */
    List<Paiement> findByVoyage(Voyage voyage);

    /** Paiement du coût de transport pour un voyage (référence PAY-COU-VOY-xxx). */
    @Query("SELECT p FROM Paiement p WHERE p.voyage.id = :voyageId AND p.reference LIKE 'PAY-COU-VOY-%'")
    List<Paiement> findCoutTransportByVoyageId(@Param("voyageId") Long voyageId);

    /** Paiements contenant une transaction donnée (pour libérer la FK avant suppression du voyage). */
    @Query("SELECT p FROM Paiement p JOIN p.transactions t WHERE t.id = :transactionId")
    List<Paiement> findByTransactionId(@Param("transactionId") Long transactionId);

    /** Paiements liés à une catégorie de dépense (transport, T1, douane). */
    List<Paiement> findByCategorieDepenseOrderByDateDesc(CategorieDepense categorieDepense);

    @Query("SELECT p FROM Paiement p WHERE p.categorieDepense.id = :categorieId ORDER BY p.date DESC")
    List<Paiement> findByCategorieDepenseIdOrderByDateDesc(@Param("categorieId") Long categorieId);

    @Query("SELECT p FROM Paiement p WHERE p.categorieDepense.id = :categorieId AND p.date >= :start AND p.date <= :end ORDER BY p.date DESC")
    List<Paiement> findByCategorieDepenseIdAndDateBetweenOrderByDateDesc(
            @Param("categorieId") Long categorieId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    /** Tous les paiements liés à une catégorie (pour menu Dépenses unifié). */
    List<Paiement> findByCategorieDepenseIsNotNullOrderByDateDesc();

    @Query("SELECT p FROM Paiement p WHERE p.categorieDepense IS NOT NULL AND p.date >= :start AND p.date <= :end ORDER BY p.date DESC")
    List<Paiement> findByCategorieDepenseIsNotNullAndDateBetweenOrderByDateDesc(@Param("start") LocalDate start, @Param("end") LocalDate end);
}

