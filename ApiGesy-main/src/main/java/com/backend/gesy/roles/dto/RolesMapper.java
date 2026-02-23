package com.backend.gesy.roles.dto;

import com.backend.gesy.roles.Roles;
import org.springframework.stereotype.Component;

@Component
public class RolesMapper {
    public RolesDTO toDTO(Roles roles) {
        if (roles == null) {
            return null;
        }
        
        RolesDTO dto = new RolesDTO();
        dto.setId(roles.getId());
        dto.setNom(roles.getNom());
        dto.setDescription(roles.getDescription());
        dto.setStatut(roles.getStatut() != null ? roles.getStatut().name() : null);
        
        return dto;
    }

    public Roles toEntity(RolesDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Roles roles = new Roles();
        roles.setId(dto.getId());
        roles.setNom(dto.getNom());
        roles.setDescription(dto.getDescription());
        if (dto.getStatut() != null) {
            roles.setStatut(Roles.StatutRole.valueOf(dto.getStatut()));
        }
        
        return roles;
    }
}

