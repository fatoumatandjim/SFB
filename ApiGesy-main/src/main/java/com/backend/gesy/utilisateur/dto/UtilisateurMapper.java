package com.backend.gesy.utilisateur.dto;

import com.backend.gesy.roles.Roles;
import com.backend.gesy.utilisateur.Utilisateur;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UtilisateurMapper {
    public UtilisateurDTO toDTO(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }
        
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setId(utilisateur.getId());
        dto.setIdentifiant(utilisateur.getIdentifiant());
        dto.setMotDePasse(utilisateur.getMotDePasse());
        dto.setNom(utilisateur.getNom());
        dto.setEmail(utilisateur.getEmail());
        dto.setTelephone(utilisateur.getTelephone());
        dto.setStatut(utilisateur.getStatut() != null ? utilisateur.getStatut().name() : null);
        dto.setActif(utilisateur.getActif());
        dto.setDefaultPass(utilisateur.getDefaultPass());
        
        if (utilisateur.getRoles() != null) {
            dto.setRoleIds(utilisateur.getRoles().stream()
                .map(Roles::getId)
                .collect(Collectors.toSet()));
            
            List<UtilisateurDTO.RoleDTO> rolesDTO = utilisateur.getRoles().stream()
                .map(role -> {
                    UtilisateurDTO.RoleDTO roleDTO = new UtilisateurDTO.RoleDTO();
                    roleDTO.setId(role.getId());
                    roleDTO.setNom(role.getNom());
                    roleDTO.setDescription(role.getDescription());
                    roleDTO.setStatut(role.getStatut() != null ? role.getStatut().name() : null);
                    return roleDTO;
                })
                .collect(Collectors.toList());
            dto.setRoles(rolesDTO);
        }
        
        if (utilisateur.getDepot() != null) {
            dto.setDepotId(utilisateur.getDepot().getId());
        }
        
        return dto;
    }

    public Utilisateur toEntity(UtilisateurDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(dto.getId());
        utilisateur.setIdentifiant(dto.getIdentifiant());
        utilisateur.setMotDePasse(dto.getMotDePasse());
        utilisateur.setNom(dto.getNom());
        utilisateur.setEmail(dto.getEmail());
        utilisateur.setTelephone(dto.getTelephone());
        if (dto.getStatut() != null) {
            utilisateur.setStatut(Utilisateur.StatutUtilisateur.valueOf(dto.getStatut()));
        }
        
        return utilisateur;
    }
}

