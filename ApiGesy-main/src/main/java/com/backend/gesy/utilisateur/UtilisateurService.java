package com.backend.gesy.utilisateur;

import java.util.List;
import java.util.Optional;

public interface UtilisateurService {
    List<Utilisateur> findAll();
    /**
     * Retourne uniquement les utilisateurs ayant un rôle de type "Responsable Logistique"
     * ou "Logisticien" (y compris l'alias "Simple Logisticien"), et actifs.
     */
    List<Utilisateur> findLogisticiensEtResponsables();
    Optional<Utilisateur> findById(Long id);
    Optional<Utilisateur> findByIdentifiant(String identifiant);
    Optional<Utilisateur> findByEmail(String email);
    Optional<Utilisateur> getCurrentUser();
    Utilisateur save(Utilisateur utilisateur);
    Utilisateur update(Long id, Utilisateur utilisateur);
    void deleteById(Long id);
}

