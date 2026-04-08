package com.backend.gesy.voyage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Résultat de la réparation des remises dépôt manquantes après d’anciennes suppressions « test déchargement ».
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReparationRemiseDepotDTO {
    /** Nombre de mouvements correspondant au libellé d’annulation test (diagnostic). */
    private int mouvementsAnnulationDetectes;

    private List<String> corriges = new ArrayList<>();
    private List<String> dejaConformes = new ArrayList<>();
    private List<String> ignoresVoyageEncoreExistant = new ArrayList<>();
    private List<String> erreurs = new ArrayList<>();
}
