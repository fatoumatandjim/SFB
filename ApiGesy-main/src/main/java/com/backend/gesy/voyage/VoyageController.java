package com.backend.gesy.voyage;

import com.backend.gesy.voyage.dto.VoyageDTO;
import com.backend.gesy.voyage.dto.VoyagePageDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/voyages")
@CrossOrigin(origins = "*")
public class VoyageController {
    @Autowired
    private VoyageService voyageService;

    @GetMapping
    public ResponseEntity<List<VoyageDTO>> getAllVoyages() {
        return ResponseEntity.ok(voyageService.findAll());
    }

    @GetMapping("/partiellement-decharges")
    public ResponseEntity<VoyagePageDto> getVoyagesPartiellementDecharges(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(voyageService.findVoyagesPartiellementDecharges(page, size));
    }

    @GetMapping("/en-cours")
    public ResponseEntity<VoyagePageDto> getVoyagesEnCours(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(voyageService.findVoyagesEnCours(page, size));
    }

    /** Voyages en cours (non déchargés) avec au moins un client assigné — pour rapport PDF camions/clients */
    @GetMapping("/en-cours-avec-clients")
    public ResponseEntity<List<VoyageDTO>> getVoyagesEnCoursAvecClients() {
        return ResponseEntity.ok(voyageService.findVoyagesEnCoursAvecClients());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VoyageDTO> getVoyageById(@PathVariable Long id) {
        return voyageService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/numero/{numeroVoyage}")
    public ResponseEntity<VoyageDTO> getVoyageByNumero(@PathVariable String numeroVoyage) {
        return voyageService.findByNumeroVoyage(numeroVoyage)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/camion/{camionId}")
    public ResponseEntity<List<VoyageDTO>> getVoyagesByCamion(@PathVariable Long camionId) {
        return ResponseEntity.ok(voyageService.findByCamionId(camionId));
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<VoyageDTO>> getVoyagesByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(voyageService.findByClientId(clientId));
    }

    @GetMapping("/transitaire/{transitaireId}")
    public ResponseEntity<List<VoyageDTO>> getVoyagesByTransitaire(@PathVariable Long transitaireId) {
        return ResponseEntity.ok(voyageService.findByTransitaireId(transitaireId));
    }

    @GetMapping("/transitaire/{transitaireId}/non-declares")
    public ResponseEntity<List<VoyageDTO>> getVoyagesNonDeclaresByTransitaire(@PathVariable Long transitaireId) {
        return ResponseEntity.ok(voyageService.findVoyagesNonDeclaresByTransitaireId(transitaireId));
    }

    @GetMapping("/transitaire/identifiant/{identifiant}/non-declares")
    public ResponseEntity<List<VoyageDTO>> getVoyagesNonDeclaresByTransitaireIdentifiant(
            @PathVariable String identifiant) {
        return ResponseEntity.ok(voyageService.findVoyagesNonDeclaresByTransitaireIdentifiant(identifiant));
    }

    @GetMapping("/transitaire/identifiant/{identifiant}/stats")
    public ResponseEntity<com.backend.gesy.voyage.dto.TransitaireStatsDTO> getTransitaireStatsByIdentifiant(
            @PathVariable String identifiant) {
        return ResponseEntity.ok(voyageService.getTransitaireStatsByIdentifiant(identifiant));
    }

    /** Voyages en cours du transitaire (non déchargés), paginés */
    @GetMapping("/transitaire/identifiant/{identifiant}/en-cours")
    public ResponseEntity<VoyagePageDto> getVoyagesEnCoursByTransitaireIdentifiant(
            @PathVariable String identifiant,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(voyageService.findVoyagesEnCoursByTransitaireIdentifiant(identifiant, page, size));
    }

    @GetMapping("/depot/{depotId}")
    public ResponseEntity<List<VoyageDTO>> getVoyagesByDepot(@PathVariable Long depotId) {
        return ResponseEntity.ok(voyageService.findByDepotId(depotId));
    }

    @GetMapping("/axe/{axeId}")
    public ResponseEntity<List<VoyageDTO>> getVoyagesByAxe(@PathVariable Long axeId) {
        return ResponseEntity.ok(voyageService.findByAxeId(axeId));
    }

    @GetMapping("/axe/{axeId}/paginated")
    public ResponseEntity<VoyagePageDto> getVoyagesByAxePaginated(
            @PathVariable Long axeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(voyageService.findByAxeIdPaginated(axeId, page, size));
    }

    @GetMapping("/depot/{depotId}/chargement")
    public ResponseEntity<List<VoyageDTO>> getVoyagesByDepotChargement(@PathVariable Long depotId) {
        return ResponseEntity.ok(voyageService.findByDepotIdAndStatutsChargement(depotId));
    }

    @GetMapping("/utilisateur/{identifiant}/chargement")
    public ResponseEntity<List<VoyageDTO>> getVoyagesByUtilisateurIdentifiantChargement(
            @PathVariable String identifiant) {
        return ResponseEntity.ok(voyageService.findByUtilisateurIdentifiant(identifiant));
    }

    @GetMapping("/utilisateur/{identifiant}")
    public ResponseEntity<List<VoyageDTO>> getVoyagesByUtilisateurIdentifiant(@PathVariable String identifiant) {
        return ResponseEntity.ok(voyageService.findByUtilisateurIdentifiant(identifiant));
    }

    @GetMapping("/depot/{depotId}/camions-charges/count")
    public ResponseEntity<Long> countCamionsChargesByDepot(@PathVariable Long depotId) {
        return ResponseEntity.ok(voyageService.countCamionsChargesByDepotId(depotId));
    }

    @PutMapping("/update-statut-multiple")
    public ResponseEntity<List<VoyageDTO>> updateStatutMultiple(
            @RequestBody List<Long> voyageIds,
            @RequestParam String statut) {
        return ResponseEntity.ok(voyageService.updateStatutMultiple(voyageIds, statut));
    }

    @GetMapping("/transitaire/{transitaireId}/archives")
    public ResponseEntity<com.backend.gesy.voyage.dto.VoyagePageDto> getArchivedVoyagesByTransitaire(
            @PathVariable Long transitaireId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(voyageService.findArchivedVoyagesByTransitaire(transitaireId, page, size));
    }

    @GetMapping("/transitaire/{transitaireId}/archives/date")
    public ResponseEntity<com.backend.gesy.voyage.dto.VoyagePageDto> getArchivedVoyagesByTransitaireAndDate(
            @PathVariable Long transitaireId,
            @RequestParam String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        java.time.LocalDate localDate = java.time.LocalDate.parse(date);
        return ResponseEntity
                .ok(voyageService.findArchivedVoyagesByTransitaireAndDate(transitaireId, localDate, page, size));
    }

    @GetMapping("/transitaire/{transitaireId}/archives/date-range")
    public ResponseEntity<com.backend.gesy.voyage.dto.VoyagePageDto> getArchivedVoyagesByTransitaireAndDateRange(
            @PathVariable Long transitaireId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        java.time.LocalDate start = java.time.LocalDate.parse(startDate);
        java.time.LocalDate end = java.time.LocalDate.parse(endDate);
        return ResponseEntity
                .ok(voyageService.findArchivedVoyagesByTransitaireAndDateRange(transitaireId, start, end, page, size));
    }

    @GetMapping("/transitaire/identifiant/{identifiant}/archives")
    public ResponseEntity<com.backend.gesy.voyage.dto.VoyagePageDto> getArchivedVoyagesByTransitaireIdentifiant(
            @PathVariable String identifiant,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(voyageService.findArchivedVoyagesByTransitaireIdentifiant(identifiant, page, size));
    }

    @GetMapping("/transitaire/identifiant/{identifiant}/archives/date")
    public ResponseEntity<com.backend.gesy.voyage.dto.VoyagePageDto> getArchivedVoyagesByTransitaireIdentifiantAndDate(
            @PathVariable String identifiant,
            @RequestParam String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        java.time.LocalDate localDate = java.time.LocalDate.parse(date);
        return ResponseEntity.ok(
                voyageService.findArchivedVoyagesByTransitaireIdentifiantAndDate(identifiant, localDate, page, size));
    }

    @GetMapping("/transitaire/identifiant/{identifiant}/archives/date-range")
    public ResponseEntity<com.backend.gesy.voyage.dto.VoyagePageDto> getArchivedVoyagesByTransitaireIdentifiantAndDateRange(
            @PathVariable String identifiant,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        java.time.LocalDate start = java.time.LocalDate.parse(startDate);
        java.time.LocalDate end = java.time.LocalDate.parse(endDate);
        return ResponseEntity.ok(voyageService.findArchivedVoyagesByTransitaireIdentifiantAndDateRange(identifiant,
                start, end, page, size));
    }

    @GetMapping("/archives")
    public ResponseEntity<com.backend.gesy.voyage.dto.VoyagePageDto> getArchivedVoyages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(voyageService.findArchivedVoyages(page, size));
    }

    @GetMapping("/archives/date")
    public ResponseEntity<com.backend.gesy.voyage.dto.VoyagePageDto> getArchivedVoyagesByDate(
            @RequestParam String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        java.time.LocalDate localDate = java.time.LocalDate.parse(date);
        return ResponseEntity.ok(voyageService.findArchivedVoyagesByDate(localDate, page, size));
    }

    @GetMapping("/archives/date-range")
    public ResponseEntity<com.backend.gesy.voyage.dto.VoyagePageDto> getArchivedVoyagesByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        java.time.LocalDate start = java.time.LocalDate.parse(startDate);
        java.time.LocalDate end = java.time.LocalDate.parse(endDate);
        return ResponseEntity.ok(voyageService.findArchivedVoyagesByDateRange(start, end, page, size));
    }

    @PostMapping
    public ResponseEntity<?> createVoyage(@RequestBody VoyageDTO voyageDTO) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(voyageService.save(voyageDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<VoyageDTO> updateVoyage(@PathVariable Long id, @RequestBody VoyageDTO voyageDTO) {
        return ResponseEntity.ok(voyageService.update(id, voyageDTO));
    }

    @PutMapping("/{id}/statut")
    public ResponseEntity<VoyageDTO> updateStatut(
            @PathVariable Long id,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Double manquant,
            @RequestParam(required = false) java.math.BigDecimal prixAchat,
            @RequestBody(required = false) com.backend.gesy.voyage.dto.UpdateStatutRequestDTO request) {
        try {
            // Vérifier si le request body contient des données valides pour le nouveau format
            // Si request est null ou si tous les champs sont null/vides, utiliser l'ancien format
            boolean useNewFormat = false;
            if (request != null) {
                // Vérifier si le body contient un statut valide OU des clients/manquants
                boolean hasStatut = request.getStatut() != null && !request.getStatut().trim().isEmpty();
                boolean hasClients = request.getClients() != null && !request.getClients().isEmpty();
                boolean hasManquants = request.getManquants() != null && !request.getManquants().isEmpty();
                
                // Si au moins un champ est présent, utiliser le nouveau format
                if (hasStatut || hasClients || hasManquants) {
                    useNewFormat = true;
                    // Si le statut n'est pas dans le body mais dans les query params, l'utiliser
                    if (!hasStatut && statut != null && !statut.trim().isEmpty()) {
                        request.setStatut(statut);
                    }
                    // Vérifier que le statut est présent
                    if (request.getStatut() == null || request.getStatut().trim().isEmpty()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(null);
                    }
                    return ResponseEntity.ok(voyageService.updateStatut(id, request));
                }
            }
            
            // Utiliser l'ancienne méthode pour compatibilité
            if (statut == null || statut.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(null);
            }
            return ResponseEntity.ok(voyageService.updateStatut(id, statut, clientId, manquant, prixAchat));
        } catch (RuntimeException e) {
            // Retourner le message d'erreur pour le débogage
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Error-Message", e.getMessage())
                    .body(null);
        } catch (Exception e) {
            // Gérer les autres exceptions (validation, etc.)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Error-Message", e.getMessage())
                    .body(null);
        }
    }

    @PutMapping("/{id}/prix-achat")
    public ResponseEntity<VoyageDTO> donnerPrixAchat(
            @PathVariable Long id,
            @RequestParam Long clientVoyageId,
            @RequestParam java.math.BigDecimal prixAchat) {
        try {
            return ResponseEntity.ok(voyageService.donnerPrixAchat(id, clientVoyageId, prixAchat));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PutMapping("/{id}/client-voyage/quantite")
    public ResponseEntity<VoyageDTO> updateClientVoyageQuantite(
            @PathVariable Long id,
            @RequestParam Long clientVoyageId,
            @RequestParam Long clientId,
            @RequestParam Double quantite) {
        try {
            return ResponseEntity.ok(voyageService.updateClientVoyageQuantite(id, clientVoyageId, clientId, quantite));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PutMapping("/{id}/transitaire")
    public ResponseEntity<VoyageDTO> assignTransitaire(
            @PathVariable Long id,
            @RequestParam Long transitaireId) {
        return ResponseEntity.ok(voyageService.assignTransitaire(id, transitaireId));
    }

    @PutMapping("/{id}/declarer")
    public ResponseEntity<VoyageDTO> declarerVoyage(
            @PathVariable Long id,
            @RequestParam(required = false) Long compteId,
            @RequestParam(required = false) Long caisseId) {
        return ResponseEntity.ok(voyageService.declarerVoyage(id, compteId, caisseId));
    }

    @PutMapping("/declarer-multiple")
    public ResponseEntity<List<VoyageDTO>> declarerVoyagesMultiple(
            @RequestBody List<Long> voyageIds,
            @RequestParam(required = false) Long compteId,
            @RequestParam(required = false) Long caisseId) {
        return ResponseEntity.ok(voyageService.declarerVoyagesMultiple(voyageIds, compteId, caisseId));
    }

    @PutMapping("/{id}/passer-non-declarer")
    public ResponseEntity<VoyageDTO> passerNonDeclarer(@PathVariable Long id) {
        return ResponseEntity.ok(voyageService.passerNonDeclarer(id));
    }

    @PutMapping("/{id}/liberer")
    public ResponseEntity<VoyageDTO> libererVoyage(@PathVariable Long id) {
        return ResponseEntity.ok(voyageService.libererVoyage(id));
    }

    @PutMapping("/liberer-multiple")
    public ResponseEntity<List<VoyageDTO>> libererVoyages(@RequestBody List<Long> voyageIds) {
        return ResponseEntity.ok(voyageService.libererVoyages(voyageIds));
    }

    @GetMapping("/transitaire/{transitaireId}/stats")
    public ResponseEntity<com.backend.gesy.voyage.dto.TransitaireStatsDTO> getTransitaireStats(
            @PathVariable Long transitaireId) {
        return ResponseEntity.ok(voyageService.getTransitaireStats(transitaireId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVoyage(@PathVariable Long id) {
        voyageService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/marge")
    public ResponseEntity<com.backend.gesy.voyage.dto.VoyageMargeDTO> getVoyageMarge(@PathVariable Long id) {
        return ResponseEntity.ok(voyageService.calculateMarge(id));
    }

    @PutMapping("/{id}/bon-enlevement/assigner")
    public ResponseEntity<VoyageDTO> assignerNumeroBonEnlevement(
            @PathVariable Long id,
            @RequestParam String numeroBonEnlevement) {
        return ResponseEntity.ok(voyageService.assignerNumeroBonEnlevement(id, numeroBonEnlevement));
    }

    @PutMapping("/{id}/bon-enlevement/generer")
    public ResponseEntity<VoyageDTO> genererNumeroBonEnlevement(@PathVariable Long id) {
        return ResponseEntity.ok(voyageService.genererNumeroBonEnlevement(id));
    }

    @GetMapping("/couts-transport")
    public ResponseEntity<com.backend.gesy.voyage.dto.CoutTransportResponseDTO> getCoutsTransport(
            @RequestParam Long fournisseurId,
            @RequestParam(required = false, defaultValue = "tous") String filterOption,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate) {
        return ResponseEntity.ok(voyageService.getCoutsTransport(fournisseurId, filterOption, startDate, endDate));
    }

    @GetMapping("/charges/identifiant/{identifiant}")
    public ResponseEntity<VoyagePageDto> getVoyagesChargesByIdentifiant(
            @PathVariable String identifiant,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate) {
        return ResponseEntity
                .ok(voyageService.findVoyagesChargesByIdentifiant(identifiant, page, size, date, startDate, endDate));
    }

    @GetMapping("/passes-non-declares")
    public ResponseEntity<java.util.List<com.backend.gesy.voyage.dto.VoyageDTO>> getVoyagesPassesNonDeclares() {
        return ResponseEntity.ok(voyageService.findVoyagesPassesNonDeclares());
    }

    @GetMapping("/passes-non-declares/paginated")
    public ResponseEntity<VoyagePageDto> getVoyagesPassesNonDeclaresPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(voyageService.findVoyagesPassesNonDeclaresPaginated(page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'COMPTABLE')")
    @GetMapping("/avec-client-sans-facture")
    public ResponseEntity<VoyagePageDto> getVoyagesAvecClientSansFacture(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(voyageService.findVoyagesAvecClientSansFacture(page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'COMPTABLE')")
    @GetMapping("/avec-client-sans-facture/groupes-par-client")
    public ResponseEntity<com.backend.gesy.voyage.dto.VoyagesParClientPageDto> getVoyagesAvecClientSansFactureGroupesParClient(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(voyageService.findVoyagesAvecClientSansFactureGroupesParClient(page, size));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'COMPTABLE')")
    @GetMapping("/sans-prix-transport")
    public ResponseEntity<VoyagePageDto> getVoyagesSansPrixTransport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(voyageService.findVoyagesSansPrixTransport(page, size));
    }
}