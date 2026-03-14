package com.backend.gesy.categoriedepense.dto;

import com.backend.gesy.categoriedepense.CategorieDepense;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CategorieDepenseMapper {

    public CategorieDepenseDTO toDTO(CategorieDepense entity) {
        if (entity == null) return null;
        
        CategorieDepenseDTO dto = new CategorieDepenseDTO();
        dto.setId(entity.getId());
        dto.setNom(entity.getNom());
        dto.setDescription(entity.getDescription());
        dto.setStatut(entity.getStatut() != null ? entity.getStatut().name() : null);
        dto.setTarifsTransport(parseTarifsTransport(entity.getTarifsTransport()));
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
        entity.setTarifsTransport(tarifsTransportToString(dto.getTarifsTransport()));
        return entity;
    }

    private static List<Integer> parseTarifsTransport(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
                })
                .filter(n -> n != null && n > 0)
                .sorted()
                .collect(Collectors.toList());
    }

    private static String tarifsTransportToString(List<Integer> list) {
        if (list == null || list.isEmpty()) return null;
        return list.stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
    }

    /** Pour mise à jour partielle (ex: tarifsTransport uniquement). Réutilise la logique centralisée. */
    public String toTarifsTransportString(List<Integer> list) {
        return tarifsTransportToString(list);
    }
}

