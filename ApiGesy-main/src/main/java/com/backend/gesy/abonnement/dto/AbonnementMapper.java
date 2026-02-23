package com.backend.gesy.abonnement.dto;

import com.backend.gesy.abonnement.Abonnement;
import org.springframework.stereotype.Component;

@Component
public class AbonnementMapper {
    public AbonnementDTO toDTO(Abonnement abonnement) {
        if (abonnement == null) {
            return null;
        }
        
        AbonnementDTO dto = new AbonnementDTO();
        dto.setId(abonnement.getId());
        dto.setType(abonnement.getType() != null ? abonnement.getType().name() : null);
        dto.setDateDebut(abonnement.getDateDebut());
        dto.setDateFin(abonnement.getDateFin());
        dto.setActif(abonnement.getActif());
        dto.setPrix(abonnement.getPrix());
        dto.setDescription(abonnement.getDescription());
        
        return dto;
    }

    public Abonnement toEntity(AbonnementDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Abonnement abonnement = new Abonnement();
        abonnement.setId(dto.getId());
        if (dto.getType() != null) {
            abonnement.setType(Abonnement.TypeAbonnement.valueOf(dto.getType()));
        }
        abonnement.setDateDebut(dto.getDateDebut());
        abonnement.setDateFin(dto.getDateFin());
        abonnement.setActif(dto.getActif());
        abonnement.setPrix(dto.getPrix());
        abonnement.setDescription(dto.getDescription());
        
        return abonnement;
    }
}

