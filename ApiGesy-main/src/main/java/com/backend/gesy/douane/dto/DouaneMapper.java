package com.backend.gesy.douane.dto;

import com.backend.gesy.douane.Douane;
import org.springframework.stereotype.Component;

@Component
public class DouaneMapper {
    public DouaneDTO toDTO(Douane douane) {
        if (douane == null) {
            return null;
        }
        DouaneDTO dto = new DouaneDTO();
        dto.setId(douane.getId());
        dto.setFraisParLitre(douane.getFraisParLitre());
        dto.setFraisParLitreGasoil(douane.getFraisParLitreGasoil());
        dto.setFraisT1(douane.getFraisT1());
        return dto;
    }

    public Douane toEntity(DouaneDTO dto) {
        if (dto == null) {
            return null;
        }
        Douane douane = new Douane();
        douane.setId(dto.getId());
        douane.setFraisParLitre(dto.getFraisParLitre());
        douane.setFraisParLitreGasoil(dto.getFraisParLitreGasoil());
        douane.setFraisT1(dto.getFraisT1());
        return douane;
    }
}

