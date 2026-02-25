package com.backend.gesy;

import com.backend.gesy.caisse.CaisseService;
import com.backend.gesy.categoriedepense.CategorieDepense;
import com.backend.gesy.categoriedepense.CategorieDepenseRepository;
import com.backend.gesy.compte.Compte;
import com.backend.gesy.compte.CompteRepository;
import com.backend.gesy.depot.DepotService;
import com.backend.gesy.depot.dto.DepotDTO;
import com.backend.gesy.produit.ProduitService;
import com.backend.gesy.produit.dto.ProduitDTO;
import com.backend.gesy.roles.Roles;
import com.backend.gesy.roles.RolesRepository;
import com.backend.gesy.transitaire.Transitaire;
import com.backend.gesy.utilisateur.Utilisateur;
import com.backend.gesy.utilisateur.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;


import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SpringBootApplication
public class GesyApplication {
	@Autowired
	private RolesRepository rolesRepository;
	@Autowired
	private UtilisateurRepository utilisateurRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private CompteRepository compteRepository;
	@Autowired
	private CategorieDepenseRepository categorieDepenseRepository;
	public static void main(String[] args) {
		SpringApplication.run(GesyApplication.class, args);
	}

	@Bean
	public CommandLineRunner initDefaultCaisse(CaisseService caisseService,
	                                          ProduitService produitService,
	                                          DepotService depotService) {
		return args -> {
			initialiserRolesParDefaut();
			caisseService.initializeDefaultCaisse();
			initialiserCategorieDepenseParDefaut();
			initialiserProduitsParDefaut(produitService);
			initialiserDepotsParDefaut(depotService);
			migratePasswordsAndAssignRoles();
			if (utilisateurRepository.findByIdentifiant("adminSFB").isEmpty()){
				Utilisateur adminUser = new Utilisateur();
				adminUser.setIdentifiant("adminSFB");
				adminUser.setMotDePasse(passwordEncoder.encode("adminSFB@_123"));
				adminUser.setDefaultPass("adminSFB@_123");
				adminUser.setEmail("sfb@admin.com");
				adminUser.setNom("Admin SFB");
				adminUser.setTelephone("2230000000");
				adminUser.setActif(true);
				adminUser.setStatut(Utilisateur.StatutUtilisateur.ACTIF);
				adminUser.setCreatedAt(java.time.LocalDateTime.now());
				Roles roles = rolesRepository.findByNom("Admin").orElseThrow(() -> new RuntimeException("Rôle Admin non trouvé"));
				adminUser.getRoles().add(roles);
				utilisateurRepository.save(adminUser);
			}

		};
	}

	private void initialiserCategorieDepenseParDefaut() {
		// Créer la catégorie "Investissement" si elle n'existe pas
		if (!categorieDepenseRepository.findByNom("Investissement").isPresent()) {
			CategorieDepense categorieInvestissement = new CategorieDepense();
			categorieInvestissement.setNom("Investissement");
			categorieInvestissement.setDescription("Catégorie pour les investissements");
			categorieInvestissement.setStatut(CategorieDepense.StatutCategorie.ACTIF);
			categorieDepenseRepository.save(categorieInvestissement);
		}
	}

	/** Produits par défaut : Essence et Gasoil (via ProduitService.save(ProduitDTO)). */
	private void initialiserProduitsParDefaut(ProduitService produitService) {
		if (produitService.findByNom("Essence").isEmpty()) {
			ProduitDTO essence = new ProduitDTO();
			essence.setNom("Essence");
			essence.setTypeProduit("ESSENCE");
			essence.setDescription("Essence");
			produitService.save(essence);
		}
		if (produitService.findByNom("Gasoil").isEmpty()) {
			ProduitDTO gasoil = new ProduitDTO();
			gasoil.setNom("Gasoil");
			gasoil.setTypeProduit("GAZOLE");
			gasoil.setDescription("Gasoil");
			produitService.save(gasoil);
		}
	}

	/** Dépôts par défaut : Sentock Vivo et Gestoci (capacité 2 milliards de litres chacun). */
	private void initialiserDepotsParDefaut(DepotService depotService) {
		double capaciteDeuxMilliards = 2_000_000_000.0;
		if (depotService.findByNom("Sentock").isEmpty()) {
			DepotDTO sentock = new DepotDTO();
			sentock.setNom("Sentock");
			sentock.setAdresse("À définir");
			sentock.setCapacite(capaciteDeuxMilliards);
			sentock.setStatut("ACTIF");
			depotService.save(sentock);
		}
		if (depotService.findByNom("Vivo").isEmpty()) {
			DepotDTO sentock = new DepotDTO();
			sentock.setNom("Vivo");
			sentock.setAdresse("À définir");
			sentock.setCapacite(capaciteDeuxMilliards);
			sentock.setStatut("ACTIF");
			depotService.save(sentock);
		}
		if (depotService.findByNom("Gestoci").isEmpty()) {
			DepotDTO gestoci = new DepotDTO();
			gestoci.setNom("Gestoci");
			gestoci.setAdresse("À définir");
			gestoci.setCapacite(capaciteDeuxMilliards);
			gestoci.setStatut("ACTIF");
			depotService.save(gestoci);
		}
	}

	private void initialiserRolesParDefaut() {
		// Admin : accès complet, validation des paiements
		if (!rolesRepository.findByNom("Admin").isPresent()) {
			Roles admin = new Roles();
			admin.setNom("Admin");
			admin.setDescription("Administrateur avec accès complet au système ; valide les paiements effectués par le comptable");
			admin.setStatut(Roles.StatutRole.ACTIF);
			rolesRepository.save(admin);
		}

		// Transitaire : interface transitaire (douane, déclaration, libération)
		if (!rolesRepository.findByNom("Transitaire").isPresent()) {
			Roles transitaire = new Roles();
			transitaire.setNom("Transitaire");
			transitaire.setDescription("Gestion des voyages assignés (douane, déclaration, libération)");
			transitaire.setStatut(Roles.StatutRole.ACTIF);
			rolesRepository.save(transitaire);
		}

		// Responsable logistique unique : crée les voyages, attribue aux logisticiens, charge les camions ; pas d'accès clients
		if (!rolesRepository.findByNom("Responsable Logistique").isPresent()) {
			Roles r = new Roles();
			r.setNom("Responsable Logistique");
			r.setDescription("Crée les voyages, attribue aux logisticiens, charge les camions ; pas d'accès aux clients");
			r.setStatut(Roles.StatutRole.ACTIF);
			rolesRepository.save(r);
		}

		// Autres logisticiens : gèrent les camions attribués, suivi jusqu'à réception ; pas d'accès clients
		if (!rolesRepository.findByNom("Logisticien").isPresent()) {
			Roles r = new Roles();
			r.setNom("Logisticien");
			r.setDescription("Gère uniquement les camions attribués, suivi jusqu'à réception ; pas d'accès aux clients");
			r.setStatut(Roles.StatutRole.ACTIF);
			rolesRepository.save(r);
		}

		// Conserver "Simple Logisticien" pour rétrocompatibilité (alias Logisticien)
		if (!rolesRepository.findByNom("Simple Logisticien").isPresent()) {
			Roles r = new Roles();
			r.setNom("Simple Logisticien");
			r.setDescription("Logisticien : camions attribués, suivi jusqu'à réception ; pas d'accès aux clients");
			r.setStatut(Roles.StatutRole.ACTIF);
			rolesRepository.save(r);
		}

		// Contrôleur : attribution, gestion des manquants ; le comptable définit les prix
		if (!rolesRepository.findByNom("Contrôleur").isPresent()) {
			Roles r = new Roles();
			r.setNom("Contrôleur");
			r.setDescription("Attribution, gestion des manquants ; les prix sont définis par le comptable");
			r.setStatut(Roles.StatutRole.ACTIF);
			rolesRepository.save(r);
		}

		// Comptable : gestion de tous les paiements, avec validation par l'administrateur
		if (!rolesRepository.findByNom("Comptable").isPresent()) {
			Roles r = new Roles();
			r.setNom("Comptable");
			r.setDescription("Gestion de tous les paiements ; paiements effectués par le comptable avec validation de l'administrateur");
			r.setStatut(Roles.StatutRole.ACTIF);
			rolesRepository.save(r);
		}

		// Charger Depot : charge les camions, départ des citernes
		// if (!rolesRepository.findByNom("Charger Depot").isPresent()) {
		// 	Roles r = new Roles();
		// 	r.setNom("Charger Depot");
		// 	r.setDescription("Charger les citernes et départ des citernes");
		// 	r.setStatut(Roles.StatutRole.ACTIF);
		// 	rolesRepository.save(r);
		// }
	}

	public void migratePasswordsAndAssignRoles() {
		System.out.println("Démarrage de la migration des mots de passe et attribution des rôles...");

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
					System.out.println("Mot de passe migré pour le compte: "+ compte.getIdentifiant());
				}
			}

			// 2. Assigner des rôles par défaut si le compte n'a pas de rôles
			if (compte.getRoles() == null || compte.getRoles().isEmpty()) {
				Set<Roles> defaultRoles = getDefaultRoles(compte);
				if (!defaultRoles.isEmpty()) {
					compte.setRoles(defaultRoles);
					updated = true;
					rolesAssignedCount++;

				}
			}

			// Sauvegarder si des modifications ont été apportées
			if (updated) {
				compteRepository.save(compte);
			}
		}

		System.out.println("Migration terminée. Mots de passe migrés: " + migratedCount + ", Comptes avec rôles assignés: " + rolesAssignedCount);
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
