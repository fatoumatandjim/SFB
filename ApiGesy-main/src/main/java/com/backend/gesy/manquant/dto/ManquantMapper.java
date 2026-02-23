package com.backend.gesy.manquant.dto;

import com.backend.gesy.manquant.Manquant;
import org.springframework.stereotype.Component;

@Component
public class ManquantMapper {

    public ManquantDTO toDTO(Manquant entity) {
        if (entity == null) return null;
        
        ManquantDTO dto = new ManquantDTO();
        dto.setId(entity.getId());
        dto.setQuantite(entity.getQuantite());
        dto.setDateCreation(entity.getDateCreation());
        dto.setDescription(entity.getDescription());
        dto.setCreePar(entity.getCreePar());
        
        if (entity.getVoyage() != null) {
            dto.setVoyageId(entity.getVoyage().getId());
            dto.setNumeroVoyage(entity.getVoyage().getNumeroVoyage());
            
            if (entity.getVoyage().getCamion() != null) {
                dto.setCamionImmatriculation(entity.getVoyage().getCamion().getImmatriculation());
            }
            
            if (entity.getVoyage().getProduit() != null) {
                dto.setProduitNom(entity.getVoyage().getProduit().getNom());
            }
            
            if (entity.getVoyage().getDepot() != null) {
                dto.setDepotNom(entity.getVoyage().getDepot().getNom());
            }
        }
        
        return dto;
    }

    public Manquant toEntity(ManquantDTO dto) {
        if (dto == null) return null;
        
        Manquant entity = new Manquant();
        entity.setId(dto.getId());
        entity.setQuantite(dto.getQuantite());
        entity.setDateCreation(dto.getDateCreation());
        entity.setDescription(dto.getDescription());
        entity.setCreePar(dto.getCreePar());
        
        // Le voyage sera défini dans le service
        
        return entity;
    }
}

