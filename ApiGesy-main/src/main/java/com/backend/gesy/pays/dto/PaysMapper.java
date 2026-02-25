package com.backend.gesy.pays.dto;

import com.backend.gesy.pays.Pays;
import org.springframework.stereotype.Component;

@Component
public class PaysMapper {

    public PaysDTO toDTO(Pays pays) {
        if (pays == null) return null;
        PaysDTO dto = new PaysDTO();
        dto.setId(pays.getId());
        dto.setNom(pays.getNom());
        dto.setFraisParLitre(pays.getFraisParLitre());
        dto.setFraisParLitreGasoil(pays.getFraisParLitreGasoil());
        dto.setFraisT1(pays.getFraisT1());
        return dto;
    }

    public Pays toEntity(PaysDTO dto) {
        if (dto == null) return null;
        Pays pays = new Pays();
        pays.setId(dto.getId());
        pays.setNom(dto.getNom());
        pays.setFraisParLitre(dto.getFraisParLitre());
        pays.setFraisParLitreGasoil(dto.getFraisParLitreGasoil());
        pays.setFraisT1(dto.getFraisT1());
        return pays;
    }
}
