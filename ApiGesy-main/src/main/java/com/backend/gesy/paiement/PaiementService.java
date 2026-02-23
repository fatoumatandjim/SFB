package com.backend.gesy.paiement;

import com.backend.gesy.paiement.dto.PaiementDTO;

import java.util.List;
import java.util.Optional;

public interface PaiementService {
    List<PaiementDTO> findAll();
    Optional<PaiementDTO> findById(Long id);
    List<PaiementDTO> findByFactureId(Long factureId);
    List<PaiementDTO> findByStatut(Paiement.StatutPaiement statut);
    PaiementDTO save(PaiementDTO paiementDTO);
    PaiementDTO update(Long id, PaiementDTO paiementDTO);
    PaiementDTO validerPaiement(Long paiementId, Long compteId, Long caisseId);
    void deleteById(Long id);
}

