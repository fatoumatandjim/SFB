package com.backend.gesy.facture;

import com.backend.gesy.facture.dto.CreanceDTO;
import com.backend.gesy.facture.dto.FactureDTO;
import com.backend.gesy.facture.dto.FacturePageDto;
import com.backend.gesy.facture.dto.FactureStatsDTO;
import com.backend.gesy.facture.dto.RecouvrementStatsDTO;

import java.util.List;
import java.util.Optional;

public interface FactureService {
    List<FactureDTO> findAll();
    FacturePageDto findAllPaginated(int page, int size);
    Optional<FactureDTO> findById(Long id);
    Optional<FactureDTO> findByNumero(String numero);
    List<FactureDTO> findByClientId(Long clientId);
    FactureDTO save(FactureDTO factureDTO);
    FactureDTO update(Long id, FactureDTO factureDTO);
    FactureDTO updateStatut(Long id, String statut);
    FactureStatsDTO getStats();
    void deleteById(Long id);
    
    // Nouvelles méthodes pour le recouvrement
    List<CreanceDTO> getUnpaidFactures();
    RecouvrementStatsDTO getRecouvrementStats();
    
    byte[] generateFacturesPdf(Long clientId);
}

