package com.backend.gesy.axe.dto;

import com.backend.gesy.axe.Axe;
import org.springframework.stereotype.Component;

@Component
public class AxeMapper {

    public AxeDTO toDTO(Axe axe) {
        if (axe == null) return null;
        AxeDTO dto = new AxeDTO();
        dto.setId(axe.getId());
        dto.setNom(axe.getNom());
        if (axe.getPays() != null) {
            dto.setPaysId(axe.getPays().getId());
            dto.setPaysNom(axe.getPays().getNom());
        }
        return dto;
    }

    public Axe toEntity(AxeDTO dto) {
        if (dto == null) return null;
        Axe axe = new Axe();
        axe.setId(dto.getId());
        axe.setNom(dto.getNom());
        return axe;
    }
}
