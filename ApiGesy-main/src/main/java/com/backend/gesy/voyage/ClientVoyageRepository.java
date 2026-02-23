package com.backend.gesy.voyage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientVoyageRepository extends JpaRepository<ClientVoyage, Long> {
    List<ClientVoyage> findByVoyageId(Long voyageId);
    
    List<ClientVoyage> findByClientId(Long clientId);
    
    Optional<ClientVoyage> findByVoyageIdAndClientId(Long voyageId, Long clientId);
    
    List<ClientVoyage> findByVoyageIdAndStatut(Long voyageId, ClientVoyage.StatutLivraison statut);
    
    List<ClientVoyage> findByPrixAchatIsNull();
}
