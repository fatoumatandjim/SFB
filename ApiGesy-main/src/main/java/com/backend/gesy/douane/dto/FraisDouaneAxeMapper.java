package com.backend.gesy.douane.dto;

import com.backend.gesy.douane.FraisDouaneAxe;
import org.springframework.stereotype.Component;

@Component
public class FraisDouaneAxeMapper {
    public FraisDouaneAxeDTO toDTO(FraisDouaneAxe entity) {
        if (entity == null) {
            return null;
        }
        FraisDouaneAxeDTO dto = new FraisDouaneAxeDTO();
        dto.setId(entity.getId());
        if (entity.getAxe() != null) {
            dto.setAxeId(entity.getAxe().getId());
            dto.setAxeNom(entity.getAxe().getNom());
        }
        dto.setFraisParLitre(entity.getFraisParLitre());
        dto.setFraisParLitreGasoil(entity.getFraisParLitreGasoil());
        dto.setFraisT1(entity.getFraisT1());
        return dto;
    }
}
