package com.backend.gesy.camion;

import com.backend.gesy.camion.dto.CamionDTO;
import com.backend.gesy.camion.dto.CamionWithVoyagesCountDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/camions")
@CrossOrigin(origins = "*")
public class CamionController {
    @Autowired
    private CamionService camionService;

    @GetMapping
    public ResponseEntity<List<CamionDTO>> getAllCamions() {
        return ResponseEntity.ok(camionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CamionDTO> getCamionById(@PathVariable Long id) {
        return camionService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/immatriculation/{immatriculation}")
    public ResponseEntity<CamionDTO> getCamionByImmatriculation(@PathVariable String immatriculation) {
        return camionService.findByImmatriculation(immatriculation)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CamionDTO> createCamion(@RequestBody CamionDTO camionDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(camionService.save(camionDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CamionDTO> updateCamion(@PathVariable Long id, @RequestBody CamionDTO camionDTO) {
        return ResponseEntity.ok(camionService.update(id, camionDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCamion(@PathVariable Long id) {
        camionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/fournisseur/{fournisseurId}")
    public ResponseEntity<List<CamionWithVoyagesCountDTO>> getCamionsByFournisseur(@PathVariable Long fournisseurId) {
        return ResponseEntity.ok(camionService.findByFournisseurId(fournisseurId));
    }
}

