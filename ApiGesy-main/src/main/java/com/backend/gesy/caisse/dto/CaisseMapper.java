package com.backend.gesy.caisse.dto;

import com.backend.gesy.caisse.Caisse;
import org.springframework.stereotype.Component;

@Component
public class CaisseMapper {
    public CaisseDTO toDTO(Caisse caisse) {
        if (caisse == null) {
            return null;
        }

        CaisseDTO dto = new CaisseDTO();
        dto.setId(caisse.getId());
        dto.setNom(caisse.getNom());
        dto.setSolde(caisse.getSolde());
        dto.setStatut(caisse.getStatut() != null ? caisse.getStatut().name() : null);
        dto.setDescription(caisse.getDescription());

        return dto;
    }

    public Caisse toEntity(CaisseDTO dto) {
        if (dto == null) {
            return null;
        }

        Caisse caisse = new Caisse();
        caisse.setId(dto.getId());
        caisse.setNom(dto.getNom());
        caisse.setSolde(dto.getSolde());
        if (dto.getStatut() != null) {
            caisse.setStatut(Caisse.StatutCaisse.valueOf(dto.getStatut()));
        }
        caisse.setDescription(dto.getDescription());

        return caisse;
    }
}

