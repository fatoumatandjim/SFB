package com.backend.gesy.facture;

import com.backend.gesy.facture.dto.CreanceDTO;
import com.backend.gesy.facture.dto.FactureDTO;
import com.backend.gesy.facture.dto.FacturePageDto;
import com.backend.gesy.facture.dto.FactureStatsDTO;
import com.backend.gesy.facture.dto.RecouvrementStatsDTO;

import com.backend.gesy.client.Client;
import com.backend.gesy.voyage.Voyage;

import java.math.BigDecimal;
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

    /**
     * Facture client (Facturation) : montant = tarif convenu au litre × litres ; utilisé uniquement pour les cessions,
     * en complément des frais douane / T1 calculés comme hors cession. Sans effet si une facture avec le marqueur
     * dédié existe déjà pour ce voyage.
     */
    void createFactureAutoCessionDroitDouaneIfAbsent(Voyage voyage, Client client, BigDecimal montantTTC,
            BigDecimal tarifParLitre, double litres);
}

