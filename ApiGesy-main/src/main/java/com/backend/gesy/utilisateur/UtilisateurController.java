package com.backend.gesy.utilisateur;

import com.backend.gesy.utilisateur.dto.UtilisateurDTO;
import com.backend.gesy.utilisateur.dto.UtilisateurMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UtilisateurController {
    private final UtilisateurService utilisateurService;
    private final UtilisateurMapper utilisateurMapper;

    @GetMapping
    public ResponseEntity<List<UtilisateurDTO>> getAllUtilisateurs() {
        List<UtilisateurDTO> dtos = utilisateurService.findAll().stream()
            .map(utilisateurMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Utilisateurs pouvant être responsables logistiques (création de voyages, camions, etc.).
     * Ne retourne que les comptes actifs ayant un rôle "Responsable Logistique",
     * "Logisticien" ou "Simple Logisticien".
     */
    @GetMapping("/logisticiens")
    public ResponseEntity<List<UtilisateurDTO>> getLogisticiensEtResponsables() {
        List<UtilisateurDTO> dtos = utilisateurService.findLogisticiensEtResponsables().stream()
            .map(utilisateurMapper::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UtilisateurDTO> getUtilisateurById(@PathVariable Long id) {
        return utilisateurService.findById(id)
            .map(utilisateurMapper::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/identifiant/{identifiant}")
    public ResponseEntity<UtilisateurDTO> getUtilisateurByIdentifiant(@PathVariable String identifiant) {
        return utilisateurService.findByIdentifiant(identifiant)
            .map(utilisateurMapper::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/current")
    public ResponseEntity<UtilisateurDTO> getCurrentUser() {
        return utilisateurService.getCurrentUser()
            .map(utilisateurMapper::toDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping
    public ResponseEntity<UtilisateurDTO> createUtilisateur(@RequestBody Utilisateur utilisateur) {
        Utilisateur saved = utilisateurService.save(utilisateur);
        return ResponseEntity.status(HttpStatus.CREATED).body(utilisateurMapper.toDTO(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UtilisateurDTO> updateUtilisateur(@PathVariable Long id, @RequestBody Utilisateur utilisateur) {
        Utilisateur updated = utilisateurService.update(id, utilisateur);
        return ResponseEntity.ok(utilisateurMapper.toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUtilisateur(@PathVariable Long id) {
        utilisateurService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

