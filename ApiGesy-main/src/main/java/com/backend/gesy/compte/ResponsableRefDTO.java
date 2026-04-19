package com.backend.gesy.compte;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Résumé d’un compte désigné comme responsable (affichage liste banque/caisse).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponsableRefDTO {
    private Long id;
    private String nom;
}
