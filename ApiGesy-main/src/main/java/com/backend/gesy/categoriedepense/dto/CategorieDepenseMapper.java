package com.backend.gesy.categoriedepense.dto;

import com.backend.gesy.categoriedepense.CategorieDepense;
import org.springframework.stereotype.Component;

@Component
public class CategorieDepenseMapper {

    public CategorieDepenseDTO toDTO(CategorieDepense entity) {
        if (entity == null) return null;
        
        CategorieDepenseDTO dto = new CategorieDepenseDTO();
        dto.setId(entity.getId());
        dto.setNom(entity.getNom());
        dto.setDescription(entity.getDescription());
        dto.setStatut(entity.getStatut() != null ? entity.getStatut().name() : null);
        return dto;
    }

    public CategorieDepense toEntity(CategorieDepenseDTO dto) {
        if (dto == null) return null;
        
        CategorieDepense entity = new CategorieDepense();
        entity.setId(dto.getId());
        entity.setNom(dto.getNom());
        entity.setDescription(dto.getDescription());
        if (dto.getStatut() != null) {
            entity.setStatut(CategorieDepense.StatutCategorie.valueOf(dto.getStatut()));
        }
        return entity;
    }
}

