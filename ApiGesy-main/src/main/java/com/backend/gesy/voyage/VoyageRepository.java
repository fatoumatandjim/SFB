package com.backend.gesy.voyage;

import com.backend.gesy.axe.Axe;
import com.backend.gesy.camion.Camion;
import com.backend.gesy.client.Client;
import com.backend.gesy.depot.Depot;
import com.backend.gesy.produit.Produit;
import com.backend.gesy.transitaire.Transitaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoyageRepository extends JpaRepository<Voyage, Long> {
       Optional<Voyage> findByNumeroVoyage(String numeroVoyage);

       List<Voyage> findByCamion(Camion camion);

       // Note: findByClient n'est plus utilisé car les clients sont maintenant via ClientVoyage
       // Utiliser ClientVoyageRepository.findByClientId() à la place

       List<Voyage> findByTransitaire(Transitaire transitaire);

       List<Voyage> findByProduit(Produit produit);

       List<Voyage> findByDepot(Depot depot);

       List<Voyage> findByAxe(Axe axe);

       // Pagination pour les voyages par axe avec tri par date décroissante
       @Query(value = "SELECT v FROM Voyage v WHERE v.axe = :axe ORDER BY v.dateDepart DESC, v.id DESC", countQuery = "SELECT COUNT(v) FROM Voyage v WHERE v.axe = :axe")
       Page<Voyage> findByAxePaginated(@Param("axe") Axe axe, Pageable pageable);

       // Trouver les voyages d'un dépôt avec des statuts spécifiques
       @Query("SELECT v FROM Voyage v WHERE v.depot = :depot " +
                     "AND (v.statut = 'CHARGEMENT' OR v.statut = 'CHARGE') " +
                     "ORDER BY v.dateDepart DESC")
       List<Voyage> findByDepotAndStatutsChargement(@Param("depot") Depot depot);

       // Compter les camions chargés par dépôt (voyages où l'état "Chargé" ou "Départ"
       // est validé)
       @Query("SELECT COUNT(DISTINCT v.camion) FROM Voyage v " +
                     "JOIN v.etats e " +
                     "WHERE v.depot = :depot " +
                     "AND e.etat IN ('Chargé', 'Départ') " +
                     "AND e.valider = true")
       Long countCamionsChargesByDepot(@Param("depot") Depot depot);

       // Pagination pour les voyages archivés d'un transitaire (voyages où l'état
       // "Douane" est validé)
       @Query(value = "SELECT DISTINCT v FROM Voyage v " +
                     "JOIN v.etats e " +
                     "WHERE v.transitaire = :transitaire " +
                     "AND e.etat = 'Douane' " +
                     "AND e.valider = true " +
                     "ORDER BY v.dateDepart DESC", countQuery = "SELECT COUNT(DISTINCT v.id) FROM Voyage v " +
                                   "JOIN v.etats e " +
                                   "WHERE v.transitaire = :transitaire " +
                                   "AND e.etat = 'Douane' " +
                                   "AND e.valider = true")
       Page<Voyage> findArchivedVoyagesByTransitaire(@Param("transitaire") Transitaire transitaire, Pageable pageable);

       // Pagination avec filtre par date pour les voyages archivés d'un transitaire
       // (voyages où l'état "Douane" est validé)
       @Query(value = "SELECT DISTINCT v FROM Voyage v " +
                     "JOIN v.etats e " +
                     "WHERE v.transitaire = :transitaire " +
                     "AND e.etat = 'Douane' " +
                     "AND e.valider = true " +
                     "AND v.dateDepart IS NOT NULL " +
                     "AND v.dateDepart >= :startOfDay AND v.dateDepart < :endOfDay " +
                     "ORDER BY v.dateDepart DESC", countQuery = "SELECT COUNT(DISTINCT v.id) FROM Voyage v " +
                                   "JOIN v.etats e " +
                                   "WHERE v.transitaire = :transitaire " +
                                   "AND e.etat = 'Douane' " +
                                   "AND e.valider = true " +
                                   "AND v.dateDepart IS NOT NULL " +
                                   "AND v.dateDepart >= :startOfDay AND v.dateDepart < :endOfDay")
       Page<Voyage> findArchivedVoyagesByTransitaireAndDate(
                     @Param("transitaire") Transitaire transitaire,
                     @Param("startOfDay") LocalDateTime startOfDay,
                     @Param("endOfDay") LocalDateTime endOfDay,
                     Pageable pageable);

       // Pagination avec filtre par intervalle de dates pour les voyages archivés d'un
       // transitaire (voyages où l'état "Douane" est validé)
       @Query(value = "SELECT DISTINCT v FROM Voyage v " +
                     "JOIN v.etats e " +
                     "WHERE v.transitaire = :transitaire " +
                     "AND e.etat = 'Douane' " +
                     "AND e.valider = true " +
                     "AND v.dateDepart IS NOT NULL " +
                     "AND v.dateDepart >= :startDate AND v.dateDepart <= :endDate " +
                     "ORDER BY v.dateDepart DESC", countQuery = "SELECT COUNT(DISTINCT v.id) FROM Voyage v " +
                                   "JOIN v.etats e " +
                                   "WHERE v.transitaire = :transitaire " +
                                   "AND e.etat = 'Douane' " +
                                   "AND e.valider = true " +
                                   "AND v.dateDepart IS NOT NULL " +
                                   "AND v.dateDepart >= :startDate AND v.dateDepart <= :endDate")
       Page<Voyage> findArchivedVoyagesByTransitaireAndDateRange(
                     @Param("transitaire") Transitaire transitaire,
                     @Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate,
                     Pageable pageable);

       // Pagination pour tous les voyages archivés (uniquement les voyages déchargés)
       @Query("SELECT v FROM Voyage v WHERE v.statut = 'DECHARGER' " +
                     "ORDER BY v.dateDepart DESC")
       Page<Voyage> findArchivedVoyages(Pageable pageable);
       
       // Voyages partiellement déchargés
       @Query("SELECT v FROM Voyage v WHERE v.statut = 'PARTIELLEMENT_DECHARGER' ORDER BY v.dateDepart DESC, v.id DESC")
       Page<Voyage> findVoyagesPartiellementDecharges(Pageable pageable);
       
       // Voyages en cours (non déchargés)
       @Query("SELECT v FROM Voyage v WHERE v.statut != 'DECHARGER' ORDER BY v.dateDepart DESC, v.id DESC")
       Page<Voyage> findVoyagesEnCours(Pageable pageable);

       // Voyages en cours (non déchargés) avec au moins un client assigné (pour rapport PDF)
       @Query("SELECT DISTINCT v FROM Voyage v JOIN v.clientVoyages cv " +
                     "WHERE v.statut != 'DECHARGER' " +
                     "ORDER BY v.dateDepart DESC, v.id DESC")
       List<Voyage> findVoyagesEnCoursAvecClients();

       // Pagination avec filtre par date pour tous les voyages archivés
       @Query("SELECT v FROM Voyage v WHERE v.statut = 'DECHARGER' " +
                     "AND DATE(v.dateDepart) = DATE(:date) " +
                     "ORDER BY v.dateDepart DESC")
       Page<Voyage> findArchivedVoyagesByDate(@Param("date") LocalDateTime date, Pageable pageable);

       // Pagination avec filtre par intervalle de dates pour tous les voyages archivés
       @Query("SELECT v FROM Voyage v WHERE v.statut = 'DECHARGER' " +
                     "AND v.dateDepart >= :startDate AND v.dateDepart <= :endDate " +
                     "ORDER BY v.dateDepart DESC")
       Page<Voyage> findArchivedVoyagesByDateRange(
                     @Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate,
                     Pageable pageable);

       // Trouver le dernier numéro de bon d'enlèvement assigné
       @Query("SELECT v.numeroBonEnlevement FROM Voyage v WHERE v.numeroBonEnlevement IS NOT NULL ORDER BY v.numeroBonEnlevement DESC")
       List<String> findLastNumeroBonEnlevement(org.springframework.data.domain.Pageable pageable);

       // Vérifier si un numéro de bon d'enlèvement existe déjà
       boolean existsByNumeroBonEnlevement(String numeroBonEnlevement);

       // Trouver les voyages chargés d'un dépôt avec pagination (voyages où l'état
       // "Chargé" ou "Départ" est validé)
       @Query(value = "SELECT DISTINCT v FROM Voyage v " +
                     "JOIN v.etats e " +
                     "WHERE v.depot = :depot " +
                     "AND e.etat IN ('Chargé', 'Départ') " +
                     "AND e.valider = true " +
                     "ORDER BY v.dateDepart DESC", countQuery = "SELECT COUNT(DISTINCT v.id) FROM Voyage v " +
                                   "JOIN v.etats e " +
                                   "WHERE v.depot = :depot " +
                                   "AND e.etat IN ('Chargé', 'Départ') " +
                                   "AND e.valider = true")
       Page<Voyage> findVoyagesChargesByDepot(@Param("depot") Depot depot, Pageable pageable);

       // Trouver les voyages chargés d'un dépôt avec filtre par date
       @Query(value = "SELECT DISTINCT v FROM Voyage v " +
                     "JOIN v.etats e " +
                     "WHERE v.depot = :depot " +
                     "AND e.etat IN ('Chargé', 'Départ') " +
                     "AND e.valider = true " +
                     "AND v.dateDepart IS NOT NULL " +
                     "AND v.dateDepart >= :startOfDay AND v.dateDepart < :endOfDay " +
                     "ORDER BY v.dateDepart DESC", countQuery = "SELECT COUNT(DISTINCT v.id) FROM Voyage v " +
                                   "JOIN v.etats e " +
                                   "WHERE v.depot = :depot " +
                                   "AND e.etat IN ('Chargé', 'Départ') " +
                                   "AND e.valider = true " +
                                   "AND v.dateDepart IS NOT NULL " +
                                   "AND v.dateDepart >= :startOfDay AND v.dateDepart < :endOfDay")
       Page<Voyage> findVoyagesChargesByDepotAndDate(
                     @Param("depot") Depot depot,
                     @Param("startOfDay") LocalDateTime startOfDay,
                     @Param("endOfDay") LocalDateTime endOfDay,
                     Pageable pageable);

       // Trouver les voyages chargés d'un dépôt avec filtre par intervalle de dates
       @Query(value = "SELECT DISTINCT v FROM Voyage v " +
                     "JOIN v.etats e " +
                     "WHERE v.depot = :depot " +
                     "AND e.etat IN ('Chargé', 'Départ') " +
                     "AND e.valider = true " +
                     "AND v.dateDepart IS NOT NULL " +
                     "AND v.dateDepart >= :startDate AND v.dateDepart <= :endDate " +
                     "ORDER BY v.dateDepart DESC", countQuery = "SELECT COUNT(DISTINCT v.id) FROM Voyage v " +
                                   "JOIN v.etats e " +
                                   "WHERE v.depot = :depot " +
                                   "AND e.etat IN ('Chargé', 'Départ') " +
                                   "AND e.valider = true " +
                                   "AND v.dateDepart IS NOT NULL " +
                                   "AND v.dateDepart >= :startDate AND v.dateDepart <= :endDate")
       Page<Voyage> findVoyagesChargesByDepotAndDateRange(
                     @Param("depot") Depot depot,
                     @Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate,
                     Pageable pageable);

       // Trouver les voyages non déclarés d'un transitaire (ancienne logique, conservée pour compatibilité)
       @Query("SELECT v FROM Voyage v WHERE v.transitaire = :transitaire " +
                     "AND (v.declarer = false OR v.declarer IS NULL) " +
                     "ORDER BY v.id DESC")
       List<Voyage> findVoyagesNonDeclaresByTransitaire(@Param("transitaire") Transitaire transitaire);

       /** Voyages actifs du transitaire (non libérés) : restent en « en cours » jusqu'à libération */
       @Query("SELECT v FROM Voyage v WHERE v.transitaire = :transitaire " +
                     "AND (v.liberer = false OR v.liberer IS NULL) " +
                     "ORDER BY v.dateDepart ASC, v.id ASC")
       List<Voyage> findVoyagesActifsByTransitaire(@Param("transitaire") Transitaire transitaire);

       /** Voyages en cours du transitaire : non déclarés ou passer_non_declarer (reste même après Libérer) */
       @Query(value = "SELECT v FROM Voyage v WHERE v.transitaire = :transitaire " +
                     "AND ( (v.declarer = false OR v.declarer IS NULL) OR v.passager = 'passer_non_declarer' ) " +
                     "ORDER BY v.dateDepart ASC, v.id ASC",
                     countQuery = "SELECT COUNT(v) FROM Voyage v WHERE v.transitaire = :transitaire " +
                                   "AND ( (v.declarer = false OR v.declarer IS NULL) OR v.passager = 'passer_non_declarer' )")
       Page<Voyage> findVoyagesEnCoursByTransitaire(@Param("transitaire") Transitaire transitaire, Pageable pageable);

       /** Voyages archivés du transitaire (libérés) : passent aux archives quand on les libère */
       @Query(value = "SELECT v FROM Voyage v WHERE v.transitaire = :transitaire " +
                     "AND v.liberer = true " +
                     "ORDER BY v.dateDepart ASC, v.id ASC",
                     countQuery = "SELECT COUNT(v) FROM Voyage v WHERE v.transitaire = :transitaire AND v.liberer = true")
       Page<Voyage> findVoyagesArchivesByTransitaire(@Param("transitaire") Transitaire transitaire, Pageable pageable);

       @Query(value = "SELECT v FROM Voyage v WHERE v.transitaire = :transitaire AND v.liberer = true " +
                     "AND v.dateDepart IS NOT NULL AND v.dateDepart >= :startOfDay AND v.dateDepart < :endOfDay " +
                     "ORDER BY v.dateDepart ASC, v.id ASC",
                     countQuery = "SELECT COUNT(v) FROM Voyage v WHERE v.transitaire = :transitaire AND v.liberer = true " +
                                   "AND v.dateDepart IS NOT NULL AND v.dateDepart >= :startOfDay AND v.dateDepart < :endOfDay")
       Page<Voyage> findVoyagesArchivesByTransitaireAndDate(
                     @Param("transitaire") Transitaire transitaire,
                     @Param("startOfDay") LocalDateTime startOfDay,
                     @Param("endOfDay") LocalDateTime endOfDay,
                     Pageable pageable);

       @Query(value = "SELECT v FROM Voyage v WHERE v.transitaire = :transitaire AND v.liberer = true " +
                     "AND v.dateDepart IS NOT NULL AND v.dateDepart >= :startDate AND v.dateDepart <= :endDate " +
                     "ORDER BY v.dateDepart ASC, v.id ASC",
                     countQuery = "SELECT COUNT(v) FROM Voyage v WHERE v.transitaire = :transitaire AND v.liberer = true " +
                                   "AND v.dateDepart IS NOT NULL AND v.dateDepart >= :startDate AND v.dateDepart <= :endDate")
       Page<Voyage> findVoyagesArchivesByTransitaireAndDateRange(
                     @Param("transitaire") Transitaire transitaire,
                     @Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate,
                     Pageable pageable);

       // Trouver les voyages passés non déclarés (passager = 'passer_non_declarer' et
       // declarer = false)
       @Query("SELECT v FROM Voyage v WHERE v.passager = 'passer_non_declarer' " +
                     "AND (v.declarer = false OR v.declarer IS NULL) " +
                     "ORDER BY v.id DESC")
       List<Voyage> findVoyagesPassesNonDeclares();

       // Trouver les voyages passés non déclarés avec pagination
       @Query(value = "SELECT v FROM Voyage v WHERE v.passager = 'passer_non_declarer' " +
                     "AND (v.declarer = false OR v.declarer IS NULL) " +
                     "ORDER BY v.id DESC", countQuery = "SELECT COUNT(v) FROM Voyage v WHERE v.passager = 'passer_non_declarer' "
                                   +
                                   "AND (v.declarer = false OR v.declarer IS NULL)")
       Page<Voyage> findVoyagesPassesNonDeclaresPaginated(Pageable pageable);

       // Trouver les voyages avec ClientVoyage mais sans facture (sans prix d'achat)
       // Un voyage a des clients s'il a des ClientVoyage
       // Un voyage a une facture s'il a au moins une facture dans la liste factures
       @Query(value = "SELECT DISTINCT v FROM Voyage v " +
                     "JOIN v.clientVoyages cv " +
                     "WHERE SIZE(v.clientVoyages) > 0 " +
                     "AND SIZE(v.factures) = 0 " +
                     "ORDER BY v.dateDepart DESC, v.id DESC", 
                     countQuery = "SELECT COUNT(DISTINCT v) FROM Voyage v " +
                                   "JOIN v.clientVoyages cv " +
                                   "WHERE SIZE(v.clientVoyages) > 0 " +
                                   "AND SIZE(v.factures) = 0")
       Page<Voyage> findVoyagesAvecClientSansFacture(Pageable pageable);

       /** Voyages attribués (non cession) sans prix de transport — pour le comptable */
       @Query("SELECT v FROM Voyage v WHERE v.cession = false AND (v.prixUnitaire IS NULL OR v.prixUnitaire <= 0) ORDER BY v.dateDepart DESC, v.id DESC")
       Page<Voyage> findVoyagesSansPrixTransport(Pageable pageable);
}
