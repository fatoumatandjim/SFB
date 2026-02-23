package com.backend.gesy.axe.dto;

import com.backend.gesy.axe.Axe;
import org.springframework.stereotype.Component;

@Component
public class AxeMapper {
    
    public AxeDTO toDTO(Axe axe) {
        if (axe == null) {
            return null;
        }
        return new AxeDTO(axe.getId(), axe.getNom());
    }
    
    public Axe toEntity(AxeDTO dto) {
        if (dto == null) {
            return null;
        }
        Axe axe = new Axe();
        axe.setId(dto.getId());
        axe.setNom(dto.getNom());
        return axe;
    }
}

