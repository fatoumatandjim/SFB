package com.backend.gesy.transitaire.dto;

import com.backend.gesy.transitaire.Transitaire;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class TransitaireMapper {
    public TransitaireDTO toDTO(Transitaire transitaire) {
        if (transitaire == null) {
            return null;
        }

        TransitaireDTO dto = new TransitaireDTO();
        dto.setId(transitaire.getId());
        dto.setIdentifiant(transitaire.getIdentifiant());
        dto.setMotDePasse(transitaire.getMotDePasse());
        dto.setDefaultPass(transitaire.getDefaultPass());
        dto.setNom(transitaire.getNom());
        dto.setEmail(transitaire.getEmail());
        dto.setTelephone(transitaire.getTelephone());
        dto.setStatut(transitaire.getStatut() != null ? transitaire.getStatut().name() : null);
        
        // Compter le nombre de voyages
        if (transitaire.getVoyages() != null) {
            dto.setNombreVoyages((long) transitaire.getVoyages().size());
        } else {
            dto.setNombreVoyages(0L);
        }
        
        // Mapper les IDs des rôles
        if (transitaire.getRoles() != null) {
            dto.setRoleIds(transitaire.getRoles().stream()
                .map(role -> role.getId())
                .collect(Collectors.toSet()));
        }

        return dto;
    }

    public Transitaire toEntity(TransitaireDTO dto) {
        if (dto == null) {
            return null;
        }

        Transitaire transitaire = new Transitaire();
        transitaire.setId(dto.getId());
        transitaire.setIdentifiant(dto.getIdentifiant());
        transitaire.setMotDePasse(dto.getMotDePasse());
        transitaire.setDefaultPass(dto.getDefaultPass());
        transitaire.setNom(dto.getNom());
        transitaire.setEmail(dto.getEmail());
        transitaire.setTelephone(dto.getTelephone());
        if (dto.getStatut() != null) {
            transitaire.setStatut(Transitaire.StatutTransitaire.valueOf(dto.getStatut()));
        }
        // Les voyages et rôles seront gérés par le service

        return transitaire;
    }
}
