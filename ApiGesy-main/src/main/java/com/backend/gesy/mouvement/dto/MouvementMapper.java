package com.backend.gesy.mouvement.dto;

import com.backend.gesy.mouvement.Mouvement;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class MouvementMapper {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public MouvementDTO toDTO(Mouvement mouvement) {
        if (mouvement == null) {
            return null;
        }

        MouvementDTO dto = new MouvementDTO();
        dto.setId(mouvement.getId());
        dto.setStockId(mouvement.getStock() != null ? mouvement.getStock().getId() : null);
        dto.setTypeMouvement(mouvement.getTypeMouvement() != null ? mouvement.getTypeMouvement().name() : null);
        dto.setQuantite(mouvement.getQuantite());
        dto.setUnite(mouvement.getUnite());
        dto.setDescription(mouvement.getDescription());
        dto.setDateMouvement(mouvement.getDateMouvement() != null 
            ? mouvement.getDateMouvement().format(DATE_FORMATTER) : null);

        return dto;
    }

    public Mouvement toEntity(MouvementDTO dto) {
        if (dto == null) {
            return null;
        }

        Mouvement mouvement = new Mouvement();
        mouvement.setId(dto.getId());
        mouvement.setQuantite(dto.getQuantite());
        mouvement.setUnite(dto.getUnite());
        mouvement.setDescription(dto.getDescription());
        if (dto.getTypeMouvement() != null) {
            mouvement.setTypeMouvement(Mouvement.TypeMouvement.valueOf(dto.getTypeMouvement()));
        }
        // stock sera défini par le service

        return mouvement;
    }
}

