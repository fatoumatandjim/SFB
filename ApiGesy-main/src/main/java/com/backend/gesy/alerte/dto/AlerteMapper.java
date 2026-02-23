package com.backend.gesy.alerte.dto;

import com.backend.gesy.alerte.Alerte;
import org.springframework.stereotype.Component;

@Component
public class AlerteMapper {
    public AlerteDTO toDTO(Alerte alerte) {
        if (alerte == null) {
            return null;
        }
        
        AlerteDTO dto = new AlerteDTO();
        dto.setId(alerte.getId());
        dto.setType(alerte.getType() != null ? alerte.getType().name() : null);
        dto.setMessage(alerte.getMessage());
        dto.setDate(alerte.getDate());
        dto.setLu(alerte.getLu());
        dto.setPriorite(alerte.getPriorite() != null ? alerte.getPriorite().name() : null);
        dto.setLien(alerte.getLien());
        dto.setEntiteType(alerte.getEntiteType());
        dto.setEntiteId(alerte.getEntiteId());
        
        return dto;
    }

    public Alerte toEntity(AlerteDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Alerte alerte = new Alerte();
        alerte.setId(dto.getId());
        if (dto.getType() != null) {
            alerte.setType(Alerte.TypeAlerte.valueOf(dto.getType()));
        }
        alerte.setMessage(dto.getMessage());
        alerte.setDate(dto.getDate());
        alerte.setLu(dto.getLu());
        if (dto.getPriorite() != null) {
            alerte.setPriorite(Alerte.PrioriteAlerte.valueOf(dto.getPriorite()));
        }
        alerte.setLien(dto.getLien());
        alerte.setEntiteType(dto.getEntiteType());
        alerte.setEntiteId(dto.getEntiteId());
        
        return alerte;
    }
}

