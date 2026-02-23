package com.backend.gesy.transitaire.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransitaireDTO {
    private Long id;
    private String identifiant;
    private String motDePasse;
    private String defaultPass;
    private String nom;
    private String email;
    private String telephone;
    private String statut; // Representing StatutTransitaire enum as String
    private Long nombreVoyages; // Nombre de voyages attribués
    private Set<Long> roleIds; // IDs des rôles
}
