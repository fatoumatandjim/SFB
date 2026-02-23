package com.backend.gesy.voyage.dto;

import com.backend.gesy.voyage.ClientVoyage;
import org.springframework.stereotype.Component;

@Component
public class ClientVoyageMapper {
    
    public ClientVoyageDTO toDTO(ClientVoyage clientVoyage) {
        if (clientVoyage == null) {
            return null;
        }
        
        ClientVoyageDTO dto = new ClientVoyageDTO();
        dto.setId(clientVoyage.getId());
        dto.setVoyageId(clientVoyage.getVoyage() != null ? clientVoyage.getVoyage().getId() : null);
        dto.setClientId(clientVoyage.getClient() != null ? clientVoyage.getClient().getId() : null);
        dto.setClientNom(clientVoyage.getClient() != null ? clientVoyage.getClient().getNom() : null);
        dto.setClientEmail(clientVoyage.getClient() != null ? clientVoyage.getClient().getEmail() : null);
        dto.setQuantite(clientVoyage.getQuantite());
        dto.setPrixAchat(clientVoyage.getPrixAchat());
        dto.setStatut(clientVoyage.getStatut() != null ? clientVoyage.getStatut().name() : null);
        dto.setManquant(clientVoyage.getManquant());
        dto.setDateCreation(clientVoyage.getDateCreation());
        dto.setDateModification(clientVoyage.getDateModification());
        
        return dto;
    }
}
