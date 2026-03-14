package com.backend.gesy.paiement;

import com.backend.gesy.paiement.dto.PaiementDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PaiementService {
    List<PaiementDTO> findAll();
    Optional<PaiementDTO> findById(Long id);
    List<PaiementDTO> findByFactureId(Long factureId);
    List<PaiementDTO> findByStatut(Paiement.StatutPaiement statut);
    /** Paiements ayant une catégorie de dépense (pour menu Dépenses). */
    List<PaiementDTO> findByCategorieId(Long categorieId);
    List<PaiementDTO> findByCategorieIdAndDateRange(Long categorieId, LocalDate startDate, LocalDate endDate);
    /** Tous les paiements qui ont une catégorie (transport, T1, douane). startDate/endDate optionnels. */
    List<PaiementDTO> findAllWithCategorie(LocalDate startDate, LocalDate endDate);
    PaiementDTO save(PaiementDTO paiementDTO);
    PaiementDTO update(Long id, PaiementDTO paiementDTO);
    PaiementDTO validerPaiement(Long paiementId, Long compteId, Long caisseId);
    void deleteById(Long id);
}

