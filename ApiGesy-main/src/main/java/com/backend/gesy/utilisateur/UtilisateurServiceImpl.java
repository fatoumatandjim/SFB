package com.backend.gesy.utilisateur;

import com.backend.gesy.depot.Depot;
import com.backend.gesy.depot.DepotRepository;
import com.backend.gesy.roles.Roles;
import com.backend.gesy.roles.RolesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Service
@Transactional
public class UtilisateurServiceImpl implements UtilisateurService {
    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private RolesRepository rolesRepository;
    
    @Autowired
    private DepotRepository depotRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public List<Utilisateur> findAll() {
        return utilisateurRepository.findAll();
    }

    @Override
    public List<Utilisateur> findLogisticiensEtResponsables() {
        // Retourner uniquement les utilisateurs actifs ayant un rôle
        // "Responsable Logistique", "Logisticien" ou "Simple Logisticien"
        return utilisateurRepository.findAll().stream()
            .filter(u -> Boolean.TRUE.equals(u.getActif()))
            .filter(u -> u.getRoles() != null && u.getRoles().stream()
                .anyMatch(r ->
                    r.getStatut() == Roles.StatutRole.ACTIF &&
                    (
                        "Responsable Logistique".equalsIgnoreCase(r.getNom()) ||
                        "Logisticien".equalsIgnoreCase(r.getNom()) ||
                        "Simple Logisticien".equalsIgnoreCase(r.getNom())
                    )
                )
            )
            .toList();
    }

    @Override
    public Optional<Utilisateur> findById(Long id) {
        return utilisateurRepository.findById(id);
    }

    @Override
    public Optional<Utilisateur> findByIdentifiant(String identifiant) {
        return utilisateurRepository.findByIdentifiant(identifiant);
    }

    @Override
    public Optional<Utilisateur> findByEmail(String email) {
        return utilisateurRepository.findByEmail(email);
    }

    @Override
    public Optional<Utilisateur> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof String) {
            String identifiant = (String) authentication.getPrincipal();
            return utilisateurRepository.findByIdentifiant(identifiant);
        }
        return Optional.empty();
    }

    @Override
    public Utilisateur save(Utilisateur utilisateur) {
        // Générer l'identifiant si non fourni
        if (utilisateur.getIdentifiant() == null || utilisateur.getIdentifiant().isEmpty()) {
            String identifiant = genererIdentifiant(utilisateur.getNom());
            utilisateur.setIdentifiant(identifiant);
        }

        // Générer le mot de passe si non fourni
        if (utilisateur.getMotDePasse() == null || utilisateur.getMotDePasse().isEmpty()) {
            String motDePasse = genererMotDePasse();
            utilisateur.setDefaultPass(motDePasse);
            // Encoder le mot de passe avec BCrypt
            utilisateur.setMotDePasse(passwordEncoder.encode(motDePasse));
        } else {
            // Encoder le mot de passe fourni
            utilisateur.setMotDePasse(passwordEncoder.encode(utilisateur.getMotDePasse()));
        }

        // Gérer les rôles
        if (utilisateur.getRoles() != null && !utilisateur.getRoles().isEmpty()) {
            Set<Roles> rolesPersistants = new HashSet<>();
            for (Roles role : utilisateur.getRoles()) {
                if (role.getId() != null) {
                    // Si le rôle a un ID, le récupérer depuis la base
                    rolesRepository.findById(role.getId()).ifPresent(rolesPersistants::add);
                } else if (role.getNom() != null) {
                    // Si le rôle a un nom, chercher par nom
                    rolesRepository.findByNom(role.getNom()).ifPresent(rolesPersistants::add);
                }
            }
            utilisateur.setRoles(rolesPersistants);
        }

        // Gérer le dépôt
        if (utilisateur.getDepot() != null && utilisateur.getDepot().getId() != null) {
            depotRepository.findById(utilisateur.getDepot().getId())
                .ifPresent(utilisateur::setDepot);
        } else {
            utilisateur.setDepot(null);
        }

        return utilisateurRepository.save(utilisateur);
    }

    private String genererIdentifiant(String nom) {
        if (nom == null || nom.isEmpty()) {
            throw new IllegalArgumentException("Le nom ne peut pas être vide");
        }

        // Convertir le nom en format sfb.nom (remplacer espaces par points, minuscules)
        String baseIdentifiant = "sfb." + nom.toLowerCase()
            .trim()
            .replaceAll("\\s+", ".") // Remplacer un ou plusieurs espaces par un point
            .replaceAll("[^a-z0-9.]", ""); // Supprimer les caractères non alphanumériques sauf les points

        String identifiant = baseIdentifiant;
        Random random = new Random();

        // Vérifier l'unicité et ajouter des chiffres si nécessaire
        int tentatives = 0;
        while (utilisateurRepository.findByIdentifiant(identifiant).isPresent() && tentatives < 100) {
            // Ajouter 2 chiffres aléatoires
            int chiffres = random.nextInt(90) + 10; // Génère un nombre entre 10 et 99
            identifiant = baseIdentifiant + chiffres;
            tentatives++;
        }

        if (tentatives >= 100) {
            // Si après 100 tentatives on n'a pas trouvé d'identifiant unique, utiliser timestamp
            identifiant = baseIdentifiant + System.currentTimeMillis() % 10000;
        }

        return identifiant;
    }

    private String genererMotDePasse() {
        Random random = new Random();
        int chiffres = random.nextInt(9000) + 1000; // Génère un nombre entre 1000 et 9999
        return "pass@" + chiffres;
    }

    @Override
    public Utilisateur update(Long id, Utilisateur utilisateur) {
        Utilisateur existingUtilisateur = utilisateurRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id: " + id));
        
        // Préserver l'identifiant existant
        utilisateur.setId(existingUtilisateur.getId());
        utilisateur.setIdentifiant(existingUtilisateur.getIdentifiant());
        
        // Si le mot de passe n'est pas fourni, conserver l'ancien
        if (utilisateur.getMotDePasse() == null || utilisateur.getMotDePasse().isEmpty()) {
            utilisateur.setMotDePasse(existingUtilisateur.getMotDePasse());
            utilisateur.setDefaultPass(existingUtilisateur.getDefaultPass());
        } else {
            // Si un nouveau mot de passe est fourni, l'encoder
            utilisateur.setMotDePasse(passwordEncoder.encode(utilisateur.getMotDePasse()));
        }

        // Gérer les rôles
        if (utilisateur.getRoles() != null && !utilisateur.getRoles().isEmpty()) {
            Set<Roles> rolesPersistants = new HashSet<>();
            for (Roles role : utilisateur.getRoles()) {
                if (role.getId() != null) {
                    rolesRepository.findById(role.getId()).ifPresent(rolesPersistants::add);
                } else if (role.getNom() != null) {
                    rolesRepository.findByNom(role.getNom()).ifPresent(rolesPersistants::add);
                }
            }
            utilisateur.setRoles(rolesPersistants);
        } else {
            // Si aucun rôle n'est fourni, conserver les rôles existants
            utilisateur.setRoles(existingUtilisateur.getRoles());
        }

        // Gérer le dépôt
        if (utilisateur.getDepot() != null && utilisateur.getDepot().getId() != null) {
            depotRepository.findById(utilisateur.getDepot().getId())
                .ifPresent(utilisateur::setDepot);
        } else {
            // Si aucun dépôt n'est fourni, conserver le dépôt existant ou le mettre à null
            if (utilisateur.getDepot() == null) {
                utilisateur.setDepot(existingUtilisateur.getDepot());
            } else {
                utilisateur.setDepot(null);
            }
        }

        return utilisateurRepository.save(utilisateur);
    }

    @Override
    public void deleteById(Long id) {
        utilisateurRepository.deleteById(id);
    }
}

