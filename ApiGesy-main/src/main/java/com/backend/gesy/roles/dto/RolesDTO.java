package com.backend.gesy.roles.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolesDTO {
    private Long id;
    private String nom;
    private String description;
    private String statut;
}

