package com.backend.gesy.achat;

import com.backend.gesy.achat.dto.AchatDTO;
import com.backend.gesy.achat.dto.AchatPageDTO;

import java.time.LocalDate;
import java.util.List;

public interface AchatService {
    List<AchatDTO> findAll();
    AchatDTO findById(Long id);
    List<AchatDTO> findByDepotId(Long depotId);
    List<AchatDTO> findByProduitId(Long produitId);
    AchatDTO save(AchatDTO achatDTO);
    void deleteById(Long id);
    
    // Pagination
    AchatPageDTO findAllPaginated(int page, int size);
    AchatPageDTO findByDate(LocalDate date, int page, int size);
    AchatPageDTO findByDateRange(LocalDate startDate, LocalDate endDate, int page, int size);
    AchatPageDTO findByDepotIdPaginated(Long depotId, int page, int size);
    AchatPageDTO findByDepotIdAndDate(Long depotId, LocalDate date, int page, int size);
    AchatPageDTO findByDepotIdAndDateRange(Long depotId, LocalDate startDate, LocalDate endDate, int page, int size);
    
    // Calcul de marge
    com.backend.gesy.achat.dto.AchatMargeDTO calculateMarge(Long achatId);
    
    // Créer un achat avec facture impayée (sans approvisionner)
    AchatDTO createAchatWithFacture(com.backend.gesy.achat.dto.CreateAchatWithFactureDTO dto);
    
    // Payer un achat (valider facture, débiter compte, créer transaction, approvisionner)
    AchatDTO payerAchat(com.backend.gesy.achat.dto.PayerAchatDTO dto);
    
    // Filtrer par statut de paiement
    AchatPageDTO findByStatutFacture(String statut, int page, int size);
    AchatPageDTO findByStatutFactureAndDate(String statut, LocalDate date, int page, int size);
    AchatPageDTO findByStatutFactureAndDateRange(String statut, LocalDate startDate, LocalDate endDate, int page, int size);

    // Achats de cession (sans transaction, stock en quantityCession)
    AchatDTO createAchatCession(com.backend.gesy.achat.dto.CreateAchatCessionDTO dto);
    AchatPageDTO findCessionPaginated(int page, int size);
    AchatPageDTO findCessionByDate(LocalDate date, int page, int size);
    AchatPageDTO findCessionByDateRange(LocalDate startDate, LocalDate endDate, int page, int size);
}

