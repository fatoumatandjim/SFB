package com.backend.gesy.utilisateur.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurDTO {
    private Long id;
    private String identifiant;
    private String motDePasse;
    private String nom;
    private String email;
    private String telephone;
    private String statut;
    private Boolean actif;
    private String defaultPass;
    private Set<Long> roleIds;
    private List<RoleDTO> roles;
    private Long depotId;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleDTO {
        private Long id;
        private String nom;
        private String description;
        private String statut;
    }
}

