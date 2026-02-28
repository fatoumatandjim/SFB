package com.backend.gesy.voyage;

import com.backend.gesy.voyage.dto.TransitaireStatsDTO;
import com.backend.gesy.voyage.dto.VoyageDTO;
import com.backend.gesy.voyage.dto.VoyagePageDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VoyageService {
        List<VoyageDTO> findAll();

        Optional<VoyageDTO> findById(Long id);

        Optional<VoyageDTO> findByNumeroVoyage(String numeroVoyage);

        List<VoyageDTO> findByCamionId(Long camionId);

        List<VoyageDTO> findByClientId(Long clientId);

        List<VoyageDTO> findByTransitaireId(Long transitaireId);

        List<VoyageDTO> findVoyagesNonDeclaresByTransitaireId(Long transitaireId);

        List<VoyageDTO> findVoyagesNonDeclaresByTransitaireIdentifiant(String identifiant);

        TransitaireStatsDTO getTransitaireStatsByIdentifiant(String identifiant);

        List<VoyageDTO> findByDepotId(Long depotId);

        List<VoyageDTO> findByDepotIdAndStatutsChargement(Long depotId);

        List<VoyageDTO> findByAxeId(Long axeId);

        VoyagePageDto findByAxeIdPaginated(Long axeId, int page, int size);

        List<VoyageDTO> findByUtilisateurIdentifiant(String identifiant);

        List<VoyageDTO> updateStatutMultiple(List<Long> voyageIds, String statut);

        Long countCamionsChargesByDepotId(Long depotId);

        VoyageDTO save(VoyageDTO voyageDTO);

        VoyageDTO update(Long id, VoyageDTO voyageDTO);

        VoyageDTO updateStatut(Long id, String statut, Long clientId, Double manquant, java.math.BigDecimal prixAchat);
        
        VoyageDTO updateStatut(Long id, com.backend.gesy.voyage.dto.UpdateStatutRequestDTO request);

        VoyageDTO donnerPrixAchat(Long voyageId, Long clientVoyageId, java.math.BigDecimal prixAchat);

        VoyageDTO updateClientVoyageQuantite(Long voyageId, Long clientVoyageId, Long newClientId, Double quantite);

        VoyageDTO assignTransitaire(Long voyageId, Long transitaireId);

        VoyageDTO declarerVoyage(Long voyageId, Long compteId, Long caisseId);

        List<VoyageDTO> declarerVoyagesMultiple(List<Long> voyageIds, Long compteId, Long caisseId);

        VoyageDTO passerNonDeclarer(Long voyageId);

        /** Marquer un voyage comme libéré */
        VoyageDTO libererVoyage(Long voyageId);

        /** Marquer plusieurs voyages comme libérés */
        List<VoyageDTO> libererVoyages(List<Long> voyageIds);

        TransitaireStatsDTO getTransitaireStats(Long transitaireId);

        VoyagePageDto findArchivedVoyagesByTransitaire(Long transitaireId, int page, int size);

        VoyagePageDto findArchivedVoyagesByTransitaireAndDate(Long transitaireId, LocalDate date, int page, int size);

        VoyagePageDto findArchivedVoyagesByTransitaireAndDateRange(Long transitaireId, LocalDate startDate,
                        LocalDate endDate, int page, int size);

        VoyagePageDto findArchivedVoyagesByTransitaireIdentifiant(String identifiant, int page, int size);

        /** Voyages en cours du transitaire (non déchargés), paginés, par identifiant */
        VoyagePageDto findVoyagesEnCoursByTransitaireIdentifiant(String identifiant, int page, int size);

        VoyagePageDto findArchivedVoyagesByTransitaireIdentifiantAndDate(String identifiant, LocalDate date, int page,
                        int size);

        VoyagePageDto findArchivedVoyagesByTransitaireIdentifiantAndDateRange(String identifiant, LocalDate startDate,
                        LocalDate endDate, int page, int size);

        VoyagePageDto findArchivedVoyages(int page, int size);

        VoyagePageDto findArchivedVoyagesByDate(LocalDate date, int page, int size);

        VoyagePageDto findArchivedVoyagesByDateRange(LocalDate startDate, LocalDate endDate, int page, int size);

        com.backend.gesy.voyage.dto.VoyageMargeDTO calculateMarge(Long voyageId);

        VoyageDTO assignerNumeroBonEnlevement(Long voyageId, String numeroBonEnlevement);

        VoyageDTO genererNumeroBonEnlevement(Long voyageId);

        com.backend.gesy.voyage.dto.CoutTransportResponseDTO getCoutsTransport(
                        Long fournisseurId,
                        String filterOption,
                        java.time.LocalDate startDate,
                        java.time.LocalDate endDate);

        VoyagePageDto findVoyagesChargesByIdentifiant(
                        String identifiant,
                        int page,
                        int size,
                        java.time.LocalDate date,
                        java.time.LocalDate startDate,
                        java.time.LocalDate endDate);

        void deleteById(Long id);

        // Voyages passés non déclarés
        List<VoyageDTO> findVoyagesPassesNonDeclares();

        VoyagePageDto findVoyagesPassesNonDeclaresPaginated(int page, int size);

        // Voyages avec client mais sans facture (sans prix d'achat)
        VoyagePageDto findVoyagesAvecClientSansFacture(int page, int size);

        com.backend.gesy.voyage.dto.VoyagesParClientPageDto findVoyagesAvecClientSansFactureGroupesParClient(int page,
                        int size);
        
        // Voyages partiellement déchargés
        VoyagePageDto findVoyagesPartiellementDecharges(int page, int size);
        
        // Voyages en cours (non déchargés)
        VoyagePageDto findVoyagesEnCours(int page, int size);

        /** Voyages en cours (non déchargés) avec au moins un client assigné (pour rapport PDF camions/clients) */
        List<VoyageDTO> findVoyagesEnCoursAvecClients();

        /** Voyages attribués (non cession) sans prix de transport — pour le comptable */
        VoyagePageDto findVoyagesSansPrixTransport(int page, int size);
}
