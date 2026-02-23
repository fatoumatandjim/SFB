package com.backend.gesy.security.migration;

import com.backend.gesy.compte.Compte;
import com.backend.gesy.compte.CompteRepository;
import com.backend.gesy.roles.Roles;
import com.backend.gesy.roles.RolesRepository;
import com.backend.gesy.utilisateur.Utilisateur;
import com.backend.gesy.transitaire.Transitaire;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordMigrationService {

    private final CompteRepository compteRepository;
    private final RolesRepository rolesRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void migratePasswordsAndAssignRoles() {
        log.info("Démarrage de la migration des mots de passe et attribution des rôles...");

        List<Compte> comptes = compteRepository.findAll();
        int migratedCount = 0;
        int rolesAssignedCount = 0;

        for (Compte compte : comptes) {
            boolean updated = false;

            // 1. Migrer le mot de passe si nécessaire
            String currentPassword = compte.getMotDePasse();
            if (currentPassword != null && !currentPassword.isEmpty()) {
                // Vérifier si le mot de passe est déjà encodé (BCrypt commence par $2a$, $2b$, ou $2y$)
                if (!currentPassword.startsWith("$2a$") && 
                    !currentPassword.startsWith("$2b$") && 
                    !currentPassword.startsWith("$2y$")) {
                    // Le mot de passe n'est pas encodé, on l'encode
                    String encodedPassword = passwordEncoder.encode(currentPassword);
                    compte.setMotDePasse(encodedPassword);
                    updated = true;
                    migratedCount++;
                    log.debug("Mot de passe migré pour le compte: {}", compte.getIdentifiant());
                }
            }

            // 2. Assigner des rôles par défaut si le compte n'a pas de rôles
            if (compte.getRoles() == null || compte.getRoles().isEmpty()) {
                Set<Roles> defaultRoles = getDefaultRoles(compte);
                if (!defaultRoles.isEmpty()) {
                    compte.setRoles(defaultRoles);
                    updated = true;
                    rolesAssignedCount++;
                    log.debug("Rôles assignés pour le compte: {} - Rôles: {}", 
                        compte.getIdentifiant(), 
                        defaultRoles.stream().map(Roles::getNom).toList());
                }
            }

            // Sauvegarder si des modifications ont été apportées
            if (updated) {
                compteRepository.save(compte);
            }
        }

        log.info("Migration terminée: {} mots de passe migrés, {} comptes avec rôles assignés", 
            migratedCount, rolesAssignedCount);
    }

    /**
     * Détermine les rôles par défaut selon le type de compte
     */
    private Set<Roles> getDefaultRoles(Compte compte) {
        Set<Roles> defaultRoles = new HashSet<>();

        if (compte instanceof Transitaire) {
            // Pour les transitaires, assigner le rôle "Transitaire"
            rolesRepository.findByNom("Transitaire")
                .ifPresent(defaultRoles::add);
        } else if (compte instanceof Utilisateur) {
            // Pour les utilisateurs, assigner le rôle "Admin" par défaut
            // Vous pouvez modifier cette logique selon vos besoins
            rolesRepository.findByNom("Admin")
                .ifPresent(defaultRoles::add);
        }

        return defaultRoles;
    }
}

